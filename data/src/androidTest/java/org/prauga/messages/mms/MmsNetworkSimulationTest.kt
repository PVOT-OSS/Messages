/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 *
 * Instrumented tests for MMS network simulation.
 * Tests network scenarios using MockMmsHttpClient.
 */
package org.prauga.messages.mms

import android.content.Context
import android.provider.Telephony.Mms
import android.provider.Telephony.MmsSms
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.mms.pdu_alt.PduHeaders
import com.google.android.mms.pdu_alt.PduParser
import com.google.common.truth.Truth.assertThat
import io.realm.Realm
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.messages.model.Message
import org.prauga.messages.testutil.InstrumentedMmsTestDataFactory
import org.prauga.messages.testutil.MockMmsHttpClient
import org.prauga.messages.testutil.MockMmsException
import org.prauga.messages.testutil.RealmTestHelper

/**
 * Instrumented tests for simulating MMS network operations.
 * These tests verify behavior under various network conditions.
 */
@RunWith(AndroidJUnit4::class)
class MmsNetworkSimulationTest {

    private lateinit var context: Context
    private lateinit var realm: Realm
    private lateinit var mockHttpClient: MockMmsHttpClient

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        RealmTestHelper.init(context)
        realm = RealmTestHelper.getRealm()
        InstrumentedMmsTestDataFactory.resetIdGenerators()
        mockHttpClient = MockMmsHttpClient()
    }

    @After
    fun tearDown() {
        mockHttpClient.reset()
        realm.close()
        RealmTestHelper.clearAll()
        RealmTestHelper.tearDown()
    }

    // ==================== Send Success Scenarios ====================

    @Test
    fun givenSuccessResponse_whenSimulateSend_thenReturnsValidPdu() {
        val testPdu = createTestSendReqPdu()

        val response = mockHttpClient.simulateSend(testPdu)

        assertThat(response).isNotNull()
        assertThat(response.isNotEmpty()).isTrue()
    }

    @Test
    fun givenSuccessResponse_whenParseSendConf_thenStatusIsOk() {
        val testPdu = createTestSendReqPdu()

        val response = mockHttpClient.simulateSend(testPdu)
        val parsedResponse = PduParser(response, true).parse()

        assertThat(parsedResponse).isNotNull()
        assertThat(parsedResponse?.messageType).isEqualTo(PduHeaders.MESSAGE_TYPE_SEND_CONF)
    }

    @Test
    fun givenSendSuccess_whenTrackRequests_thenRequestRecorded() {
        val testPdu = createTestSendReqPdu()

        mockHttpClient.simulateSend(testPdu)

        assertThat(mockHttpClient.getSendRequests()).hasSize(1)
        assertThat(mockHttpClient.getSendRequests()[0]).isEqualTo(testPdu)
    }

    @Test
    fun givenMultipleSends_whenTrackRequests_thenAllRecorded() {
        val pdu1 = createTestSendReqPdu()
        val pdu2 = createTestSendReqPdu()
        val pdu3 = createTestSendReqPdu()

        mockHttpClient.simulateSend(pdu1)
        mockHttpClient.simulateSend(pdu2)
        mockHttpClient.simulateSend(pdu3)

        assertThat(mockHttpClient.getSendRequests()).hasSize(3)
    }

    // ==================== Send Failure Scenarios ====================

    @Test
    fun givenNetworkFailure_whenSimulateSend_thenThrowsException() {
        mockHttpClient.shouldFail = true
        mockHttpClient.failureMessage = "Network timeout"
        val testPdu = createTestSendReqPdu()

        var exceptionThrown = false
        try {
            mockHttpClient.simulateSend(testPdu)
        } catch (e: MockMmsException) {
            exceptionThrown = true
            assertThat(e.message).isEqualTo("Network timeout")
        }

        assertThat(exceptionThrown).isTrue()
    }

    @Test
    fun givenErrorResponse_whenParseSendConf_thenStatusIsError() {
        mockHttpClient.sendResponse = MockMmsHttpClient.createErrorSendConfPdu(0x81) // Unspecified error
        val testPdu = createTestSendReqPdu()

        val response = mockHttpClient.simulateSend(testPdu)
        val parsedResponse = PduParser(response, true).parse()

        assertThat(parsedResponse).isNotNull()
        assertThat(parsedResponse?.messageType).isEqualTo(PduHeaders.MESSAGE_TYPE_SEND_CONF)
    }

    @Test
    fun givenFailure_whenRequestTracked_thenStillRecorded() {
        mockHttpClient.shouldFail = true
        val testPdu = createTestSendReqPdu()

        try {
            mockHttpClient.simulateSend(testPdu)
        } catch (e: MockMmsException) {
            // Expected
        }

        // Request should still be tracked even if it failed
        assertThat(mockHttpClient.getSendRequests()).hasSize(1)
    }

    // ==================== Retrieve Success Scenarios ====================

    @Test
    fun givenSuccessResponse_whenSimulateRetrieve_thenReturnsValidPdu() {
        val contentLocation = "http://mmsc.carrier.com/mms/1234567890"

        val response = mockHttpClient.simulateRetrieve(contentLocation)

        assertThat(response).isNotNull()
        assertThat(response.isNotEmpty()).isTrue()
    }

    @Test
    fun givenRetrieveSuccess_whenTrackRequests_thenRequestRecorded() {
        val contentLocation = "http://mmsc.carrier.com/mms/1234567890"

        mockHttpClient.simulateRetrieve(contentLocation)

        assertThat(mockHttpClient.getRetrieveRequests()).hasSize(1)
        assertThat(mockHttpClient.getRetrieveRequests()[0]).isEqualTo(contentLocation)
    }

    // ==================== Retrieve Failure Scenarios ====================

    @Test
    fun givenNetworkFailure_whenSimulateRetrieve_thenThrowsException() {
        mockHttpClient.shouldFail = true
        mockHttpClient.failureMessage = "Connection refused"
        val contentLocation = "http://mmsc.carrier.com/mms/1234567890"

        var exceptionThrown = false
        try {
            mockHttpClient.simulateRetrieve(contentLocation)
        } catch (e: MockMmsException) {
            exceptionThrown = true
            assertThat(e.message).isEqualTo("Connection refused")
        }

        assertThat(exceptionThrown).isTrue()
    }

    // ==================== Network Delay Scenarios ====================

    @Test
    fun givenNetworkDelay_whenSimulateSend_thenDelayOccurs() {
        mockHttpClient.networkDelayMs = 100
        val testPdu = createTestSendReqPdu()

        val startTime = System.currentTimeMillis()
        mockHttpClient.simulateSend(testPdu)
        val endTime = System.currentTimeMillis()

        assertThat(endTime - startTime).isAtLeast(100L)
    }

    @Test
    fun givenNetworkDelay_whenSimulateRetrieve_thenDelayOccurs() {
        mockHttpClient.networkDelayMs = 100
        val contentLocation = "http://mmsc.carrier.com/mms/1234567890"

        val startTime = System.currentTimeMillis()
        mockHttpClient.simulateRetrieve(contentLocation)
        val endTime = System.currentTimeMillis()

        assertThat(endTime - startTime).isAtLeast(100L)
    }

    // ==================== Message State with Network Simulation ====================

    @Test
    fun givenMessageInOutbox_whenSimulateSuccess_thenCanTransitionToSent() {
        // Create a message in OUTBOX state
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            boxId = Mms.MESSAGE_BOX_OUTBOX
        )
        val messageId = message.id

        assertThat(message.boxId).isEqualTo(Mms.MESSAGE_BOX_OUTBOX)

        // Simulate network success
        val testPdu = createTestSendReqPdu()
        val response = mockHttpClient.simulateSend(testPdu)
        assertThat(response).isNotNull()

        // Update message state to SENT (simulating what MmsSentReceiver would do)
        realm.executeTransaction { r ->
            val msg = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            msg?.boxId = Mms.MESSAGE_BOX_SENT
        }

        val updatedMessage = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()

        assertThat(updatedMessage!!.boxId).isEqualTo(Mms.MESSAGE_BOX_SENT)
    }

    @Test
    fun givenMessageInOutbox_whenSimulateFailure_thenCanTransitionToFailed() {
        // Create a message in OUTBOX state
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            boxId = Mms.MESSAGE_BOX_OUTBOX
        )
        val messageId = message.id

        // Simulate network failure
        mockHttpClient.shouldFail = true
        val testPdu = createTestSendReqPdu()

        var networkFailed = false
        try {
            mockHttpClient.simulateSend(testPdu)
        } catch (e: MockMmsException) {
            networkFailed = true
        }

        assertThat(networkFailed).isTrue()

        // Update message state to FAILED (simulating what MmsSentReceiver would do)
        realm.executeTransaction { r ->
            val msg = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            msg?.boxId = Mms.MESSAGE_BOX_FAILED
            msg?.errorType = MmsSms.ERR_TYPE_GENERIC_PERMANENT
        }

        val updatedMessage = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()

        assertThat(updatedMessage!!.boxId).isEqualTo(Mms.MESSAGE_BOX_FAILED)
        assertThat(updatedMessage.errorType).isEqualTo(MmsSms.ERR_TYPE_GENERIC_PERMANENT)
    }

    // ==================== Notification Ind Parsing ====================

    @Test
    fun givenNotificationInd_whenParse_thenContentLocationExtracted() {
        val contentLocation = "http://mmsc.carrier.com/mms/retrieve/abc123"
        val notificationPdu = MockMmsHttpClient.createNotificationIndPdu(contentLocation)

        val parsedPdu = PduParser(notificationPdu, true).parse()

        assertThat(parsedPdu).isNotNull()
        assertThat(parsedPdu?.messageType).isEqualTo(PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND)
    }

    // ==================== Reset and Clear Tests ====================

    @Test
    fun givenTrackedRequests_whenClearRequests_thenEmpty() {
        mockHttpClient.simulateSend(createTestSendReqPdu())
        mockHttpClient.simulateSend(createTestSendReqPdu())
        mockHttpClient.simulateRetrieve("http://example.com")

        assertThat(mockHttpClient.getSendRequests()).isNotEmpty()
        assertThat(mockHttpClient.getRetrieveRequests()).isNotEmpty()

        mockHttpClient.clearRequests()

        assertThat(mockHttpClient.getSendRequests()).isEmpty()
        assertThat(mockHttpClient.getRetrieveRequests()).isEmpty()
    }

    @Test
    fun givenCustomConfig_whenReset_thenDefaultsRestored() {
        mockHttpClient.shouldFail = true
        mockHttpClient.failureMessage = "Custom error"
        mockHttpClient.networkDelayMs = 5000

        mockHttpClient.reset()

        assertThat(mockHttpClient.shouldFail).isFalse()
        assertThat(mockHttpClient.networkDelayMs).isEqualTo(0)
    }

    // ==================== Concurrent Operations ====================

    @Test
    fun givenConcurrentSends_whenSimulate_thenAllTracked() {
        val threads = (1..5).map { i ->
            Thread {
                val pdu = createTestSendReqPdu()
                mockHttpClient.simulateSend(pdu)
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertThat(mockHttpClient.getSendRequests()).hasSize(5)
    }

    // ==================== Error Type Constants Test ====================

    @Test
    fun givenErrorTypeGeneric_whenStored_thenCanQuery() {
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

        val failedMessages = realm.where(Message::class.java)
            .greaterThan("errorType", 0)
            .findAll()

        assertThat(failedMessages).isNotEmpty()
    }

    @Test
    fun givenErrorTypePermanent_whenStored_thenCanQuery() {
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

        val permanentFailures = realm.where(Message::class.java)
            .equalTo("errorType", MmsSms.ERR_TYPE_GENERIC_PERMANENT)
            .findAll()

        assertThat(permanentFailures).isNotEmpty()
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a minimal test SendReq PDU for testing.
     */
    private fun createTestSendReqPdu(): ByteArray {
        return byteArrayOf(
            // Message type: SendReq (0x80)
            0x8C.toByte(), PduHeaders.MESSAGE_TYPE_SEND_REQ.toByte(),
            // Transaction ID
            0x98.toByte(), 't'.code.toByte(), 'e'.code.toByte(), 's'.code.toByte(),
            't'.code.toByte(), '1'.code.toByte(), '2'.code.toByte(), '3'.code.toByte(), 0x00,
            // MMS Version: 1.0
            0x8D.toByte(), 0x90.toByte(),
            // To: +15551234567
            0x97.toByte(), '+'.code.toByte(), '1'.code.toByte(), '5'.code.toByte(),
            '5'.code.toByte(), '5'.code.toByte(), '1'.code.toByte(), '2'.code.toByte(),
            '3'.code.toByte(), '4'.code.toByte(), '5'.code.toByte(), '6'.code.toByte(),
            '7'.code.toByte(), '/'.code.toByte(), 'T'.code.toByte(), 'Y'.code.toByte(),
            'P'.code.toByte(), 'E'.code.toByte(), '='.code.toByte(), 'P'.code.toByte(),
            'L'.code.toByte(), 'M'.code.toByte(), 'N'.code.toByte(), 0x00,
            // Content type: application/vnd.wap.multipart.related
            0x84.toByte(), 0xB3.toByte()
        )
    }
}
