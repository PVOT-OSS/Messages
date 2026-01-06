/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 *
 * Instrumented tests for MMS retry and recovery functionality.
 */
package org.prauga.messages.mms

import android.content.Context
import android.provider.Telephony.Mms
import android.provider.Telephony.MmsSms
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.realm.Realm
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.messages.model.Message
import org.prauga.messages.testutil.InstrumentedMmsTestDataFactory
import org.prauga.messages.testutil.RealmTestHelper

/**
 * Instrumented tests for MMS retry and recovery scenarios.
 * Tests message state management during send failures and retries.
 */
@RunWith(AndroidJUnit4::class)
class MmsRetryRecoveryInstrumentedTest {

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

    // ==================== Failed Message State Tests ====================

    @Test
    fun givenFailedMms_whenQueryByBoxId_thenFound() {
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            boxId = Mms.MESSAGE_BOX_FAILED
        )

        val failedMessages = realm.where(Message::class.java)
            .equalTo("boxId", Mms.MESSAGE_BOX_FAILED)
            .findAll()

        assertThat(failedMessages.size).isEqualTo(1)
        assertThat(failedMessages[0]!!.id).isEqualTo(message.id)
    }

    @Test
    fun givenFailedMms_whenHasErrorType_thenIsFailedMessage() {
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            boxId = Mms.MESSAGE_BOX_FAILED
        )
        val messageId = message.id

        realm.executeTransaction { r ->
            val msg = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            msg?.errorType = MmsSms.ERR_TYPE_GENERIC_PERMANENT
        }

        val updatedMessage = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()

        assertThat(updatedMessage!!.isFailedMessage()).isTrue()
    }

    @Test
    fun givenMmsInOutbox_whenNotFailed_thenIsSending() {
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            boxId = Mms.MESSAGE_BOX_OUTBOX
        )

        assertThat(message.isSending()).isTrue()
        assertThat(message.isFailedMessage()).isFalse()
    }

    // ==================== Retry State Transition Tests ====================

    @Test
    fun givenFailedMms_whenMarkForRetry_thenMovesToOutbox() {
        // Create a failed message
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            boxId = Mms.MESSAGE_BOX_FAILED
        )
        val messageId = message.id

        realm.executeTransaction { r ->
            val msg = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            msg?.errorType = MmsSms.ERR_TYPE_GENERIC_PERMANENT
        }

        assertThat(message.isFailedMessage()).isTrue()

        // Mark for retry (move to OUTBOX)
        realm.executeTransaction { r ->
            val msg = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            msg?.boxId = Mms.MESSAGE_BOX_OUTBOX
            msg?.errorType = 0 // Clear error type
        }

        val retryingMessage = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()

        assertThat(retryingMessage!!.boxId).isEqualTo(Mms.MESSAGE_BOX_OUTBOX)
        assertThat(retryingMessage.errorType).isEqualTo(0)
        assertThat(retryingMessage.isSending()).isTrue()
    }

    @Test
    fun givenRetryingMms_whenSucceeds_thenMovesToSent() {
        // Create message in OUTBOX (retrying state)
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            boxId = Mms.MESSAGE_BOX_OUTBOX
        )
        val messageId = message.id

        // Simulate successful retry
        realm.executeTransaction { r ->
            val msg = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            msg?.boxId = Mms.MESSAGE_BOX_SENT
        }

        val sentMessage = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()

        assertThat(sentMessage!!.boxId).isEqualTo(Mms.MESSAGE_BOX_SENT)
        assertThat(sentMessage.isSending()).isFalse()
        assertThat(sentMessage.isFailedMessage()).isFalse()
    }

    @Test
    fun givenRetryingMms_whenFailsAgain_thenMovesToFailed() {
        // Create message in OUTBOX (retrying state)
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            boxId = Mms.MESSAGE_BOX_OUTBOX
        )
        val messageId = message.id

        // Simulate failed retry
        realm.executeTransaction { r ->
            val msg = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            msg?.boxId = Mms.MESSAGE_BOX_FAILED
            msg?.errorType = MmsSms.ERR_TYPE_GENERIC_PERMANENT
        }

        val failedMessage = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()

        assertThat(failedMessage!!.boxId).isEqualTo(Mms.MESSAGE_BOX_FAILED)
        assertThat(failedMessage.isFailedMessage()).isTrue()
    }

    // ==================== Multiple Retry Tests ====================

    @Test
    fun givenFailedMms_whenRetryMultipleTimes_thenStateTracked() {
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            boxId = Mms.MESSAGE_BOX_FAILED
        )
        val messageId = message.id

        // First retry attempt - move to OUTBOX
        realm.executeTransaction { r ->
            val msg = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            msg?.boxId = Mms.MESSAGE_BOX_OUTBOX
            msg?.errorType = 0
        }

        var currentMessage = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()
        assertThat(currentMessage!!.boxId).isEqualTo(Mms.MESSAGE_BOX_OUTBOX)

        // First retry fails - move to FAILED
        realm.executeTransaction { r ->
            val msg = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            msg?.boxId = Mms.MESSAGE_BOX_FAILED
            msg?.errorType = MmsSms.ERR_TYPE_GENERIC
        }

        currentMessage = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()
        assertThat(currentMessage!!.boxId).isEqualTo(Mms.MESSAGE_BOX_FAILED)

        // Second retry attempt - move to OUTBOX
        realm.executeTransaction { r ->
            val msg = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            msg?.boxId = Mms.MESSAGE_BOX_OUTBOX
            msg?.errorType = 0
        }

        currentMessage = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()
        assertThat(currentMessage!!.boxId).isEqualTo(Mms.MESSAGE_BOX_OUTBOX)

        // Second retry succeeds - move to SENT
        realm.executeTransaction { r ->
            val msg = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            msg?.boxId = Mms.MESSAGE_BOX_SENT
        }

        currentMessage = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()
        assertThat(currentMessage!!.boxId).isEqualTo(Mms.MESSAGE_BOX_SENT)
    }

    // ==================== Error Type Tests ====================

    @Test
    fun givenGenericError_whenQuery_thenFoundByErrorType() {
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            boxId = Mms.MESSAGE_BOX_FAILED
        )
        val messageId = message.id

        realm.executeTransaction { r ->
            val msg = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            msg?.errorType = MmsSms.ERR_TYPE_GENERIC
        }

        val genericErrors = realm.where(Message::class.java)
            .equalTo("errorType", MmsSms.ERR_TYPE_GENERIC)
            .findAll()

        assertThat(genericErrors).isNotEmpty()
    }

    @Test
    fun givenPermanentError_whenQuery_thenFoundByErrorType() {
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            boxId = Mms.MESSAGE_BOX_FAILED
        )
        val messageId = message.id

        realm.executeTransaction { r ->
            val msg = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            msg?.errorType = MmsSms.ERR_TYPE_GENERIC_PERMANENT
        }

        val permanentErrors = realm.where(Message::class.java)
            .equalTo("errorType", MmsSms.ERR_TYPE_GENERIC_PERMANENT)
            .findAll()

        assertThat(permanentErrors).isNotEmpty()
    }

    @Test
    fun givenMmsNetworkError_whenQuery_thenFoundByErrorType() {
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            boxId = Mms.MESSAGE_BOX_FAILED
        )
        val messageId = message.id

        realm.executeTransaction { r ->
            val msg = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            msg?.errorType = MmsSms.ERR_TYPE_MMS_PROTO_TRANSIENT
        }

        val networkErrors = realm.where(Message::class.java)
            .equalTo("errorType", MmsSms.ERR_TYPE_MMS_PROTO_TRANSIENT)
            .findAll()

        assertThat(networkErrors).isNotEmpty()
    }

    // ==================== Parts Preservation Tests ====================

    @Test
    fun givenFailedMmsWithParts_whenRetry_thenPartsPreserved() {
        // Create image MMS that failed
        val message = InstrumentedMmsTestDataFactory.createImageMms(
            realm,
            text = "Check this photo!"
        )
        val messageId = message.id

        // Mark as failed
        realm.executeTransaction { r ->
            val msg = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            msg?.boxId = Mms.MESSAGE_BOX_FAILED
            msg?.errorType = MmsSms.ERR_TYPE_GENERIC
        }

        val failedMessage = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()

        // Parts should still be there
        assertThat(failedMessage!!.parts.size).isEqualTo(2) // text + image
    }

    @Test
    fun givenFailedMmsWithParts_whenRetrySucceeds_thenPartsStillThere() {
        val message = InstrumentedMmsTestDataFactory.createImageMms(
            realm,
            text = "Photo attachment"
        )
        val messageId = message.id
        val originalPartCount = message.parts.size

        // Mark as failed
        realm.executeTransaction { r ->
            val msg = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            msg?.boxId = Mms.MESSAGE_BOX_FAILED
        }

        // Mark as retrying
        realm.executeTransaction { r ->
            val msg = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            msg?.boxId = Mms.MESSAGE_BOX_OUTBOX
        }

        // Mark as sent
        realm.executeTransaction { r ->
            val msg = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            msg?.boxId = Mms.MESSAGE_BOX_SENT
        }

        val sentMessage = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()

        assertThat(sentMessage!!.parts.size).isEqualTo(originalPartCount)
    }

    // ==================== Query Failed Messages Tests ====================

    @Test
    fun givenMultipleFailedMms_whenQuery_thenAllFound() {
        // Create 3 failed messages
        repeat(3) {
            val msg = InstrumentedMmsTestDataFactory.createOutgoingMms(realm)
            realm.executeTransaction { r ->
                val m = r.where(Message::class.java)
                    .equalTo("id", msg.id)
                    .findFirst()
                m?.boxId = Mms.MESSAGE_BOX_FAILED
                m?.errorType = MmsSms.ERR_TYPE_GENERIC
            }
        }

        val failedMessages = realm.where(Message::class.java)
            .equalTo("boxId", Mms.MESSAGE_BOX_FAILED)
            .findAll()

        assertThat(failedMessages.size).isEqualTo(3)
    }

    @Test
    fun givenMixedStatusMms_whenQueryFailed_thenOnlyFailedReturned() {
        // Create sent message
        InstrumentedMmsTestDataFactory.createOutgoingMms(realm, boxId = Mms.MESSAGE_BOX_SENT)

        // Create outbox message
        InstrumentedMmsTestDataFactory.createOutgoingMms(realm, boxId = Mms.MESSAGE_BOX_OUTBOX)

        // Create failed message
        val failedMsg = InstrumentedMmsTestDataFactory.createOutgoingMms(realm)
        realm.executeTransaction { r ->
            val m = r.where(Message::class.java)
                .equalTo("id", failedMsg.id)
                .findFirst()
            m?.boxId = Mms.MESSAGE_BOX_FAILED
        }

        val failedMessages = realm.where(Message::class.java)
            .equalTo("boxId", Mms.MESSAGE_BOX_FAILED)
            .findAll()

        assertThat(failedMessages.size).isEqualTo(1)
    }

    // ==================== Thread-based Recovery Tests ====================

    @Test
    fun givenFailedMmsInThread_whenQueryByThread_thenFound() {
        val threadId = 9999L

        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(realm)
        realm.executeTransaction { r ->
            val m = r.where(Message::class.java)
                .equalTo("id", message.id)
                .findFirst()
            m?.threadId = threadId
            m?.boxId = Mms.MESSAGE_BOX_FAILED
        }

        val failedInThread = realm.where(Message::class.java)
            .equalTo("threadId", threadId)
            .equalTo("boxId", Mms.MESSAGE_BOX_FAILED)
            .findAll()

        assertThat(failedInThread.size).isEqualTo(1)
    }

    // ==================== Delete Failed Message Tests ====================

    @Test
    fun givenFailedMms_whenDelete_thenRemoved() {
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            boxId = Mms.MESSAGE_BOX_FAILED
        )
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

    // ==================== isFailedMessage() Logic Tests ====================

    @Test
    fun givenMmsWithHighErrorType_thenIsFailedMessage() {
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(realm)
        val messageId = message.id

        realm.executeTransaction { r ->
            val m = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            m?.errorType = MmsSms.ERR_TYPE_GENERIC_PERMANENT
        }

        val updatedMessage = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()

        // errorType >= ERR_TYPE_GENERIC_PERMANENT means failed
        assertThat(updatedMessage!!.isFailedMessage()).isTrue()
    }

    @Test
    fun givenMmsInFailedBox_thenIsFailedMessage() {
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            boxId = Mms.MESSAGE_BOX_FAILED
        )

        // boxId == MESSAGE_BOX_FAILED means failed
        assertThat(message.isFailedMessage()).isTrue()
    }

    @Test
    fun givenMmsInSentBox_thenNotFailedMessage() {
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            boxId = Mms.MESSAGE_BOX_SENT
        )

        assertThat(message.isFailedMessage()).isFalse()
    }

    // ==================== Clear Error on Retry Tests ====================

    @Test
    fun givenFailedMmsWithErrorType_whenPrepareRetry_thenErrorCleared() {
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            boxId = Mms.MESSAGE_BOX_FAILED
        )
        val messageId = message.id

        realm.executeTransaction { r ->
            val m = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            m?.errorType = MmsSms.ERR_TYPE_GENERIC_PERMANENT
        }

        // Verify error is set
        var currentMessage = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()
        assertThat(currentMessage!!.errorType).isGreaterThan(0)

        // Prepare for retry - clear error
        realm.executeTransaction { r ->
            val m = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            m?.boxId = Mms.MESSAGE_BOX_OUTBOX
            m?.errorType = 0
        }

        currentMessage = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()
        assertThat(currentMessage!!.errorType).isEqualTo(0)
        assertThat(currentMessage.boxId).isEqualTo(Mms.MESSAGE_BOX_OUTBOX)
    }
}
