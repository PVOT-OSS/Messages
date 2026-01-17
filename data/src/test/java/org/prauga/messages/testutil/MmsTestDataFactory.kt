/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 *
 * Factory for creating test MMS data objects.
 * Note: For unit tests, we avoid RealmList operations.
 * Full Realm-based tests should use instrumented tests.
 */
package org.prauga.messages.testutil

import android.provider.Telephony.Mms
import io.realm.RealmList
import org.prauga.messages.model.Conversation
import org.prauga.messages.model.EmojiReaction
import org.prauga.messages.model.Message
import org.prauga.messages.model.MmsPart
import org.prauga.messages.model.Recipient
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * Factory for creating test MMS-related domain objects.
 * Provides methods to create Message, MmsPart, Conversation, and EmojiReaction objects
 * for testing purposes.
 *
 * Note: In unit tests, RealmList operations may fail. These tests verify
 * the model structure. For full integration tests with RealmList, use
 * instrumented tests.
 */
object MmsTestDataFactory {

    private val messageIdGenerator = AtomicLong(1000)
    private val partIdGenerator = AtomicLong(2000)
    private val conversationIdGenerator = AtomicLong(100)

    private const val DEFAULT_ADDRESS = "+1234567890"

    /**
     * Resets all ID generators to their initial values.
     * Call this in @Before methods to ensure consistent IDs across tests.
     */
    fun resetIdGenerators() {
        messageIdGenerator.set(1000)
        partIdGenerator.set(2000)
        conversationIdGenerator.set(100)
    }

    /**
     * Returns the next message ID without incrementing.
     */
    fun peekNextMessageId(): Long = messageIdGenerator.get() + 1

    // ==================== Message Creation ====================

    /**
     * Creates a basic text MMS message.
     * Note: parts list is populated but RealmList.add() may fail in unit tests.
     */
    fun createTextMms(
        text: String = "Test MMS message",
        address: String = DEFAULT_ADDRESS,
        threadId: Long = conversationIdGenerator.get()
    ): Message {
        val message = Message()
        message.id = messageIdGenerator.incrementAndGet()
        message.threadId = threadId
        message.address = address
        message.body = text
        message.date = System.currentTimeMillis()
        message.dateSent = System.currentTimeMillis()
        message.type = "mms"
        message.boxId = Mms.MESSAGE_BOX_INBOX
        message.read = false
        message.seen = false

        // Try to add part - this may fail in unit tests without Realm
        try {
            message.parts.add(createTextPart(text))
        } catch (e: Throwable) {
            // RealmList operations fail without Realm initialization
            // In unit tests, parts will be empty
        }

        return message
    }

    /**
     * Creates an MMS message with an image attachment.
     */
    fun createImageMms(
        text: String = "",
        imageType: String = "image/jpeg",
        address: String = DEFAULT_ADDRESS,
        threadId: Long = conversationIdGenerator.get()
    ): Message {
        val message = Message()
        message.id = messageIdGenerator.incrementAndGet()
        message.threadId = threadId
        message.address = address
        message.body = text
        message.date = System.currentTimeMillis()
        message.dateSent = System.currentTimeMillis()
        message.type = "mms"
        message.boxId = Mms.MESSAGE_BOX_INBOX
        message.read = false
        message.seen = false

        try {
            if (text.isNotEmpty()) {
                message.parts.add(createTextPart(text))
            }
            message.parts.add(createImagePart(imageType))
        } catch (e: Throwable) {
            // RealmList operations fail without Realm
        }

        return message
    }

    /**
     * Creates an MMS message with a video attachment.
     */
    fun createVideoMms(
        text: String = "",
        videoType: String = "video/mp4",
        address: String = DEFAULT_ADDRESS,
        threadId: Long = conversationIdGenerator.get()
    ): Message {
        val message = Message()
        message.id = messageIdGenerator.incrementAndGet()
        message.threadId = threadId
        message.address = address
        message.body = text
        message.date = System.currentTimeMillis()
        message.dateSent = System.currentTimeMillis()
        message.type = "mms"
        message.boxId = Mms.MESSAGE_BOX_INBOX
        message.read = false
        message.seen = false

        try {
            if (text.isNotEmpty()) {
                message.parts.add(createTextPart(text))
            }
            message.parts.add(createVideoPart(videoType))
        } catch (e: Throwable) {
            // RealmList operations fail without Realm
        }

        return message
    }

    /**
     * Creates an MMS message with an audio attachment.
     */
    fun createAudioMms(
        text: String = "",
        audioType: String = "audio/mp3",
        address: String = DEFAULT_ADDRESS,
        threadId: Long = conversationIdGenerator.get()
    ): Message {
        val message = Message()
        message.id = messageIdGenerator.incrementAndGet()
        message.threadId = threadId
        message.address = address
        message.body = text
        message.date = System.currentTimeMillis()
        message.dateSent = System.currentTimeMillis()
        message.type = "mms"
        message.boxId = Mms.MESSAGE_BOX_INBOX
        message.read = false
        message.seen = false

        try {
            if (text.isNotEmpty()) {
                message.parts.add(createTextPart(text))
            }
            message.parts.add(createAudioPart(audioType))
        } catch (e: Throwable) {
            // RealmList operations fail without Realm
        }

        return message
    }

    /**
     * Creates an MMS message with a vCard attachment.
     */
    fun createVCardMms(
        text: String = "",
        address: String = DEFAULT_ADDRESS,
        threadId: Long = conversationIdGenerator.get()
    ): Message {
        val message = Message()
        message.id = messageIdGenerator.incrementAndGet()
        message.threadId = threadId
        message.address = address
        message.body = text
        message.date = System.currentTimeMillis()
        message.dateSent = System.currentTimeMillis()
        message.type = "mms"
        message.boxId = Mms.MESSAGE_BOX_INBOX
        message.read = false
        message.seen = false

        try {
            if (text.isNotEmpty()) {
                message.parts.add(createTextPart(text))
            }
            message.parts.add(createVCardPart())
        } catch (e: Throwable) {
            // RealmList operations fail without Realm
        }

        return message
    }

    /**
     * Creates a group MMS message with multiple recipients.
     */
    fun createGroupMms(
        recipients: List<String> = listOf("+15551111111", "+15552222222", "+15553333333"),
        text: String = "Group message",
        threadId: Long = conversationIdGenerator.get()
    ): Message {
        val message = Message()
        message.id = messageIdGenerator.incrementAndGet()
        message.threadId = threadId
        message.address = recipients.joinToString(", ")
        message.body = text
        message.date = System.currentTimeMillis()
        message.dateSent = System.currentTimeMillis()
        message.type = "mms"
        message.boxId = Mms.MESSAGE_BOX_INBOX
        message.read = false
        message.seen = false

        try {
            message.parts.add(createTextPart(text))
        } catch (e: Throwable) {
            // RealmList operations fail without Realm
        }

        return message
    }

    /**
     * Creates an outgoing MMS message.
     */
    fun createOutgoingMms(
        address: String = DEFAULT_ADDRESS,
        text: String = "Outgoing MMS",
        threadId: Long = conversationIdGenerator.get(),
        status: Int = Mms.MESSAGE_BOX_SENT
    ): Message {
        val message = Message()
        message.id = messageIdGenerator.incrementAndGet()
        message.threadId = threadId
        message.address = address
        message.body = text
        message.date = System.currentTimeMillis()
        message.dateSent = System.currentTimeMillis()
        message.type = "mms"
        message.boxId = status
        message.read = true
        message.seen = true

        try {
            message.parts.add(createTextPart(text))
        } catch (e: Throwable) {
            // RealmList operations fail without Realm
        }

        return message
    }

    /**
     * Creates an MMS with a subject line.
     */
    fun createMmsWithSubject(
        subject: String = "Test Subject",
        text: String = "Message with subject",
        address: String = DEFAULT_ADDRESS,
        threadId: Long = conversationIdGenerator.get()
    ): Message {
        val message = Message()
        message.id = messageIdGenerator.incrementAndGet()
        message.threadId = threadId
        message.address = address
        message.body = text
        message.subject = subject
        message.date = System.currentTimeMillis()
        message.dateSent = System.currentTimeMillis()
        message.type = "mms"
        message.boxId = Mms.MESSAGE_BOX_INBOX
        message.read = false
        message.seen = false

        try {
            message.parts.add(createTextPart(text))
        } catch (e: Throwable) {
            // RealmList operations fail without Realm
        }

        return message
    }

    // ==================== MmsPart Creation ====================

    /**
     * Creates a text part.
     */
    fun createTextPart(text: String = "Test text"): MmsPart {
        val part = MmsPart()
        part.id = partIdGenerator.incrementAndGet()
        part.type = "text/plain"
        part.text = text

        return part
    }

    /**
     * Creates an image part.
     */
    fun createImagePart(mimeType: String = "image/jpeg"): MmsPart {
        val part = MmsPart()
        part.id = partIdGenerator.incrementAndGet()
        part.type = mimeType
        part.name = "image.${getExtension(mimeType)}"

        return part
    }

    /**
     * Creates a video part.
     */
    fun createVideoPart(mimeType: String = "video/mp4"): MmsPart {
        val part = MmsPart()
        part.id = partIdGenerator.incrementAndGet()
        part.type = mimeType
        part.name = "video.${getExtension(mimeType)}"

        return part
    }

    /**
     * Creates an audio part.
     */
    fun createAudioPart(mimeType: String = "audio/mp3"): MmsPart {
        val part = MmsPart()
        part.id = partIdGenerator.incrementAndGet()
        part.type = mimeType
        part.name = "audio.${getExtension(mimeType)}"

        return part
    }

    /**
     * Creates a vCard part.
     */
    fun createVCardPart(): MmsPart {
        val part = MmsPart()
        part.id = partIdGenerator.incrementAndGet()
        part.type = "text/x-vCard"
        part.name = "contact.vcf"

        return part
    }

    // ==================== Conversation Creation ====================

    /**
     * Creates a basic conversation.
     */
    fun createConversation(
        address: String = DEFAULT_ADDRESS,
        threadId: Long = conversationIdGenerator.incrementAndGet()
    ): Conversation {
        val conversation = Conversation()
        conversation.id = threadId

        try {
            val recipient = Recipient()
            recipient.address = address
            conversation.recipients.add(recipient)
        } catch (e: Throwable) {
            // RealmList operations fail without Realm
        }

        return conversation
    }

    /**
     * Creates a group conversation with multiple recipients.
     */
    fun createGroupConversation(
        addresses: List<String> = listOf("+15551111111", "+15552222222", "+15553333333"),
        threadId: Long = conversationIdGenerator.incrementAndGet()
    ): Conversation {
        val conversation = Conversation()
        conversation.id = threadId

        try {
            addresses.forEach { address ->
                val recipient = Recipient()
                recipient.address = address
                conversation.recipients.add(recipient)
            }
        } catch (e: Throwable) {
            // RealmList operations fail without Realm
        }

        return conversation
    }

    // ==================== EmojiReaction Creation ====================

    /**
     * Creates an emoji reaction.
     */
    fun createEmojiReaction(
        emoji: String = "👍",
        senderAddress: String = DEFAULT_ADDRESS,
        threadId: Long = conversationIdGenerator.get()
    ): EmojiReaction {
        val reaction = EmojiReaction()
        reaction.emoji = emoji
        reaction.senderAddress = senderAddress
        reaction.threadId = threadId

        return reaction
    }

    // ==================== Helper Methods ====================

    private fun getExtension(mimeType: String): String {
        return when {
            mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
            mimeType.contains("png") -> "png"
            mimeType.contains("gif") -> "gif"
            mimeType.contains("mp4") -> "mp4"
            mimeType.contains("3gpp") -> "3gp"
            mimeType.contains("mp3") -> "mp3"
            mimeType.contains("aac") -> "aac"
            mimeType.contains("amr") -> "amr"
            mimeType.contains("ogg") -> "ogg"
            else -> "bin"
        }
    }
}
