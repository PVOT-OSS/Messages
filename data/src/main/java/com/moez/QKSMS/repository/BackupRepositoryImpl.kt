/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Telephony
import androidx.core.content.contentValuesOf
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.squareup.moshi.Moshi
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import io.realm.Realm
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Timer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.schedule
import okio.buffer
import okio.sink
import okio.source
import org.prauga.messages.common.util.extensions.now
import org.prauga.messages.extensions.forEachMessagePage
import org.prauga.messages.model.BackupFile
import org.prauga.messages.model.Message
import org.prauga.messages.util.Preferences
import timber.log.Timber

@Singleton
class BackupRepositoryImpl
@Inject
constructor(
    private val context: Context,
    private val moshi: Moshi,
    private val prefs: Preferences,
    private val syncRepo: SyncRepository,
) : BackupRepository {

    /**
     * Simpler version of [Backup] which allows us to read certain fields from the backup without
     * needing to parse the entire file
     */
    data class BackupMetadata(val messageCount: Int = 0)

    data class BackupMessage(
        val type: Int,
        val address: String,
        val date: Long,
        val dateSent: Long,
        val read: Boolean,
        val status: Int,
        val body: String,
        val protocol: Int,
        val serviceCenter: String?,
        val locked: Boolean,
        val subId: Int,
    )

    // Subjects to emit our progress events to
    private val backupProgress: Subject<BackupRepository.Progress> =
        BehaviorSubject.createDefault(BackupRepository.Progress.Idle())
    private val restoreProgress: Subject<BackupRepository.Progress> =
        BehaviorSubject.createDefault(BackupRepository.Progress.Idle())

    @Volatile private var stopFlag: Boolean = false

    private val json = BackupJson(moshi)

    override fun getDefaultBackupPath(): String {
        return "${Environment.getExternalStorageDirectory()}/QKSMS/Backups"
    }

    override fun getBackupDocumentTree(): DocumentFile? {
        return prefs.backupDirectory
            .get()
            .takeIf { uri -> uri != Uri.EMPTY }
            ?.let { uri -> DocumentFile.fromTreeUri(context, uri) }
            ?.takeIf { dir -> dir.exists() && dir.canRead() && dir.canWrite() }
    }

    override fun getBackupPathUriForPicker(): Uri {
        return prefs.backupDirectory.get().takeIf { uri -> uri != Uri.EMPTY }
            ?: getDefaultBackupPath().toUri()
    }

    override fun persistBackupDirectory(directory: Uri) {
        context.contentResolver.takePersistableUriPermission(
            directory,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        prefs.backupDirectory.set(directory)

        Timber.v("Updated backup directory: $directory")
    }

    override fun performBackup() {
        // If a backup or restore is already running, don't do anything
        if (isBackupOrRestoreRunning()) return

        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(now())
        val file = getBackupDocumentTree()?.createFile("application/json", "backup-$timestamp.json")

        try {
            val stream =
                file?.let { context.contentResolver.openOutputStream(it.uri) }
                    ?: throw Exception("Failed to open output stream")

            stream.sink().buffer().use { sink ->
                Realm.getDefaultInstance().use { realm ->
                    val messageCount = realm.where(Message::class.java).count().toInt()
                    var index = 0
                    json.write(sink, messageCount) { write ->
                        realm.forEachMessagePage({ where(Message::class.java) }) { page ->
                            page.forEach { message ->
                                backupProgress.onNext(
                                    BackupRepository.Progress.Running(messageCount, index++)
                                )
                                write(messageToBackupMessage(message))
                            }
                        }
                        backupProgress.onNext(BackupRepository.Progress.Saving())
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e)
            file?.delete()
        }

        // Mark the task finished, and set it as Idle a second later
        backupProgress.onNext(BackupRepository.Progress.Finished())
        Timer().schedule(1000) { backupProgress.onNext(BackupRepository.Progress.Idle()) }
    }

    private fun messageToBackupMessage(message: Message): BackupMessage =
        BackupMessage(
            type = message.boxId,
            address = message.address,
            date = message.date,
            dateSent = message.dateSent,
            read = message.read,
            status = message.deliveryStatus,
            body = message.body,
            protocol = 0,
            serviceCenter = null,
            locked = message.locked,
            subId = message.subId,
        )

    override fun getBackupProgress(): Observable<BackupRepository.Progress> = backupProgress

    @SuppressLint("Recycle") // InputStream is closed by Okio BufferedSource
    override fun parseBackup(uri: Uri): BackupFile {
        val adapter = moshi.adapter(BackupMetadata::class.java)

        val file =
            DocumentFile.fromSingleUri(context, uri)
                ?: throw IllegalArgumentException("Couldn't open backup file")

        val metadata =
            context.contentResolver
                .openInputStream(file.uri)
                ?.source()
                ?.buffer()
                ?.use(adapter::fromJson)
                ?: throw IllegalArgumentException("Couldn't parse backup file")

        return BackupFile(file.lastModified(), metadata.messageCount)
    }

    override fun performRestore(uri: Uri) {
        // If a backupFile or restore is already running, don't do anything
        if (isBackupOrRestoreRunning()) return

        restoreProgress.onNext(BackupRepository.Progress.Parsing())

        val messageCount = readBackup(uri) { true }
        var errorCount = 0
        var index = 0
        var stopped = false

        readBackup(uri) { message ->
            if (stopFlag) {
                stopped = true
                return@readBackup false
            }

            // Update the progress
            restoreProgress.onNext(BackupRepository.Progress.Running(messageCount, index++))

            try {
                restoreMessage(message!!)
            } catch (e: Exception) {
                Timber.w(e)
                errorCount++
            }
            true
        }

        if (stopped) {
            stopFlag = false
            restoreProgress.onNext(BackupRepository.Progress.Idle())
            return
        }

        if (errorCount > 0) {
            Timber.w(Exception("Failed to backup $errorCount/$messageCount messages"))
        }

        // Sync the messages
        restoreProgress.onNext(BackupRepository.Progress.Syncing())
        syncRepo.syncMessages()

        // Mark the task finished, and set it as Idle a second later
        restoreProgress.onNext(BackupRepository.Progress.Finished())
        Timer().schedule(1000) { restoreProgress.onNext(BackupRepository.Progress.Idle()) }
    }

    @SuppressLint("Recycle") // InputStream is closed by Okio BufferedSource
    private fun readBackup(uri: Uri, onMessage: (BackupMessage?) -> Boolean): Int =
        DocumentFile.fromSingleUri(context, uri)
            ?.let { file -> context.contentResolver.openInputStream(file.uri) }
            ?.source()
            ?.buffer()
            ?.use { source -> json.read(source, onMessage) } ?: 0

    private fun restoreMessage(message: BackupMessage) {
        val values =
            contentValuesOf(
                Telephony.Sms.TYPE to message.type,
                Telephony.Sms.ADDRESS to message.address,
                Telephony.Sms.DATE to message.date,
                Telephony.Sms.DATE_SENT to message.dateSent,
                Telephony.Sms.READ to message.read,
                Telephony.Sms.SEEN to 1,
                Telephony.Sms.STATUS to message.status,
                Telephony.Sms.BODY to message.body,
                Telephony.Sms.PROTOCOL to message.protocol,
                Telephony.Sms.SERVICE_CENTER to message.serviceCenter,
                Telephony.Sms.LOCKED to message.locked,
            )

        if (prefs.canUseSubId.get()) {
            values.put(Telephony.Sms.SUBSCRIPTION_ID, message.subId)
        }

        context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
    }

    override fun getRestoreProgress(): Observable<BackupRepository.Progress> = restoreProgress

    override fun stopRestore() {
        stopFlag = true
    }

    private fun isBackupOrRestoreRunning(): Boolean {
        return backupProgress.blockingFirst().running || restoreProgress.blockingFirst().running
    }
}
