/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 *
 * Factory for creating test MMS data in instrumented tests.
 * Unlike unit test factory, this one can use Realm operations.
 */
package org.prauga.messages.testutil

import android.provider.Telephony.Mms
import io.realm.Realm
import org.prauga.messages.model.Conversation
import org.prauga.messages.model.EmojiReaction
import org.prauga.messages.model.Message
import org.prauga.messages.model.MmsPart
import org.prauga.messages.model.Recipient
import java.util.concurrent.atomic.AtomicLong

/**
 * Factory for creating test MMS-related domain objects in instrumented tests.
 * This factory can properly use Realm operations since it runs on a real device.
 */
object InstrumentedMmsTestDataFactory {

    private val messageIdGenerator = AtomicLong(1000)
    private val partIdGenerator = AtomicLong(2000)
    private val conversationIdGenerator = AtomicLong(100)
    private val reactionIdGenerator = AtomicLong(3000)
    private val recipientIdGenerator = AtomicLong(4000)

    private const val DEFAULT_ADDRESS = "+1234567890"

    /**
     * Resets all ID generators.
     */
    fun resetIdGenerators() {
        messageIdGenerator.set(1000)
        partIdGenerator.set(2000)
        conversationIdGenerator.set(100)
        reactionIdGenerator.set(3000)
        recipientIdGenerator.set(4000)
    }

    // ==================== Message Creation ====================

    /**
     * Creates and persists a text MMS message.
     */
    fun createTextMms(
        realm: Realm,
        text: String = "Test MMS message",
        address: String = DEFAULT_ADDRESS,
        threadId: Long = conversationIdGenerator.get()
    ): Message {
        var message: Message? = null
        realm.executeTransaction { r ->
            message = r.createObject(Message::class.java, messageIdGenerator.incrementAndGet())
            message?.apply {
                this.threadId = threadId
                this.address = address
                this.body = text
                this.date = System.currentTimeMillis()
                this.dateSent = System.currentTimeMillis()
                this.type = "mms"
                this.boxId = Mms.MESSAGE_BOX_INBOX
                this.read = false
                this.seen = false

                // Add text part
                val part = r.createObject(MmsPart::class.java, partIdGenerator.incrementAndGet())
                part.type = "text/plain"
                part.text = text
                this.parts.add(part)
            }
        }
        return message!!
    }

    /**
     * Creates and persists an MMS message with image.
     */
    fun createImageMms(
        realm: Realm,
        text: String = "",
        imageType: String = "image/jpeg",
        address: String = DEFAULT_ADDRESS,
        threadId: Long = conversationIdGenerator.get()
    ): Message {
        var message: Message? = null
        realm.executeTransaction { r ->
            message = r.createObject(Message::class.java, messageIdGenerator.incrementAndGet())
            message?.apply {
                this.threadId = threadId
                this.address = address
                this.body = text
                this.date = System.currentTimeMillis()
                this.dateSent = System.currentTimeMillis()
                this.type = "mms"
                this.boxId = Mms.MESSAGE_BOX_INBOX
                this.read = false
                this.seen = false

                if (text.isNotEmpty()) {
                    val textPart = r.createObject(MmsPart::class.java, partIdGenerator.incrementAndGet())
                    textPart.type = "text/plain"
                    textPart.text = text
                    this.parts.add(textPart)
                }

                val imagePart = r.createObject(MmsPart::class.java, partIdGenerator.incrementAndGet())
                imagePart.type = imageType
                imagePart.name = "image.jpg"
                this.parts.add(imagePart)
            }
        }
        return message!!
    }

    /**
     * Creates and persists an MMS message with video.
     */
    fun createVideoMms(
        realm: Realm,
        text: String = "",
        videoType: String = "video/mp4",
        address: String = DEFAULT_ADDRESS,
        threadId: Long = conversationIdGenerator.get()
    ): Message {
        var message: Message? = null
        realm.executeTransaction { r ->
            message = r.createObject(Message::class.java, messageIdGenerator.incrementAndGet())
            message?.apply {
                this.threadId = threadId
                this.address = address
                this.body = text
                this.date = System.currentTimeMillis()
                this.dateSent = System.currentTimeMillis()
                this.type = "mms"
                this.boxId = Mms.MESSAGE_BOX_INBOX
                this.read = false
                this.seen = false

                if (text.isNotEmpty()) {
                    val textPart = r.createObject(MmsPart::class.java, partIdGenerator.incrementAndGet())
                    textPart.type = "text/plain"
                    textPart.text = text
                    this.parts.add(textPart)
                }

                val videoPart = r.createObject(MmsPart::class.java, partIdGenerator.incrementAndGet())
                videoPart.type = videoType
                videoPart.name = "video.mp4"
                this.parts.add(videoPart)
            }
        }
        return message!!
    }

    /**
     * Creates and persists an MMS message with audio.
     */
    fun createAudioMms(
        realm: Realm,
        text: String = "",
        audioType: String = "audio/mp3",
        address: String = DEFAULT_ADDRESS,
        threadId: Long = conversationIdGenerator.get()
    ): Message {
        var message: Message? = null
        realm.executeTransaction { r ->
            message = r.createObject(Message::class.java, messageIdGenerator.incrementAndGet())
            message?.apply {
                this.threadId = threadId
                this.address = address
                this.body = text
                this.date = System.currentTimeMillis()
                this.dateSent = System.currentTimeMillis()
                this.type = "mms"
                this.boxId = Mms.MESSAGE_BOX_INBOX
                this.read = false
                this.seen = false

                if (text.isNotEmpty()) {
                    val textPart = r.createObject(MmsPart::class.java, partIdGenerator.incrementAndGet())
                    textPart.type = "text/plain"
                    textPart.text = text
                    this.parts.add(textPart)
                }

                val audioPart = r.createObject(MmsPart::class.java, partIdGenerator.incrementAndGet())
                audioPart.type = audioType
                audioPart.name = "audio.mp3"
                this.parts.add(audioPart)
            }
        }
        return message!!
    }

    /**
     * Creates and persists a group MMS message.
     */
    fun createGroupMms(
        realm: Realm,
        recipients: List<String> = listOf("+15551111111", "+15552222222", "+15553333333"),
        text: String = "Group message",
        threadId: Long = conversationIdGenerator.get()
    ): Message {
        var message: Message? = null
        realm.executeTransaction { r ->
            message = r.createObject(Message::class.java, messageIdGenerator.incrementAndGet())
            message?.apply {
                this.threadId = threadId
                this.address = recipients.joinToString(", ")
                this.body = text
                this.date = System.currentTimeMillis()
                this.dateSent = System.currentTimeMillis()
                this.type = "mms"
                this.boxId = Mms.MESSAGE_BOX_INBOX
                this.read = false
                this.seen = false

                val textPart = r.createObject(MmsPart::class.java, partIdGenerator.incrementAndGet())
                textPart.type = "text/plain"
                textPart.text = text
                this.parts.add(textPart)
            }
        }
        return message!!
    }

    /**
     * Creates and persists an outgoing MMS message.
     */
    fun createOutgoingMms(
        realm: Realm,
        text: String = "Outgoing MMS",
        address: String = DEFAULT_ADDRESS,
        threadId: Long = conversationIdGenerator.get(),
        boxId: Int = Mms.MESSAGE_BOX_SENT
    ): Message {
        var message: Message? = null
        realm.executeTransaction { r ->
            message = r.createObject(Message::class.java, messageIdGenerator.incrementAndGet())
            message?.apply {
                this.threadId = threadId
                this.address = address
                this.body = text
                this.date = System.currentTimeMillis()
                this.dateSent = System.currentTimeMillis()
                this.type = "mms"
                this.boxId = boxId
                this.read = true
                this.seen = true

                val textPart = r.createObject(MmsPart::class.java, partIdGenerator.incrementAndGet())
                textPart.type = "text/plain"
                textPart.text = text
                this.parts.add(textPart)
            }
        }
        return message!!
    }

    /**
     * Creates and persists an MMS with subject.
     */
    fun createMmsWithSubject(
        realm: Realm,
        subject: String = "Test Subject",
        text: String = "Message body",
        address: String = DEFAULT_ADDRESS,
        threadId: Long = conversationIdGenerator.get()
    ): Message {
        var message: Message? = null
        realm.executeTransaction { r ->
            message = r.createObject(Message::class.java, messageIdGenerator.incrementAndGet())
            message?.apply {
                this.threadId = threadId
                this.address = address
                this.body = text
                this.subject = subject
                this.date = System.currentTimeMillis()
                this.dateSent = System.currentTimeMillis()
                this.type = "mms"
                this.boxId = Mms.MESSAGE_BOX_INBOX
                this.read = false
                this.seen = false

                val textPart = r.createObject(MmsPart::class.java, partIdGenerator.incrementAndGet())
                textPart.type = "text/plain"
                textPart.text = text
                this.parts.add(textPart)
            }
        }
        return message!!
    }

    // ==================== Conversation Creation ====================

    /**
     * Creates and persists a conversation.
     */
    fun createConversation(
        realm: Realm,
        address: String = DEFAULT_ADDRESS
    ): Conversation {
        var conversation: Conversation? = null
        realm.executeTransaction { r ->
            conversation = r.createObject(Conversation::class.java, conversationIdGenerator.incrementAndGet())

            val recipient = r.createObject(Recipient::class.java, recipientIdGenerator.incrementAndGet())
            recipient.address = address
            conversation?.recipients?.add(recipient)
        }
        return conversation!!
    }

    /**
     * Creates and persists a group conversation.
     */
    fun createGroupConversation(
        realm: Realm,
        addresses: List<String> = listOf("+15551111111", "+15552222222", "+15553333333")
    ): Conversation {
        var conversation: Conversation? = null
        realm.executeTransaction { r ->
            conversation = r.createObject(Conversation::class.java, conversationIdGenerator.incrementAndGet())

            addresses.forEach { address ->
                val recipient = r.createObject(Recipient::class.java, recipientIdGenerator.incrementAndGet())
                recipient.address = address
                conversation?.recipients?.add(recipient)
            }
        }
        return conversation!!
    }

    // ==================== EmojiReaction Creation ====================

    /**
     * Creates and persists an emoji reaction.
     */
    fun createEmojiReaction(
        realm: Realm,
        emoji: String = "👍",
        senderAddress: String = DEFAULT_ADDRESS,
        threadId: Long = conversationIdGenerator.get()
    ): EmojiReaction {
        var reaction: EmojiReaction? = null
        realm.executeTransaction { r ->
            reaction = r.createObject(EmojiReaction::class.java, reactionIdGenerator.incrementAndGet())
            reaction?.apply {
                this.emoji = emoji
                this.senderAddress = senderAddress
                this.threadId = threadId
            }
        }
        return reaction!!
    }

    // ==================== Helper Methods ====================

    /**
     * Creates standalone MmsPart (not persisted).
     */
    fun createTextPart(text: String = "Test text"): MmsPart {
        return MmsPart().apply {
            id = partIdGenerator.incrementAndGet()
            type = "text/plain"
            this.text = text
        }
    }

    /**
     * Creates standalone MmsPart for image (not persisted).
     */
    fun createImagePart(mimeType: String = "image/jpeg"): MmsPart {
        return MmsPart().apply {
            id = partIdGenerator.incrementAndGet()
            type = mimeType
            name = "image.jpg"
        }
    }

    /**
     * Creates standalone MmsPart for video (not persisted).
     */
    fun createVideoPart(mimeType: String = "video/mp4"): MmsPart {
        return MmsPart().apply {
            id = partIdGenerator.incrementAndGet()
            type = mimeType
            name = "video.mp4"
        }
    }

    /**
     * Creates standalone MmsPart for audio (not persisted).
     */
    fun createAudioPart(mimeType: String = "audio/mp3"): MmsPart {
        return MmsPart().apply {
            id = partIdGenerator.incrementAndGet()
            type = mimeType
            name = "audio.mp3"
        }
    }
}
