/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 *
 * Instrumented tests for MMS send flow.
 */
package org.prauga.messages.mms

import android.content.Context
import android.provider.Telephony.Mms
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.realm.Realm
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.messages.model.Message
import org.prauga.messages.model.MmsPart
import org.prauga.messages.testutil.InstrumentedMmsTestDataFactory
import org.prauga.messages.testutil.RealmTestHelper

/**
 * Instrumented tests for MMS send flow.
 * Tests message creation, persistence, and retrieval using Realm.
 */
@RunWith(AndroidJUnit4::class)
class MmsSendFlowInstrumentedTest {

    private lateinit var context: Context
    private lateinit var realm: Realm

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        RealmTestHelper.init(context)
        realm = RealmTestHelper.getRealm()
        InstrumentedMmsTestDataFactory.resetIdGenerators()
    }

    @After
    fun tearDown() {
        realm.close()
        RealmTestHelper.clearAll()
        RealmTestHelper.tearDown()
    }

    // ==================== Text MMS Tests ====================

    @Test
    fun givenTextMms_whenCreate_thenPersistedInRealm() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, "Hello World!")

        assertThat(message.id).isGreaterThan(0L)
        assertThat(message.body).isEqualTo("Hello World!")
        assertThat(message.type).isEqualTo("mms")
    }

    @Test
    fun givenTextMms_whenCreate_thenHasTextPart() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, "Test message")

        assertThat(message.parts).isNotEmpty()
        assertThat(message.parts.size).isEqualTo(1)
        assertThat(message.parts[0]!!.type).isEqualTo("text/plain")
        assertThat(message.parts[0]!!.text).isEqualTo("Test message")
    }

    @Test
    fun givenTextMms_whenRetrieve_thenDataMatches() {
        val originalMessage = InstrumentedMmsTestDataFactory.createTextMms(realm, "Retrieve test")
        val messageId = originalMessage.id

        val retrievedMessage = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()

        assertThat(retrievedMessage).isNotNull()
        assertThat(retrievedMessage!!.body).isEqualTo("Retrieve test")
    }

    @Test
    fun givenTextMms_whenCreate_thenHasTimestamp() {
        val beforeTime = System.currentTimeMillis()
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm)
        val afterTime = System.currentTimeMillis()

        assertThat(message.date).isAtLeast(beforeTime)
        assertThat(message.date).isAtMost(afterTime)
    }

    // ==================== Image MMS Tests ====================

    @Test
    fun givenImageMms_whenCreate_thenHasImagePart() {
        val message = InstrumentedMmsTestDataFactory.createImageMms(realm)

        assertThat(message.parts).isNotEmpty()

        val hasImagePart = message.parts.any { it.type?.startsWith("image/") == true }
        assertThat(hasImagePart).isTrue()
    }

    @Test
    fun givenImageMmsWithCaption_whenCreate_thenHasBothParts() {
        val message = InstrumentedMmsTestDataFactory.createImageMms(
            realm,
            text = "Check out this photo!"
        )

        assertThat(message.parts.size).isEqualTo(2)

        val hasTextPart = message.parts.any { it.type == "text/plain" }
        val hasImagePart = message.parts.any { it.type?.startsWith("image/") == true }

        assertThat(hasTextPart).isTrue()
        assertThat(hasImagePart).isTrue()
    }

    @Test
    fun givenJpegImage_whenCreate_thenTypeIsCorrect() {
        val message = InstrumentedMmsTestDataFactory.createImageMms(
            realm,
            imageType = "image/jpeg"
        )

        val imagePart = message.parts.first { it.type?.startsWith("image/") == true }
        assertThat(imagePart.type).isEqualTo("image/jpeg")
    }

    @Test
    fun givenPngImage_whenCreate_thenTypeIsCorrect() {
        val message = InstrumentedMmsTestDataFactory.createImageMms(
            realm,
            imageType = "image/png"
        )

        val imagePart = message.parts.first { it.type?.startsWith("image/") == true }
        assertThat(imagePart.type).isEqualTo("image/png")
    }

    @Test
    fun givenGifImage_whenCreate_thenTypeIsCorrect() {
        val message = InstrumentedMmsTestDataFactory.createImageMms(
            realm,
            imageType = "image/gif"
        )

        val imagePart = message.parts.first { it.type?.startsWith("image/") == true }
        assertThat(imagePart.type).isEqualTo("image/gif")
    }

    // ==================== Video MMS Tests ====================

    @Test
    fun givenVideoMms_whenCreate_thenHasVideoPart() {
        val message = InstrumentedMmsTestDataFactory.createVideoMms(realm)

        assertThat(message.parts).isNotEmpty()

        val hasVideoPart = message.parts.any { it.type?.startsWith("video/") == true }
        assertThat(hasVideoPart).isTrue()
    }

    @Test
    fun givenMp4Video_whenCreate_thenTypeIsCorrect() {
        val message = InstrumentedMmsTestDataFactory.createVideoMms(
            realm,
            videoType = "video/mp4"
        )

        val videoPart = message.parts.first { it.type?.startsWith("video/") == true }
        assertThat(videoPart.type).isEqualTo("video/mp4")
    }

    @Test
    fun given3gppVideo_whenCreate_thenTypeIsCorrect() {
        val message = InstrumentedMmsTestDataFactory.createVideoMms(
            realm,
            videoType = "video/3gpp"
        )

        val videoPart = message.parts.first { it.type?.startsWith("video/") == true }
        assertThat(videoPart.type).isEqualTo("video/3gpp")
    }

    // ==================== Audio MMS Tests ====================

    @Test
    fun givenAudioMms_whenCreate_thenHasAudioPart() {
        val message = InstrumentedMmsTestDataFactory.createAudioMms(realm)

        assertThat(message.parts).isNotEmpty()

        val hasAudioPart = message.parts.any { it.type?.startsWith("audio/") == true }
        assertThat(hasAudioPart).isTrue()
    }

    @Test
    fun givenMp3Audio_whenCreate_thenTypeIsCorrect() {
        val message = InstrumentedMmsTestDataFactory.createAudioMms(
            realm,
            audioType = "audio/mp3"
        )

        val audioPart = message.parts.first { it.type?.startsWith("audio/") == true }
        assertThat(audioPart.type).isEqualTo("audio/mp3")
    }

    @Test
    fun givenAacAudio_whenCreate_thenTypeIsCorrect() {
        val message = InstrumentedMmsTestDataFactory.createAudioMms(
            realm,
            audioType = "audio/aac"
        )

        val audioPart = message.parts.first { it.type?.startsWith("audio/") == true }
        assertThat(audioPart.type).isEqualTo("audio/aac")
    }

    // ==================== Outgoing MMS Tests ====================

    @Test
    fun givenOutgoingMms_whenCreate_thenBoxIdIsSent() {
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(realm)

        assertThat(message.boxId).isEqualTo(Mms.MESSAGE_BOX_SENT)
    }

    @Test
    fun givenOutgoingMms_whenCreate_thenMarkedAsRead() {
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(realm)

        assertThat(message.read).isTrue()
        assertThat(message.seen).isTrue()
    }

    @Test
    fun givenOutgoingMmsInOutbox_whenCreate_thenBoxIdIsOutbox() {
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            boxId = Mms.MESSAGE_BOX_OUTBOX
        )

        assertThat(message.boxId).isEqualTo(Mms.MESSAGE_BOX_OUTBOX)
    }

    // ==================== Subject Tests ====================

    @Test
    fun givenMmsWithSubject_whenCreate_thenSubjectSet() {
        val message = InstrumentedMmsTestDataFactory.createMmsWithSubject(
            realm,
            subject = "Important Message"
        )

        assertThat(message.subject).isEqualTo("Important Message")
    }

    @Test
    fun givenMmsWithSubject_whenCreate_thenBodyAlsoSet() {
        val message = InstrumentedMmsTestDataFactory.createMmsWithSubject(
            realm,
            subject = "Subject",
            text = "Message body"
        )

        assertThat(message.subject).isEqualTo("Subject")
        assertThat(message.body).isEqualTo("Message body")
    }

    // ==================== Thread Tests ====================

    @Test
    fun givenMms_whenCreateWithThreadId_thenThreadIdSet() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            threadId = 999L
        )

        assertThat(message.threadId).isEqualTo(999L)
    }

    @Test
    fun givenMultipleMms_whenCreateInSameThread_thenSameThreadId() {
        val message1 = InstrumentedMmsTestDataFactory.createTextMms(realm, "First", threadId = 100L)
        val message2 = InstrumentedMmsTestDataFactory.createTextMms(realm, "Second", threadId = 100L)

        assertThat(message1.threadId).isEqualTo(message2.threadId)
    }

    // ==================== Query Tests ====================

    @Test
    fun givenMultipleMms_whenQueryByType_thenAllFound() {
        InstrumentedMmsTestDataFactory.createTextMms(realm, "Message 1")
        InstrumentedMmsTestDataFactory.createTextMms(realm, "Message 2")
        InstrumentedMmsTestDataFactory.createTextMms(realm, "Message 3")

        val mmsMessages = realm.where(Message::class.java)
            .equalTo("type", "mms")
            .findAll()

        assertThat(mmsMessages.size).isEqualTo(3)
    }

    @Test
    fun givenMmsInThread_whenQueryByThread_thenFound() {
        InstrumentedMmsTestDataFactory.createTextMms(realm, "In thread", threadId = 500L)
        InstrumentedMmsTestDataFactory.createTextMms(realm, "Different", threadId = 501L)

        val messagesInThread = realm.where(Message::class.java)
            .equalTo("threadId", 500L)
            .findAll()

        assertThat(messagesInThread.size).isEqualTo(1)
        assertThat(messagesInThread[0]!!.body).isEqualTo("In thread")
    }

    @Test
    fun givenInboxMms_whenQueryByBoxId_thenFound() {
        InstrumentedMmsTestDataFactory.createTextMms(realm) // Inbox
        InstrumentedMmsTestDataFactory.createOutgoingMms(realm) // Sent

        val inboxMessages = realm.where(Message::class.java)
            .equalTo("boxId", Mms.MESSAGE_BOX_INBOX)
            .findAll()

        assertThat(inboxMessages.size).isEqualTo(1)
    }

    // ==================== Address Tests ====================

    @Test
    fun givenMms_whenCreateWithAddress_thenAddressSet() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            address = "+15559876543"
        )

        assertThat(message.address).isEqualTo("+15559876543")
    }

    @Test
    fun givenMms_whenQueryByAddress_thenFound() {
        InstrumentedMmsTestDataFactory.createTextMms(realm, address = "+15551111111")
        InstrumentedMmsTestDataFactory.createTextMms(realm, address = "+15552222222")

        val messages = realm.where(Message::class.java)
            .equalTo("address", "+15551111111")
            .findAll()

        assertThat(messages.size).isEqualTo(1)
    }

    // ==================== Part Count Tests ====================

    @Test
    fun givenTextOnlyMms_whenCountParts_thenOnePart() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm)

        assertThat(message.parts.size).isEqualTo(1)
    }

    @Test
    fun givenImageWithCaptionMms_whenCountParts_thenTwoParts() {
        val message = InstrumentedMmsTestDataFactory.createImageMms(realm, text = "Caption")

        assertThat(message.parts.size).isEqualTo(2)
    }

    // ==================== Unicode Tests ====================

    @Test
    fun givenUnicodeText_whenCreate_thenPreserved() {
        val unicodeText = "Hello 你好 مرحبا"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, unicodeText)

        assertThat(message.body).isEqualTo(unicodeText)
    }

    @Test
    fun givenEmojiText_whenCreate_thenPreserved() {
        val emojiText = "Party time! 🎉🎊🥳"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, emojiText)

        assertThat(message.body).isEqualTo(emojiText)
    }

    // ==================== Deletion Tests ====================

    @Test
    fun givenMms_whenDelete_thenNotFound() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm)
        val messageId = message.id

        realm.executeTransaction { r ->
            val toDelete = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            toDelete?.deleteFromRealm()
        }

        val found = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()

        assertThat(found).isNull()
    }

    // ==================== Update Tests ====================

    @Test
    fun givenMms_whenMarkAsRead_thenUpdated() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm)
        val messageId = message.id

        realm.executeTransaction { r ->
            val toUpdate = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            toUpdate?.read = true
            toUpdate?.seen = true
        }

        val updated = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()

        assertThat(updated!!.read).isTrue()
        assertThat(updated.seen).isTrue()
    }
}
