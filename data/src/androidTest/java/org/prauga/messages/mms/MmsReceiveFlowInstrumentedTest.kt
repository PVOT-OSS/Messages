/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 *
 * Instrumented tests for MMS receive flow.
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
 * Instrumented tests for MMS receive flow.
 * Tests incoming message handling, storage, and retrieval using Realm.
 */
@RunWith(AndroidJUnit4::class)
class MmsReceiveFlowInstrumentedTest {

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

    // ==================== Basic Receive Tests ====================

    @Test
    fun givenIncomingTextMms_whenReceived_thenStoredInInbox() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, "Incoming message")

        assertThat(message.boxId).isEqualTo(Mms.MESSAGE_BOX_INBOX)
    }

    @Test
    fun givenIncomingMms_whenReceived_thenMarkedAsUnread() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm)

        assertThat(message.read).isFalse()
        assertThat(message.seen).isFalse()
    }

    @Test
    fun givenIncomingMms_whenReceived_thenHasSenderAddress() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            address = "+15559876543"
        )

        assertThat(message.address).isEqualTo("+15559876543")
    }

    @Test
    fun givenIncomingMms_whenReceived_thenHasTimestamp() {
        val beforeTime = System.currentTimeMillis()
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm)
        val afterTime = System.currentTimeMillis()

        assertThat(message.date).isAtLeast(beforeTime)
        assertThat(message.date).isAtMost(afterTime)
    }

    // ==================== Image Receive Tests ====================

    @Test
    fun givenIncomingImageMms_whenReceived_thenHasImagePart() {
        val message = InstrumentedMmsTestDataFactory.createImageMms(realm)

        val hasImagePart = message.parts.any { it.type?.startsWith("image/") == true }
        assertThat(hasImagePart).isTrue()
    }

    @Test
    fun givenIncomingJpegMms_whenReceived_thenTypePreserved() {
        val message = InstrumentedMmsTestDataFactory.createImageMms(
            realm,
            imageType = "image/jpeg"
        )

        val imagePart = message.parts.first { it.type?.startsWith("image/") == true }
        assertThat(imagePart.type).isEqualTo("image/jpeg")
    }

    @Test
    fun givenIncomingPngMms_whenReceived_thenTypePreserved() {
        val message = InstrumentedMmsTestDataFactory.createImageMms(
            realm,
            imageType = "image/png"
        )

        val imagePart = message.parts.first { it.type?.startsWith("image/") == true }
        assertThat(imagePart.type).isEqualTo("image/png")
    }

    @Test
    fun givenIncomingGifMms_whenReceived_thenTypePreserved() {
        val message = InstrumentedMmsTestDataFactory.createImageMms(
            realm,
            imageType = "image/gif"
        )

        val imagePart = message.parts.first { it.type?.startsWith("image/") == true }
        assertThat(imagePart.type).isEqualTo("image/gif")
    }

    @Test
    fun givenIncomingImageWithCaption_whenReceived_thenBothPartsStored() {
        val message = InstrumentedMmsTestDataFactory.createImageMms(
            realm,
            text = "Check this out!"
        )

        assertThat(message.parts.size).isEqualTo(2)

        val hasTextPart = message.parts.any { it.type == "text/plain" }
        val hasImagePart = message.parts.any { it.type?.startsWith("image/") == true }

        assertThat(hasTextPart).isTrue()
        assertThat(hasImagePart).isTrue()
    }

    // ==================== Video Receive Tests ====================

    @Test
    fun givenIncomingVideoMms_whenReceived_thenHasVideoPart() {
        val message = InstrumentedMmsTestDataFactory.createVideoMms(realm)

        val hasVideoPart = message.parts.any { it.type?.startsWith("video/") == true }
        assertThat(hasVideoPart).isTrue()
    }

    @Test
    fun givenIncomingMp4Video_whenReceived_thenTypePreserved() {
        val message = InstrumentedMmsTestDataFactory.createVideoMms(
            realm,
            videoType = "video/mp4"
        )

        val videoPart = message.parts.first { it.type?.startsWith("video/") == true }
        assertThat(videoPart.type).isEqualTo("video/mp4")
    }

    @Test
    fun givenIncoming3gppVideo_whenReceived_thenTypePreserved() {
        val message = InstrumentedMmsTestDataFactory.createVideoMms(
            realm,
            videoType = "video/3gpp"
        )

        val videoPart = message.parts.first { it.type?.startsWith("video/") == true }
        assertThat(videoPart.type).isEqualTo("video/3gpp")
    }

    // ==================== Audio Receive Tests ====================

    @Test
    fun givenIncomingAudioMms_whenReceived_thenHasAudioPart() {
        val message = InstrumentedMmsTestDataFactory.createAudioMms(realm)

        val hasAudioPart = message.parts.any { it.type?.startsWith("audio/") == true }
        assertThat(hasAudioPart).isTrue()
    }

    @Test
    fun givenIncomingMp3Audio_whenReceived_thenTypePreserved() {
        val message = InstrumentedMmsTestDataFactory.createAudioMms(
            realm,
            audioType = "audio/mp3"
        )

        val audioPart = message.parts.first { it.type?.startsWith("audio/") == true }
        assertThat(audioPart.type).isEqualTo("audio/mp3")
    }

    @Test
    fun givenIncomingAacAudio_whenReceived_thenTypePreserved() {
        val message = InstrumentedMmsTestDataFactory.createAudioMms(
            realm,
            audioType = "audio/aac"
        )

        val audioPart = message.parts.first { it.type?.startsWith("audio/") == true }
        assertThat(audioPart.type).isEqualTo("audio/aac")
    }

    // ==================== Thread Association Tests ====================

    @Test
    fun givenIncomingMms_whenReceived_thenAssociatedWithThread() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            threadId = 1234L
        )

        assertThat(message.threadId).isEqualTo(1234L)
    }

    @Test
    fun givenMultipleIncomingMms_whenReceivedInSameThread_thenSameThreadId() {
        val message1 = InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            text = "First",
            threadId = 500L
        )
        val message2 = InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            text = "Second",
            threadId = 500L
        )

        assertThat(message1.threadId).isEqualTo(message2.threadId)

        val messagesInThread = realm.where(Message::class.java)
            .equalTo("threadId", 500L)
            .findAll()

        assertThat(messagesInThread.size).isEqualTo(2)
    }

    @Test
    fun givenIncomingMmsFromDifferentSenders_whenReceived_thenDifferentThreads() {
        val message1 = InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            address = "+15551111111",
            threadId = 100L
        )
        val message2 = InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            address = "+15552222222",
            threadId = 200L
        )

        assertThat(message1.threadId).isNotEqualTo(message2.threadId)
    }

    // ==================== Query Tests ====================

    @Test
    fun givenInboxMms_whenQueryByBoxId_thenFound() {
        InstrumentedMmsTestDataFactory.createTextMms(realm)
        InstrumentedMmsTestDataFactory.createTextMms(realm)
        InstrumentedMmsTestDataFactory.createOutgoingMms(realm)

        val inboxMessages = realm.where(Message::class.java)
            .equalTo("boxId", Mms.MESSAGE_BOX_INBOX)
            .findAll()

        assertThat(inboxMessages.size).isEqualTo(2)
    }

    @Test
    fun givenUnreadMms_whenQueryByRead_thenFound() {
        InstrumentedMmsTestDataFactory.createTextMms(realm)
        InstrumentedMmsTestDataFactory.createTextMms(realm)
        InstrumentedMmsTestDataFactory.createOutgoingMms(realm) // read=true

        val unreadMessages = realm.where(Message::class.java)
            .equalTo("read", false)
            .findAll()

        assertThat(unreadMessages.size).isEqualTo(2)
    }

    @Test
    fun givenMmsFromSender_whenQueryByAddress_thenFound() {
        InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            address = "+15559999999"
        )
        InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            address = "+15558888888"
        )

        val messagesFromSender = realm.where(Message::class.java)
            .equalTo("address", "+15559999999")
            .findAll()

        assertThat(messagesFromSender.size).isEqualTo(1)
    }

    // ==================== Mark as Read Tests ====================

    @Test
    fun givenUnreadMms_whenMarkAsRead_thenUpdated() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm)
        val messageId = message.id

        assertThat(message.read).isFalse()

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

    @Test
    fun givenMultipleUnreadMms_whenMarkAllAsRead_thenAllUpdated() {
        InstrumentedMmsTestDataFactory.createTextMms(realm, threadId = 777L)
        InstrumentedMmsTestDataFactory.createTextMms(realm, threadId = 777L)
        InstrumentedMmsTestDataFactory.createTextMms(realm, threadId = 777L)

        realm.executeTransaction { r ->
            val messages = r.where(Message::class.java)
                .equalTo("threadId", 777L)
                .findAll()
            messages.forEach { msg ->
                msg.read = true
                msg.seen = true
            }
        }

        val unreadInThread = realm.where(Message::class.java)
            .equalTo("threadId", 777L)
            .equalTo("read", false)
            .findAll()

        assertThat(unreadInThread.size).isEqualTo(0)
    }

    // ==================== Subject Tests ====================

    @Test
    fun givenIncomingMmsWithSubject_whenReceived_thenSubjectPreserved() {
        val message = InstrumentedMmsTestDataFactory.createMmsWithSubject(
            realm,
            subject = "Meeting Tomorrow"
        )

        assertThat(message.subject).isEqualTo("Meeting Tomorrow")
    }

    @Test
    fun givenIncomingMmsWithSubjectAndBody_whenReceived_thenBothPreserved() {
        val message = InstrumentedMmsTestDataFactory.createMmsWithSubject(
            realm,
            subject = "Photos from trip",
            text = "Here are the photos I mentioned"
        )

        assertThat(message.subject).isEqualTo("Photos from trip")
        assertThat(message.body).isEqualTo("Here are the photos I mentioned")
    }

    // ==================== Retrieval Tests ====================

    @Test
    fun givenStoredMms_whenRetrieveById_thenDataComplete() {
        val original = InstrumentedMmsTestDataFactory.createImageMms(
            realm,
            text = "Photo caption",
            imageType = "image/jpeg",
            address = "+15557654321",
            threadId = 999L
        )
        val messageId = original.id

        val retrieved = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()

        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.body).isEqualTo("Photo caption")
        assertThat(retrieved.address).isEqualTo("+15557654321")
        assertThat(retrieved.threadId).isEqualTo(999L)
        assertThat(retrieved.type).isEqualTo("mms")
        assertThat(retrieved.parts.size).isEqualTo(2)
    }

    @Test
    fun givenStoredMms_whenRetrieveByThread_thenOrderedByDate() {
        // Create messages with slight time differences
        val message1 = InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            text = "First",
            threadId = 888L
        )

        Thread.sleep(10) // Ensure different timestamps

        val message2 = InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            text = "Second",
            threadId = 888L
        )

        val messagesInThread = realm.where(Message::class.java)
            .equalTo("threadId", 888L)
            .sort("date")
            .findAll()

        assertThat(messagesInThread.size).isEqualTo(2)
        assertThat(messagesInThread[0]!!.body).isEqualTo("First")
        assertThat(messagesInThread[1]!!.body).isEqualTo("Second")
    }

    // ==================== Parts Retrieval Tests ====================

    @Test
    fun givenMultiPartMms_whenRetrieve_thenAllPartsAccessible() {
        val message = InstrumentedMmsTestDataFactory.createImageMms(
            realm,
            text = "Caption",
            imageType = "image/png"
        )
        val messageId = message.id

        val retrieved = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()

        assertThat(retrieved!!.parts.size).isEqualTo(2)

        val textPart = retrieved.parts.first { it.type == "text/plain" }
        val imagePart = retrieved.parts.first { it.type == "image/png" }

        assertThat(textPart.text).isEqualTo("Caption")
        assertThat(imagePart.name).isEqualTo("image.jpg")
    }

    // ==================== Deletion Tests ====================

    @Test
    fun givenReceivedMms_whenDelete_thenRemoved() {
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

    @Test
    fun givenThreadWithMultipleMms_whenDeleteOne_thenOthersRemain() {
        val message1 = InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            text = "Keep me",
            threadId = 666L
        )
        val message2 = InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            text = "Delete me",
            threadId = 666L
        )
        val message2Id = message2.id

        realm.executeTransaction { r ->
            val toDelete = r.where(Message::class.java)
                .equalTo("id", message2Id)
                .findFirst()
            toDelete?.deleteFromRealm()
        }

        val remaining = realm.where(Message::class.java)
            .equalTo("threadId", 666L)
            .findAll()

        assertThat(remaining.size).isEqualTo(1)
        assertThat(remaining[0]!!.body).isEqualTo("Keep me")
    }
}
