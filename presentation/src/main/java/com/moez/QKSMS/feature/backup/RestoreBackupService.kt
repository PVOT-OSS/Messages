/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.prauga.messages.feature.backup

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.android.AndroidInjection
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.prauga.messages.common.util.extensions.getLabel
import org.prauga.messages.manager.NotificationManager
import org.prauga.messages.repository.BackupRepository
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class RestoreBackupService : Service() {

    companion object {
        private const val NOTIFICATION_ID = -1

        private const val ACTION_START = "org.prauga.messages.ACTION_START"
        private const val ACTION_STOP = "org.prauga.messages.ACTION_STOP"
        private const val EXTRA_FILE_URI = "org.prauga.messages.EXTRA_FILE_URI"

        fun start(context: Context, backupFile: Uri) {
            val intent = Intent(context, RestoreBackupService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_FILE_URI, backupFile.toString())

            ContextCompat.startForegroundService(context, intent)
        }
    }

    @Inject
    lateinit var backupRepo: BackupRepository
    @Inject
    lateinit var notificationManager: NotificationManager

    private val notification by lazy { notificationManager.getNotificationForBackup() }

    override fun onCreate() = AndroidInjection.inject(this)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent.action) {
            ACTION_START -> start(intent)
            ACTION_STOP -> stop()
        }

        return Service.START_STICKY
    }

    @SuppressLint("CheckResult")
    private fun start(intent: Intent) {
        val notificationManager = NotificationManagerCompat.from(this)

        startForeground(NOTIFICATION_ID, notification.build())

        backupRepo.getRestoreProgress()
            .sample(200, TimeUnit.MILLISECONDS, true)
            .subscribeOn(Schedulers.io())
            .subscribe { progress ->
                when (progress) {
                    is BackupRepository.Progress.Idle -> stop()

                    is BackupRepository.Progress.Running -> notification
                        .setProgress(progress.max, progress.count, progress.indeterminate)
                        .setContentText(progress.getLabel(this))
                        .let { notificationManager.notify(NOTIFICATION_ID, it.build()) }

                    else -> notification
                        .setProgress(0, 0, progress.indeterminate)
                        .setContentText(progress.getLabel(this))
                        .let { notificationManager.notify(NOTIFICATION_ID, it.build()) }
                }
            }

        // Start the restore
        Observable.just(intent)
            .map { Uri.parse(it.getStringExtra(EXTRA_FILE_URI)) }
            .map(backupRepo::performRestore)
            .subscribeOn(Schedulers.io())
            .subscribe({}, Timber::w)
    }

    private fun stop() {
        stopForeground(true)
        stopSelf()
    }

}