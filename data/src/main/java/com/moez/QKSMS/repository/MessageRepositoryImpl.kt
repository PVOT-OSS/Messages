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
package org.prauga.messages.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Telephony
import android.provider.Telephony.Mms
import android.provider.Telephony.Sms
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.webkit.MimeTypeMap
import androidx.core.content.contentValuesOf
import com.android.mms.MmsConfig
import com.android.mms.dom.smil.parser.SmilXmlSerializer
import com.android.mms.transaction.DownloadManager
import com.google.android.mms.ContentType
import com.google.android.mms.MMSPart
import com.google.android.mms.pdu_alt.CharacterSets
import com.google.android.mms.pdu_alt.EncodedStringValue
import com.google.android.mms.pdu_alt.MultimediaMessagePdu
import com.google.android.mms.pdu_alt.PduBody
import com.google.android.mms.pdu_alt.PduHeaders
import com.google.android.mms.pdu_alt.PduPart
import com.google.android.mms.pdu_alt.PduPersister
import com.google.android.mms.pdu_alt.SendReq
import com.google.android.mms.smil.SmilHelper
import com.klinker.android.send_message.CarrierMmsConfig
import com.klinker.android.send_message.SmsManagerFactory
import com.klinker.android.send_message.StripAccents
import com.klinker.android.send_message.Transaction
import com.klinker.android.send_message.Utils
import io.realm.Case
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt
import org.prauga.messages.common.util.extensions.now
import org.prauga.messages.compat.TelephonyCompat
import org.prauga.messages.extensions.anyOf
import org.prauga.messages.extensions.isImage
import org.prauga.messages.extensions.isVideo
import org.prauga.messages.extensions.resourceExists
import org.prauga.messages.manager.ActiveConversationManager
import org.prauga.messages.manager.KeyManager
import org.prauga.messages.mapper.CursorToPart
import org.prauga.messages.model.Attachment
import org.prauga.messages.model.Conversation
import org.prauga.messages.model.Message
import org.prauga.messages.model.MmsPart
import org.prauga.messages.model.ReceivedSmsDetails
import org.prauga.messages.receiver.SendSmsReceiver
import org.prauga.messages.receiver.SmsDeliveredReceiver
import org.prauga.messages.receiver.SmsSentParts
import org.prauga.messages.receiver.SmsSentReceiver
import org.prauga.messages.util.ImageUtils
import org.prauga.messages.util.PhoneNumberUtils
import org.prauga.messages.util.Preferences
import org.prauga.messages.util.tryOrNull
import org.prauga.messages.worker.DelayedSmsWorker
import timber.log.Timber

@Singleton
open class MessageRepositoryImpl
@Inject
constructor(
    private val activeConversationManager: ActiveConversationManager,
    private val context: Context,
    private val cursorToPart: CursorToPart,
    private val messageIds: KeyManager,
    private val phoneNumberUtils: PhoneNumberUtils,
    private val prefs: Preferences,
    private val syncRepository: SyncRepository,
    private val reactions: EmojiReactionRepository,
) : MessageRepository {

    companion object {
        private const val MMS_HEADER_ALLOWANCE = 2048
        const val TELEPHONY_UPDATE_CHUNK_SIZE = 200
    }

    private fun getMessagesBase(threadId: Long, query: String) =
        Realm.getDefaultInstance()
            .where(Message::class.java)
            .equalTo("threadId", threadId)
            .equalTo("isEmojiReaction", false)
            .let {
                when (query.isEmpty()) {
                    true -> it
                    false ->
                        it.beginGroup()
                            .contains("body", query, Case.INSENSITIVE)
                            .or()
                            .contains("parts.text", query, Case.INSENSITIVE)
                            .endGroup()
                }
            }
            .sort("date")

    override fun getMessages(threadId: Long, query: String): RealmResults<Message> =
        getMessagesBase(threadId, query).findAllAsync()

    override fun getMessagesSync(threadId: Long, query: String): RealmResults<Message> =
        getMessagesBase(threadId, query).findAll()

    override fun getMessage(id: Long) =
        Realm.getDefaultInstance()
            .also { it.refresh() }
            .where(Message::class.java)
            .equalTo("id", id)
            .findFirst()

    override fun getMessageForPart(id: Long) =
        Realm.getDefaultInstance().where(Message::class.java).equalTo("parts.id", id).findFirst()

    override fun getLastIncomingMessage(threadId: Long): RealmResults<Message> =
        Realm.getDefaultInstance()
            .where(Message::class.java)
            .equalTo("threadId", threadId)
            .beginGroup()
            .beginGroup()
            .equalTo("type", "sms")
            .`in`("boxId", arrayOf(Sms.MESSAGE_TYPE_INBOX, Sms.MESSAGE_TYPE_ALL))
            .endGroup()
            .or()
            .beginGroup()
            .equalTo("type", "mms")
            .`in`("boxId", arrayOf(Mms.MESSAGE_BOX_INBOX, Mms.MESSAGE_BOX_ALL))
            .endGroup()
            .endGroup()
            .sort("date", Sort.DESCENDING)
            .findAll()

    override fun getUnreadCount() =
        Realm.getDefaultInstance().use { realm ->
            realm.refresh()
            realm
                .where(Conversation::class.java)
                .equalTo("archived", false)
                .equalTo("blocked", false)
                .equalTo("lastMessage.read", false)
                .count()
        }

    override fun getPart(id: Long) =
        Realm.getDefaultInstance().where(MmsPart::class.java).equalTo("id", id).findFirst()

    override fun getPartsForConversation(threadId: Long): RealmResults<MmsPart> =
        Realm.getDefaultInstance()
            .where(MmsPart::class.java)
            .equalTo("messages.threadId", threadId)
            .beginGroup()
            .contains("type", "image/")
            .or()
            .contains("type", "video/")
            .endGroup()
            .sort("id", Sort.DESCENDING)
            .findAllAsync()

    override fun savePart(id: Long): Uri? {
        val part = getPart(id) ?: return null

        val extension =
            MimeTypeMap.getSingleton().getExtensionFromMimeType(part.type) ?: return null
        // fileDateAndTime is divided by 1000 in order to remove the extra 0's after date and time
        // This way the file name isn't so long.
        val fileDateAndTime = (part.messages?.firstOrNull()?.date)?.div(1000)
        val fileName = "QUIK_${part.type.split("/").last()}_$fileDateAndTime.$extension"

        val values =
            contentValuesOf(
                MediaStore.MediaColumns.DISPLAY_NAME to fileName,
                MediaStore.MediaColumns.MIME_TYPE to part.type,
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.IS_PENDING, 1)
            values.put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                when {
                    part.isImage() -> "${Environment.DIRECTORY_PICTURES}/QUIK"
                    part.isVideo() -> "${Environment.DIRECTORY_MOVIES}/QUIK"
                    else -> "${Environment.DIRECTORY_DOWNLOADS}/QUIK"
                },
            )
        }

        val contentUri =
            when {
                part.isImage() -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                part.isVideo() -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                else -> MediaStore.Files.getContentUri("external")
            }

        val uri = context.contentResolver.insert(contentUri, values)
        Timber.v("Saving $fileName (${part.type}) to $uri")

        uri?.let {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                context.contentResolver.openInputStream(part.getUri())?.use { inputStream ->
                    inputStream.copyTo(outputStream, 1024)
                }
            }
            Timber.v("Saved $fileName (${part.type}) to $uri")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.update(
                    uri,
                    contentValuesOf(MediaStore.MediaColumns.IS_PENDING to 0),
                    null,
                    null,
                )
                Timber.v("Marked $uri as not pending")
            }
        }

        return uri
    }

    override fun getUnreadUnseenMessages(threadId: Long): RealmResults<Message> =
        Realm.getDefaultInstance()
            .also { it.refresh() }
            .where(Message::class.java)
            .equalTo("seen", false)
            .equalTo("read", false)
            .equalTo("threadId", threadId)
            .sort("date")
            .findAll()

    override fun getUnreadMessages(threadId: Long): RealmResults<Message> =
        Realm.getDefaultInstance()
            .where(Message::class.java)
            .equalTo("read", false)
            .equalTo("threadId", threadId)
            .sort("date")
            .findAll()

    // marks all messages in threads as read and/or seen in the native provider
    private fun telephonyMarkSeenRead(
        seen: Boolean?,
        read: Boolean?,
        threadIds: Collection<Long>,
    ): Int {
        if (((seen == null) && (read == null)) || threadIds.isEmpty()) return -1

        var countUpdated = 0

        // 'read' can be modified at the conversation level which updates all messages
        read?.let {
            tryOrNull(true) {
                // chunked so where clause doesn't get too long if there are many threads
                threadIds.forEach {
                    countUpdated +=
                        context.contentResolver.update(
                            ContentUris.withAppendedId(
                                Telephony.MmsSms.CONTENT_CONVERSATIONS_URI,
                                it,
                            ),
                            contentValuesOf(Sms.READ to read),
                            "${Sms.READ} = ${if (read) 0 else 1}",
                            null,
                        )
                }
            }
        }

        seen?.let {
            // 'seen' has to be modified at the messages level
            threadIds.chunked(TELEPHONY_UPDATE_CHUNK_SIZE).forEach {
                // chunked for smaller where clause size
                val values = contentValuesOf(Sms.SEEN to seen)
                val whereClause =
                    "${Sms.SEEN} = ${if (seen) 0 else 1} " +
                        "and ${Sms.THREAD_ID} in (${it.joinToString(",")})"

                // sms messages
                tryOrNull(true) {
                    countUpdated +=
                        context.contentResolver.update(Sms.CONTENT_URI, values, whereClause, null)
                }

                // mms messages
                tryOrNull(true) {
                    countUpdated +=
                        context.contentResolver.update(Mms.CONTENT_URI, values, whereClause, null)
                }
            }
        }

        return countUpdated // a mix of convo and message updates, so not overly useful. meh
    }

    override fun markAllSeen() =
        mutableSetOf<Long>().let { threadIds ->
            Realm.getDefaultInstance()
                .use { realm ->
                    realm
                        .where(Message::class.java)
                        .equalTo("seen", false)
                        .findAll()
                        .takeIf { it.isNotEmpty() }
                        ?.let { messages ->
                            realm.executeTransaction {
                                messages.forEach {
                                    it.seen = true
                                    threadIds += it.threadId
                                }
                            }
                        }
                }
                .run { telephonyMarkSeenRead(true, null, threadIds) }
        }

    override fun markSeen(threadId: Long) =
        Realm.getDefaultInstance()
            .use { realm ->
                realm
                    .where(Message::class.java)
                    .equalTo("threadId", threadId)
                    .equalTo("seen", false)
                    .findAll()
                    .let { messages ->
                        realm.executeTransaction { messages.forEach { it.seen = true } }
                    }
            }
            .run { telephonyMarkSeenRead(true, null, listOf(threadId)) }

    override fun markRead(threadIds: Collection<Long>) =
        threadIds
            .takeIf { it.isNotEmpty() }
            ?.let {
                Realm.getDefaultInstance()
                    ?.use { realm ->
                        realm
                            .where(Message::class.java)
                            .anyOf("threadId", threadIds.toLongArray())
                            .beginGroup()
                            .equalTo("read", false)
                            .or()
                            .equalTo("seen", false)
                            .endGroup()
                            .findAll()
                            .let { messages ->
                                realm.executeTransaction {
                                    messages.forEach {
                                        it.seen = true
                                        it.read = true
                                    }
                                }
                            }
                    }
                    .run { telephonyMarkSeenRead(true, true, threadIds) }
            } ?: 0

    override fun markUnread(threadIds: Collection<Long>) =
        threadIds
            .takeIf { it.isNotEmpty() }
            ?.let {
                Realm.getDefaultInstance()
                    ?.use { realm ->
                        val conversations =
                            realm
                                .where(Conversation::class.java)
                                .anyOf("id", threadIds.toLongArray())
                                .equalTo("lastMessage.read", true)
                                .findAll()

                        realm.executeTransaction {
                            conversations.forEach { it.lastMessage?.read = false }
                        }
                    }
                    .run { telephonyMarkSeenRead(null, false, threadIds) }
            } ?: 0

    override fun sendMessage(
        subId: Int,
        threadId: Long,
        addresses: Collection<String>,
        body: String,
        attachments: Collection<Attachment>,
        delay: Int,
        applySignature: Boolean,
    ) {
        val signedBody =
            when {
                !applySignature -> body
                prefs.signature.get().isEmpty() -> body
                body.isNotEmpty() -> body + '\n' + prefs.signature.get()
                else -> prefs.signature.get()
            }

        val smsManager =
            subId.takeIf { it != -1 }?.let(SmsManagerFactory::createSmsManager)
                ?: SmsManager.getDefault()

        // We only care about stripping SMS
        val strippedBody =
            when (prefs.unicode.get()) {
                true -> StripAccents.stripAccents(signedBody)
                false -> signedBody
            }

        val parts = smsManager.divideMessage(strippedBody).orEmpty()
        val forceMms =
            (prefs.longAsMms.get() && parts.size > 1) ||
                carrierRequiresMms(subId, addresses, strippedBody, parts.size)

        if (addresses.size == 1 && attachments.isEmpty() && !forceMms) { // SMS
            if (delay > 0) { // With delay
                val sendTime = System.currentTimeMillis() + delay
                val message =
                    insertSentSms(subId, threadId, addresses.first(), strippedBody, sendTime)

                scheduleDelayedSms(message.id, sendTime)
            } else { // No delay
                val message = insertSentSms(subId, threadId, addresses.first(), strippedBody, now())
                sendSms(message)
            }
        } else { // MMS
            val parts = arrayListOf<MMSPart>()

            val carrierConfig = CarrierMmsConfig.get(context, subId)
            val automaticSize = prefs.mmsSize.get() == -1

            val maxWidth = carrierConfig.maxImageWidth.takeIf { automaticSize } ?: Int.MAX_VALUE

            val maxHeight = carrierConfig.maxImageHeight.takeIf { automaticSize } ?: Int.MAX_VALUE

            var remainingBytes =
                when (prefs.mmsSize.get()) {
                    -1 -> (carrierConfig.maxMessageSize - MMS_HEADER_ALLOWANCE).toDouble()
                    0 -> Int.MAX_VALUE.toDouble()
                    else -> prefs.mmsSize.get() * 1024 * 0.9 // a bit of wiggle room
                }

            signedBody
                .takeIf { it.isNotEmpty() }
                ?.toByteArray()
                ?.let { bytes ->
                    remainingBytes -= bytes.size
                    parts += MMSPart("text", ContentType.TEXT_PLAIN, bytes)
                }

            // Attach those that can't be compressed (ie. everything but images)
            parts +=
                attachments
                    // filter in non-images only
                    .filter { !it.isImage(context) }
                    // filter in only items that exist (user may have deleted the file)
                    .filter { it.uri.resourceExists(context) }
                    .map {
                        remainingBytes -= it.getResourceBytes(context).size
                        val mmsPart =
                            MMSPart(
                                it.getName(context),
                                it.getType(context),
                                it.getResourceBytes(context),
                            )

                        // release the attachment hold on the image bytes so the GC can reclaim
                        it.releaseResourceBytes()

                        mmsPart
                    }

            val imageBytesByAttachment =
                attachments
                    // filter in images only
                    .filter { it.isImage(context) }
                    // filter in only items that exist (user may have deleted the file)
                    .filter { it.uri.resourceExists(context) }
                    .associateWith {
                        when (it.getType(context) == "image/gif") {
                            true -> ImageUtils.getScaledGif(context, it.uri, maxWidth, maxHeight)
                            false -> ImageUtils.getScaledImage(context, it.uri, maxWidth, maxHeight)
                        }
                    }
                    .toMutableMap()

            val imageByteCount = imageBytesByAttachment.values.sumOf { it.size }
            if (imageByteCount > remainingBytes) {
                imageBytesByAttachment.forEach { (attachment, originalBytes) ->
                    val uri = attachment.uri
                    val maxBytes = originalBytes.size / imageByteCount.toFloat() * remainingBytes

                    // Get the image dimensions
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(
                        context.contentResolver.openInputStream(uri),
                        null,
                        options,
                    )
                    val width = options.outWidth
                    val height = options.outHeight
                    val aspectRatio = width.toFloat() / height.toFloat()

                    var attempts = 0
                    var scaledBytes = originalBytes

                    while (scaledBytes.size > maxBytes) {
                        // Estimate how much we need to scale the image down by. If it's still
                        // too big, we'll need to try smaller and smaller values
                        val scale = maxBytes / originalBytes.size * (0.9 - attempts * 0.2)
                        if (scale <= 0) {
                            val errorMsg =
                                "Failed to compress image from ${originalBytes.size / 1024}KB " +
                                    "to fit within ${maxBytes.toInt() / 1024}KB limit. " +
                                    "Try using a smaller image or increasing your MMS size limit in settings."
                            Timber.e(errorMsg)
                            throw MmsCompressionException(errorMsg, attachment.getName(context))
                        }

                        val newArea = scale * width * height
                        val newWidth = sqrt(newArea * aspectRatio).toInt()
                        val newHeight = (newWidth / aspectRatio).toInt()

                        attempts++
                        scaledBytes =
                            when (attachment.getType(context) == "image/gif") {
                                true ->
                                    ImageUtils.getScaledGif(
                                        context,
                                        attachment.uri,
                                        newWidth,
                                        newHeight,
                                    )
                                false ->
                                    ImageUtils.getScaledImage(
                                        context,
                                        attachment.uri,
                                        newWidth,
                                        newHeight,
                                    )
                            }

                        Timber.d(
                            "Compression attempt $attempts: ${scaledBytes.size / 1024
                        }/${maxBytes.toInt() / 1024}Kb ($width*$height -> $newWidth*${
                            newHeight})"
                        )

                        // release the attachment hold on the image bytes so the GC can reclaim
                        attachment.releaseResourceBytes()
                    }

                    Timber.v(
                        "Compressed ${originalBytes.size / 1024}Kb to ${
                        scaledBytes.size / 1024}Kb with a target size of ${
                        maxBytes.toInt() / 1024}Kb in $attempts attempts"
                    )
                    imageBytesByAttachment[attachment] = scaledBytes
                }
            }

            imageBytesByAttachment.forEach { (attachment, bytes) ->
                parts +=
                    when (attachment.getType(context) == "image/gif") {
                        true -> MMSPart(attachment.getName(context), ContentType.IMAGE_GIF, bytes)
                        false -> MMSPart(attachment.getName(context), ContentType.IMAGE_JPEG, bytes)
                    }
            }

            val recipients =
                addresses.map { address ->
                    if (isEmailAddress(address)) address
                    else phoneNumberUtils.normalizeNumber(address)
                }

            // Pre-insert the MMS message
            val (_, messageUri) = insertSentMms(subId, threadId, recipients, parts, null)

            // We need to strip the separators from outgoing MMS, or else they'll appear to have
            // sent and not go through
            val transaction = Transaction(context)
            transaction.sendNewMessage(
                subId,
                threadId,
                recipients,
                parts,
                null,
                messageUri,
                userMaxMessageSize(subId),
            )
        }
    }

    override fun sendSms(message: Message) {
        try {
            sendSmsParts(message)
        } catch (e: Exception) {
            Timber.w(e, "Couldn't send SMS ${message.id}")
            reportSmsFailure(message.id)
        }
    }

    private fun reportSmsFailure(id: Long) {
        val intent =
            Intent(context, SmsSentReceiver::class.java)
                .putExtra(SmsSentParts.EXTRA_ID, id)
                .putExtra(SmsSentParts.EXTRA_ATTEMPT, System.currentTimeMillis())
        context.sendOrderedBroadcast(
            intent,
            null,
            null,
            null,
            SmsManager.RESULT_ERROR_GENERIC_FAILURE,
            null,
            null,
        )
    }

    private fun sendSmsParts(message: Message) {
        val smsManager =
            message.subId.takeIf { it != -1 }?.let(SmsManagerFactory::createSmsManager)
                ?: SmsManager.getDefault()

        val config = CarrierMmsConfig.get(context, message.subId)
        val gateway =
            config.emailGatewayNumber.takeIf {
                it.isNotEmpty() && isEmailAddress(message.address)
            }
        val destination = gateway ?: message.address
        val body = if (gateway != null) "${message.address} ${message.body}" else message.body

        val parts =
            smsManager.divideMessage(
                if (prefs.unicode.get()) StripAccents.stripAccents(body) else body
            ) ?: arrayListOf()

        val mutable =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val attempt = System.currentTimeMillis()
        val sentIntents =
            parts.indices.map { part ->
                val intent =
                    Intent(context, SmsSentReceiver::class.java)
                        .setData(Uri.parse("smssent://${message.id}/$part"))
                        .putExtra(SmsSentParts.EXTRA_ID, message.id)
                        .putExtra(SmsSentParts.EXTRA_ATTEMPT, attempt)
                        .putExtra(SmsSentParts.EXTRA_PART, part)
                        .putExtra(SmsSentParts.EXTRA_PARTS, parts.size)
                PendingIntent.getBroadcast(
                    context,
                    message.id.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or mutable,
                )
            }

        val deliveryIntent =
            PendingIntent.getBroadcast(
                context,
                message.id.toInt(),
                Intent(context, SmsDeliveredReceiver::class.java)
                    .setData(Uri.parse("smsdelivered://${message.id}"))
                    .putExtra("id", message.id),
                PendingIntent.FLAG_UPDATE_CURRENT or mutable,
            )
        val deliveryIntents =
            parts.indices.map { part ->
                deliveryIntent.takeIf { prefs.delivery.get() && part == parts.lastIndex }
            }

        try {
            if (config.sendMultipartSmsAsSeparateMessages && parts.size > 1) {
                parts.forEachIndexed { part, text ->
                    smsManager.sendTextMessage(
                        destination,
                        null,
                        text,
                        sentIntents[part],
                        deliveryIntents[part],
                    )
                }
            } else {
                smsManager.sendMultipartTextMessage(
                    destination,
                    null,
                    parts,
                    ArrayList(sentIntents),
                    ArrayList(deliveryIntents),
                )
            }
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Message body lengths: ${parts.map { it?.length }}")
            reportSmsFailure(message.id)
        }
    }

    override fun resendMms(message: Message) =
        tryOrNull {
                PduPersister.getPduPersister(context).load(message.getUri()) as MultimediaMessagePdu
            }
            ?.let { pdu ->
                Transaction(context)
                    .sendNewMessage(
                        message.subId,
                        message.threadId,
                        pdu.to.map { it.string }.filter { it.isNotBlank() },
                        message.parts.mapNotNull { part ->
                            val bytes =
                                tryOrNull(false) {
                                    context.contentResolver.openInputStream(part.getUri())?.use {
                                        inputStream ->
                                        inputStream.readBytes()
                                    }
                                } ?: return@mapNotNull null

                            MMSPart(part.name.orEmpty(), part.type, bytes)
                        },
                        message.subject,
                        message.getUri(),
                        userMaxMessageSize(message.subId),
                    )
            } ?: Unit

    private fun carrierRequiresMms(
        subId: Int,
        addresses: Collection<String>,
        body: String,
        smsParts: Int,
    ): Boolean {
        val config = tryOrNull { CarrierMmsConfig.get(context, subId) } ?: return false
        if (!config.mmsEnabled) return false

        val threshold = config.smsToMmsTextThreshold
        if (threshold > 0 && smsParts > threshold) return true

        val lengthThreshold =
            config.smsToMmsTextLengthThreshold.let { threshold ->
                val is16Bit =
                    tryOrNull(false) {
                        SmsMessage.calculateLength(body, false)[3] == SmsMessage.ENCODING_16BIT
                    } == true
                if (threshold > 0 && is16Bit) threshold / 2 else threshold
            }
        if (lengthThreshold > 0 && body.length > lengthThreshold) return true

        return addresses.any(::isEmailAddress) && config.emailGatewayNumber.isEmpty()
    }

    private fun isEmailAddress(address: String) = address.contains('@')

    private fun userMaxMessageSize(subId: Int): Int? {
        val chosen = prefs.mmsSize.get()
        if (chosen == -1) return null

        val carrierLimit = CarrierMmsConfig.get(context, subId).maxMessageSize
        if (chosen != 0 && chosen * 1024 <= carrierLimit) return null

        MmsConfig.init(context)
        return MmsConfig.getMaxMessageSize()
    }

    override fun downloadMms(message: Message, automatic: Boolean) =
        downloadMms(message.getUri(), message.subId, automatic)

    override fun downloadMms(notificationUri: Uri, subId: Int, automatic: Boolean) {
        val location =
            context.contentResolver
                .query(
                    notificationUri,
                    arrayOf(Mms.CONTENT_LOCATION, Mms.MESSAGE_TYPE, Mms.STATUS),
                    null,
                    null,
                    null,
                )
                ?.use { cursor ->
                    when {
                        !cursor.moveToFirst() -> null
                        cursor.getInt(1) != Message.MESSAGE_TYPE_NOTIFICATION_IND -> null
                        cursor.getInt(2) == Message.MMS_STATUS_PERMANENT_FAILURE -> null
                        else -> cursor.getString(0)
                    }
                }

        if (location.isNullOrEmpty()) {
            Timber.w("$notificationUri isn't an MMS waiting to be downloaded")
            return
        }

        DownloadManager.getInstance()
            .downloadMultimediaMessage(context, location, notificationUri, automatic, subId)
    }

    override fun getPendingMmsDownloads(since: Long): List<Message> =
        Realm.getDefaultInstance().use { realm ->
            realm.refresh()
            realm
                .where(Message::class.java)
                .equalTo("type", "mms")
                .equalTo("messageType", Message.MESSAGE_TYPE_NOTIFICATION_IND)
                .notEqualTo("mmsStatus", Message.MMS_STATUS_PERMANENT_FAILURE)
                .greaterThan("date", since)
                .findAll()
                .let(realm::copyFromRealm)
        }

    override fun getUnsentSms(): List<Message> =
        Realm.getDefaultInstance().use { realm ->
            realm.refresh()
            realm
                .where(Message::class.java)
                .equalTo("type", "sms")
                .equalTo("boxId", Sms.MESSAGE_TYPE_OUTBOX)
                .findAll()
                .let(realm::copyFromRealm)
        }

    override fun scheduleDelayedSms(id: Long, sendTime: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exact =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        // Only ever one of the two, or the message would go twice
        try {
            if (exact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    sendTime,
                    getIntentForDelayedSms(id),
                )
                DelayedSmsWorker.cancel(context, id)
                return
            }
        } catch (e: SecurityException) {
            Timber.w(e, "Exact alarm access was taken away")
        }

        alarmManager.cancel(getIntentForDelayedSms(id))
        DelayedSmsWorker.schedule(context, id, sendTime - now())
    }

    override fun cancelDelayedSms(id: Long): Boolean {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(
            getIntentForDelayedSms(id)
        )

        val waiting =
            Realm.getDefaultInstance().use { realm ->
                realm.refresh()
                realm.where(Message::class.java).equalTo("id", id).findFirst()?.let { message ->
                    message.isSms() &&
                        message.boxId == Sms.MESSAGE_TYPE_OUTBOX &&
                        message.date > now()
                } ?: false
            }
        if (waiting) DelayedSmsWorker.cancel(context, id)
        return waiting
    }

    override fun sendDelayedSmsNow(id: Long) {
        if (!cancelDelayedSms(id)) return

        val date = now()
        val message =
            Realm.getDefaultInstance().use { realm ->
                realm.refresh()
                val message =
                    realm.where(Message::class.java).equalTo("id", id).findFirst() ?: return
                realm.executeTransaction { message.date = date }
                realm.copyFromRealm(message)
            }
        tryOrNull {
            context.contentResolver.update(
                message.getUri(),
                contentValuesOf(Sms.DATE to date),
                null,
                null,
            )
        }

        sendSms(message)
    }

    private fun getIntentForDelayedSms(id: Long) =
        PendingIntent.getBroadcast(
            context,
            id.toInt(),
            Intent(context, SendSmsReceiver::class.java).putExtra("id", id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    override fun insertSentSms(
        subId: Int,
        threadId: Long,
        address: String,
        body: String,
        date: Long,
    ): Message {
        // Check if this is a reaction message before inserting
        val parsedReaction = reactions.parseEmojiReaction(body)

        // Insert the message to Realm
        val message =
            Message().apply {
                this.threadId = threadId
                this.address = address
                this.body = body
                this.date = date
                this.subId = subId

                id = messageIds.newId()
                boxId = Sms.MESSAGE_TYPE_OUTBOX
                type = "sms"
                read = true
                seen = true
                // Mark as reaction upfront so it's hidden from the start
                isEmojiReaction = parsedReaction != null
            }

        // Insert the message to the native content provider
        val values =
            contentValuesOf(
                Sms.ADDRESS to address,
                Sms.BODY to body,
                Sms.DATE to System.currentTimeMillis(),
                Sms.READ to true,
                Sms.SEEN to true,
                Sms.TYPE to Sms.MESSAGE_TYPE_OUTBOX,
                Sms.THREAD_ID to threadId,
            )

        if (prefs.canUseSubId.get()) values.put(Sms.SUBSCRIPTION_ID, message.subId)

        var uri: Uri?
        var managedMessage: Message? = null

        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction { managedMessage = realm.copyToRealmOrUpdate(message) }

            // Update the contentId after the message has been inserted to the content provider
            // The message might have been deleted by now, so only proceed if it's valid
            //
            // do this after inserting the message because it might be slow, and we want the message
            // to be inserted into Realm immediately. We don't need to do this after receiving one
            uri =
                context.contentResolver.insert(Sms.CONTENT_URI, values)?.apply {
                    lastPathSegment?.toLong()?.let { id ->
                        realm.executeTransaction {
                            managedMessage?.takeIf { it.isValid }?.contentId = id
                        }
                    }
                }

            // If this is a reaction message, find the target and link them
            if (parsedReaction != null) {
                managedMessage?.let { savedMessage ->
                    val targetMessage =
                        reactions.findTargetMessage(
                            savedMessage.threadId,
                            parsedReaction.originalMessage,
                            realm,
                        )
                    realm.executeTransaction {
                        reactions.saveEmojiReaction(
                            savedMessage,
                            parsedReaction,
                            targetMessage,
                            realm,
                        )
                    }
                }
            }
        }

        // On some devices, we can't obtain a threadId until after the first message is sent in a
        // conversation. In this case, update the message's threadId after it gets added
        // to the native ContentProvider
        if (threadId == 0L) uri?.let(syncRepository::syncMessage)

        return message
    }

    /**
     * Pre-insert a MMS message into both Realm and the system content provider before sending. This
     * ensures the message appears as "Sending" immediately in the UI.
     *
     * @return Pair of the inserted Message and the content URI for use in
     *   Transaction.sendNewMessage()
     */
    private fun insertSentMms(
        subId: Int,
        threadId: Long,
        addresses: List<String>,
        parts: List<MMSPart>,
        subject: String?,
    ): Pair<Message, Uri> {
        val sendReq = buildSendReq(subId, addresses, subject, parts)
        val persister = PduPersister.getPduPersister(context)
        val messageUri =
            persister.persist(
                sendReq,
                Uri.parse("content://mms/outbox"),
                threadId,
                true,
                true,
                null,
            )

        val contentId = messageUri.lastPathSegment?.toLongOrNull() ?: 0L

        if (subId >= 0 && prefs.canUseSubId.get()) {
            tryOrNull {
                context.contentResolver.update(
                    messageUri,
                    contentValuesOf(Mms.SUBSCRIPTION_ID to subId),
                    null,
                    null,
                )
            }
        }

        // Query parts from the content provider
        val mmsParts = mutableListOf<MmsPart>()
        cursorToPart.getPartsCursor(contentId)?.use { cursor ->
            while (cursor.moveToNext()) {
                mmsParts.add(cursorToPart.map(cursor))
            }
        }

        // Insert the message to Realm
        val message =
            Message().apply {
                this.threadId = threadId
                this.address = addresses.joinToString()
                this.body =
                    parts.find { it.mimeType == ContentType.TEXT_PLAIN }?.data?.let { String(it) }
                        ?: ""
                this.date = System.currentTimeMillis()
                this.dateSent = System.currentTimeMillis()
                this.subId = subId
                this.subject = subject ?: ""

                id = messageIds.newId()
                this.contentId = contentId
                boxId = Mms.MESSAGE_BOX_OUTBOX
                type = "mms"
                read = true
                seen = true
            }

        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                // Add parts to the message
                mmsParts.forEach { part -> message.parts.add(realm.copyToRealmOrUpdate(part)) }
                realm.copyToRealmOrUpdate(message)
            }
        }

        return Pair(message, messageUri)
    }

    private fun buildSendReq(
        subId: Int,
        recipients: List<String>,
        subject: String?,
        parts: List<MMSPart>,
    ): SendReq {
        val req = SendReq()

        Utils.getMyPhoneNumber(context, subId)
            ?.takeIf(String::isNotEmpty)
            ?.let(::EncodedStringValue)
            ?.let(req::setFrom)
        recipients.map(::EncodedStringValue).forEach(req::addTo)
        subject?.takeIf(String::isNotEmpty)?.let(::EncodedStringValue)?.let(req::setSubject)

        req.date = System.currentTimeMillis() / 1000
        req.body = PduBody()

        parts.map(this::partToPduPart).forEach { req.body.addPart(it) }

        // SMIL document for compatibility
        req.body.addPart(
            0,
            PduPart().apply {
                contentId = "smil".toByteArray()
                contentLocation = "smil.xml".toByteArray()
                contentType = ContentType.APP_SMIL.toByteArray()
                data =
                    ByteArrayOutputStream()
                        .apply {
                            SmilXmlSerializer.serialize(
                                SmilHelper.createSmilDocument(req.body),
                                this,
                            )
                        }
                        .toByteArray()
            },
        )

        req.messageSize = parts.mapNotNull { it.data?.size }.sum().toLong()
        req.messageClass = PduHeaders.MESSAGE_CLASS_PERSONAL_STR.toByteArray()
        req.expiry = Transaction.DEFAULT_EXPIRY_TIME

        try {
            req.priority = Transaction.DEFAULT_PRIORITY
            req.deliveryReport = PduHeaders.VALUE_NO
            req.readReport = PduHeaders.VALUE_NO
        } catch (e: Exception) {
            Timber.w(e)
        }

        return req
    }

    private fun partToPduPart(part: MMSPart): PduPart =
        PduPart().apply {
            val filename = part.name

            if (part.mimeType.startsWith("text")) {
                charset = CharacterSets.UTF_8
            }

            contentType = part.mimeType.toByteArray()
            contentLocation = filename.toByteArray()
            val index = filename.lastIndexOf(".")
            contentId = (if (index == -1) filename else filename.substring(0, index)).toByteArray()
            data = part.data
        }

    private fun deleteReplacedSms(subId: Int, address: String, protocol: Int) {
        val subscription = if (prefs.canUseSubId.get()) " AND ${Sms.SUBSCRIPTION_ID} = ?" else ""
        val args =
            listOfNotNull(
                address,
                address,
                protocol.toString(),
                subId.toString().takeIf { subscription.isNotEmpty() },
            )
        val query = { addressMatch: String ->
            context.contentResolver.query(
                Sms.CONTENT_URI,
                arrayOf(Sms._ID),
                "($addressMatch) AND ${Sms.PROTOCOL} = ?$subscription",
                args.toTypedArray(),
                null,
            )
        }
        // Older providers may not know PHONE_NUMBERS_EQUAL with a minimum match
        val replaced =
            (tryOrNull(false) {
                    query("${Sms.ADDRESS} = ? OR PHONE_NUMBERS_EQUAL(?, ${Sms.ADDRESS}, 1, 7)")
                } ?: query("${Sms.ADDRESS} = ? OR ${Sms.ADDRESS} = ?"))
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null } ?: return

        val uri = ContentUris.withAppendedId(Sms.CONTENT_URI, replaced)
        context.contentResolver.delete(uri, null, null)
        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                realm
                    .where(Message::class.java)
                    .equalTo("type", "sms")
                    .equalTo("contentId", replaced)
                    .findAll()
                    .deleteAllFromRealm()
            }
        }
    }

    override fun insertReceivedSms(
        subId: Int,
        address: String,
        body: String,
        sentTime: Long,
        details: ReceivedSmsDetails,
    ): Message {
        // Check if this is a reaction message before inserting
        val parsedReaction = tryOrNull { reactions.parseEmojiReaction(body) }

        val date =
            if (Calendar.getInstance().get(Calendar.YEAR) < 2025 && sentTime > 0) sentTime
            else System.currentTimeMillis()

        if (details.replace) tryOrNull { deleteReplacedSms(subId, address, details.protocol) }

        val values =
            contentValuesOf(
                Sms.ADDRESS to address,
                Sms.BODY to body,
                Sms.DATE to date,
                Sms.DATE_SENT to sentTime,
                Sms.READ to 0,
                Sms.SEEN to 1,
                Sms.PROTOCOL to details.protocol,
                Sms.REPLY_PATH_PRESENT to if (details.replyPathPresent) 1 else 0,
            )
        details.serviceCenter
            ?.takeIf { it.isNotEmpty() }
            ?.let { values.put(Sms.SERVICE_CENTER, it) }
        details.subject?.takeIf { it.isNotEmpty() }?.let { values.put(Sms.SUBJECT, it) }
        if (prefs.canUseSubId.get()) values.put(Sms.SUBSCRIPTION_ID, subId)
        val uri = context.contentResolver.insert(Sms.Inbox.CONTENT_URI, values)

        val providerThreadId =
            uri?.let {
                tryOrNull {
                    context.contentResolver.query(it, arrayOf(Sms.THREAD_ID), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getLong(0) else null
                    }
                }
            }
        val threadId =
            providerThreadId
                ?: tryOrNull { TelephonyCompat.getOrCreateThreadId(context, address) }
                ?: 0L

        // Insert the message to Realm
        val message =
            Message().apply {
                this.address = address
                this.body = body
                this.dateSent = sentTime
                this.date = date
                this.subId = subId

                id = messageIds.newId()
                contentId = uri?.lastPathSegment?.toLongOrNull() ?: 0L
                this.threadId = threadId
                boxId = Sms.MESSAGE_TYPE_INBOX
                type = "sms"
                read = activeConversationManager.getActiveConversation() == threadId
                // Mark as reaction upfront so it's hidden from the start
                isEmojiReaction = parsedReaction != null
            }

        if (message.read) {
            uri?.let {
                tryOrNull {
                    context.contentResolver.update(it, contentValuesOf(Sms.READ to 1), null, null)
                }
            }
        }

        Realm.getDefaultInstance().use { realm ->
            var managedMessage: Message? = null
            realm.executeTransaction { managedMessage = realm.copyToRealmOrUpdate(message) }

            // If this is a reaction message, find the target and link them
            if (parsedReaction != null) {
                managedMessage?.let { savedMessage ->
                    val targetMessage =
                        reactions.findTargetMessage(
                            savedMessage.threadId,
                            parsedReaction.originalMessage,
                            realm,
                        )
                    realm.executeTransaction {
                        reactions.saveEmojiReaction(
                            savedMessage,
                            parsedReaction,
                            targetMessage,
                            realm,
                        )
                    }
                }
            }
        }

        return message
    }

    /** Marks the message as sending, in case we need to retry sending it */
    override fun markSending(id: Long) =
        Realm.getDefaultInstance().use { realm ->
            realm.refresh()

            realm.where(Message::class.java).equalTo("id", id).findFirst()?.let { message ->
                // Update the message in realm
                realm.executeTransaction {
                    message.boxId =
                        when (message.isSms()) {
                            true -> Sms.MESSAGE_TYPE_OUTBOX
                            false -> Mms.MESSAGE_BOX_OUTBOX
                        }
                }

                // Update the message in the native ContentProvider
                context.contentResolver.update(
                    message.getUri(),
                    when (message.isSms()) {
                        true -> contentValuesOf(Sms.TYPE to Sms.MESSAGE_TYPE_OUTBOX)
                        false -> contentValuesOf(Mms.MESSAGE_BOX to Mms.MESSAGE_BOX_OUTBOX)
                    },
                    null,
                    null,
                )
            }
            Unit
        }

    override fun markSent(id: Long) =
        Realm.getDefaultInstance().use { realm ->
            realm.refresh()

            realm.where(Message::class.java).equalTo("id", id).findFirst()?.let { message ->
                var failed = false
                realm.executeTransaction {
                    failed = message.isSms() && message.boxId == Sms.MESSAGE_TYPE_FAILED
                    if (failed) return@executeTransaction
                    message.boxId =
                        when (message.isSms()) {
                            true -> Sms.MESSAGE_TYPE_SENT
                            false -> Mms.MESSAGE_BOX_SENT
                        }
                }
                if (failed) return@let

                // Update the message in the native ContentProvider
                context.contentResolver.update(
                    message.getUri(),
                    when (message.isSms()) {
                        true -> contentValuesOf(Sms.TYPE to Sms.MESSAGE_TYPE_SENT)
                        false -> contentValuesOf(Mms.MESSAGE_BOX to Mms.MESSAGE_BOX_SENT)
                    },
                    null,
                    null,
                )
            }
            Unit
        }

    override fun markFailed(id: Long, resultCode: Int) =
        Realm.getDefaultInstance().use { realm ->
            realm.refresh()

            realm.where(Message::class.java).equalTo("id", id).findFirst()?.let { message ->
                // Update the message in realm
                realm.executeTransaction {
                    message.boxId =
                        when (message.isSms()) {
                            true -> Sms.MESSAGE_TYPE_FAILED
                            false -> Mms.MESSAGE_BOX_FAILED
                        }
                    message.errorCode = resultCode
                }

                // Update the message in the native ContentProvider
                context.contentResolver.update(
                    message.getUri(),
                    when (message.isSms()) {
                        true ->
                            contentValuesOf(
                                Sms.TYPE to Sms.MESSAGE_TYPE_FAILED,
                                Sms.ERROR_CODE to resultCode,
                            )
                        false -> contentValuesOf(Mms.MESSAGE_BOX to Mms.MESSAGE_BOX_FAILED)
                    },
                    null,
                    null,
                )
            }
            Unit
        }

    override fun markDelivered(id: Long) =
        Realm.getDefaultInstance().use { realm ->
            realm.refresh()

            realm.where(Message::class.java).equalTo("id", id).findFirst()?.let { message ->
                // Delivery reports are SMS-only; skip MMS messages
                if (!message.isSms()) {
                    Timber.w("markDelivered called with MMS message id=$id, skipping")
                    return@let
                }

                // Update the message in realm
                realm.executeTransaction {
                    message.deliveryStatus = Sms.STATUS_COMPLETE
                    message.dateSent = System.currentTimeMillis()
                    message.read = true
                }

                // Update the message in the native ContentProvider
                context.contentResolver.update(
                    message.getUri(),
                    contentValuesOf(
                        Sms.STATUS to Sms.STATUS_COMPLETE,
                        Sms.DATE_SENT to System.currentTimeMillis(),
                        Sms.READ to true,
                    ),
                    null,
                    null,
                )
            }
            Unit
        }

    override fun markDeliveryFailed(id: Long, resultCode: Int) =
        Realm.getDefaultInstance().use { realm ->
            realm.refresh()

            realm.where(Message::class.java).equalTo("id", id).findFirst()?.let { message ->
                // Delivery reports are SMS-only; skip MMS messages
                if (!message.isSms()) {
                    Timber.w("markDeliveryFailed called with MMS message id=$id, skipping")
                    return@let
                }

                // Update the message in realm
                realm.executeTransaction {
                    message.deliveryStatus = Sms.STATUS_FAILED
                    message.dateSent = System.currentTimeMillis()
                    message.read = true
                    message.errorCode = resultCode
                }

                // Update the message in the native ContentProvider
                context.contentResolver.update(
                    message.getUri(),
                    contentValuesOf(
                        Sms.STATUS to Sms.STATUS_FAILED,
                        Sms.DATE_SENT to System.currentTimeMillis(),
                        Sms.READ to true,
                        Sms.ERROR_CODE to resultCode,
                    ),
                    null,
                    null,
                )
            }
            Unit
        }

    override fun deleteMessages(messageIds: Collection<Long>) =
        Realm.getDefaultInstance().use { realm ->
            realm.refresh()

            val messages =
                realm.where(Message::class.java).anyOf("id", messageIds.toLongArray()).findAll()

            val uris = messages.map { it.getUri() }

            realm.executeTransaction { messages.deleteAllFromRealm() }

            uris.forEach { uri -> context.contentResolver.delete(uri, null, null) }
        }

    override fun getOldMessageCounts(maxAgeDays: Int) =
        Realm.getDefaultInstance().use { realm ->
            realm
                .where(Message::class.java)
                .lessThan("date", now() - TimeUnit.DAYS.toMillis(maxAgeDays.toLong()))
                .findAll()
                .groupingBy { message -> message.threadId }
                .eachCount()
        }

    override fun deleteOldMessages(maxAgeDays: Int) =
        Realm.getDefaultInstance().use { realm ->
            val messages =
                realm
                    .where(Message::class.java)
                    .lessThan("date", now() - TimeUnit.DAYS.toMillis(maxAgeDays.toLong()))
                    .findAll()

            val uris = messages.map { it.getUri() }

            realm.executeTransaction { messages.deleteAllFromRealm() }

            uris.forEach { uri -> context.contentResolver.delete(uri, null, null) }
        }
}
