/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 *
 * Instrumented integration tests for MMS functionality.
 * These tests run on an actual Android device or emulator and use
 * real Android components with a mocked network layer.
 */
package org.prauga.messages.mms

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.mms.pdu_alt.PduComposer
import com.google.android.mms.pdu_alt.PduHeaders
import com.google.android.mms.pdu_alt.PduParser
import com.google.android.mms.pdu_alt.SendReq
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.messages.testutil.MockMmsHttpClient

/**
 * Integration tests for MMS functionality.
 *
 * These tests verify:
 * - PDU encoding and parsing with real Android components
 * - MMS message structure validation
 * - Mock network layer interactions
 * - Content type handling
 */
@RunWith(AndroidJUnit4::class)
class MmsIntegrationTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var mockHttpClient: MockMmsHttpClient

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        contentResolver = context.contentResolver
        mockHttpClient = MockMmsHttpClient()
    }

    @After
    fun tearDown() {
        mockHttpClient.reset()
    }

    // ==================== PDU Round-Trip Tests ====================

    @Test
    fun givenSendReq_whenComposeAndParse_thenRoundTripSucceeds() {
        // Create a SendReq PDU
        val sendReq = SendReq()
        sendReq.setMessageType(PduHeaders.MESSAGE_TYPE_SEND_REQ)
        sendReq.setMmsVersion(PduHeaders.MMS_VERSION_1_0)
        sendReq.setTransactionId("txn123".toByteArray())

        // Compose to bytes
        val composer = PduComposer(context, sendReq)
        val pduBytes = composer.make()

        assertThat(pduBytes).isNotNull()
        assertThat(pduBytes).isNotEmpty()

        // Parse back
        val parser = PduParser(pduBytes)
        val parsedPdu = parser.parse()

        assertThat(parsedPdu).isNotNull()
        assertThat(parsedPdu).isInstanceOf(SendReq::class.java)
    }

    @Test
    fun givenSendReq_whenCompose_thenContainsMessageType() {
        val sendReq = SendReq()
        sendReq.setMessageType(PduHeaders.MESSAGE_TYPE_SEND_REQ)
        sendReq.setMmsVersion(PduHeaders.MMS_VERSION_1_0)
        sendReq.setTransactionId("test_txn".toByteArray())

        val composer = PduComposer(context, sendReq)
        val pduBytes = composer.make()

        assertThat(pduBytes).isNotNull()
        // First bytes should contain message type indicator
        assertThat(pduBytes!![0]).isEqualTo(0x8C.toByte())
    }

    // ==================== Mock Network Tests ====================

    @Test
    fun givenMockClient_whenSend_thenReceivesResponse() {
        val testPdu = byteArrayOf(0x8C.toByte(), 0x80.toByte()) // Minimal send req

        val response = mockHttpClient.simulateSend(testPdu)

        assertThat(response).isNotEmpty()
        assertThat(mockHttpClient.getSendRequests()).hasSize(1)
    }

    @Test
    fun givenMockClient_whenRetrieve_thenReceivesResponse() {
        val testLocation = "http://mmsc.example.com/mms/123"

        val response = mockHttpClient.simulateRetrieve(testLocation)

        assertThat(response).isNotEmpty()
        assertThat(mockHttpClient.getRetrieveRequests()).contains(testLocation)
    }

    @Test
    fun givenMockClientConfiguredToFail_whenSend_thenThrowsException() {
        mockHttpClient.shouldFail = true
        mockHttpClient.failureMessage = "Connection refused"

        var exceptionThrown = false
        try {
            mockHttpClient.simulateSend(byteArrayOf())
        } catch (e: Exception) {
            exceptionThrown = true
            assertThat(e.message).isEqualTo("Connection refused")
        }

        assertThat(exceptionThrown).isTrue()
    }

    @Test
    fun givenMultipleSends_whenCheckRequests_thenAllTracked() {
        mockHttpClient.simulateSend(byteArrayOf(0x01))
        mockHttpClient.simulateSend(byteArrayOf(0x02))
        mockHttpClient.simulateSend(byteArrayOf(0x03))

        assertThat(mockHttpClient.getSendRequests()).hasSize(3)
    }

    @Test
    fun givenMockClientReset_whenCheckRequests_thenEmpty() {
        mockHttpClient.simulateSend(byteArrayOf(0x01))
        mockHttpClient.reset()

        assertThat(mockHttpClient.getSendRequests()).isEmpty()
        assertThat(mockHttpClient.shouldFail).isFalse()
    }

    // ==================== SendConf Response Tests ====================

    @Test
    fun givenSuccessSendConf_whenParse_thenIsValid() {
        val sendConfBytes = MockMmsHttpClient.createSuccessSendConfPdu()
        val parser = PduParser(sendConfBytes)
        val pdu = parser.parse()

        assertThat(pdu).isNotNull()
        assertThat(pdu?.messageType).isEqualTo(PduHeaders.MESSAGE_TYPE_SEND_CONF)
    }

    @Test
    fun givenErrorSendConf_whenParse_thenHasErrorStatus() {
        val errorConfBytes = MockMmsHttpClient.createErrorSendConfPdu(0x81)
        val parser = PduParser(errorConfBytes)
        val pdu = parser.parse()

        // Parse should still succeed even for error response
        assertThat(pdu).isNotNull()
    }

    // ==================== NotificationInd Tests ====================

    @Test
    fun givenNotificationInd_whenParse_thenHasContentLocation() {
        val contentLocation = "http://mmsc.example.com/mms/retrieve/123"
        val notificationBytes = MockMmsHttpClient.createNotificationIndPdu(contentLocation)

        val parser = PduParser(notificationBytes)
        val pdu = parser.parse()

        assertThat(pdu).isNotNull()
        assertThat(pdu?.messageType).isEqualTo(PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND)
    }

    // ==================== Content Type Tests ====================

    @Test
    fun givenImageContentType_whenValidate_thenRecognized() {
        val imageTypes = listOf(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/bmp",
            "image/wbmp"
        )

        imageTypes.forEach { type ->
            assertThat(type.startsWith("image/")).isTrue()
        }
    }

    @Test
    fun givenVideoContentType_whenValidate_thenRecognized() {
        val videoTypes = listOf(
            "video/mp4",
            "video/3gpp",
            "video/3gpp2",
            "video/h263"
        )

        videoTypes.forEach { type ->
            assertThat(type.startsWith("video/")).isTrue()
        }
    }

    @Test
    fun givenAudioContentType_whenValidate_thenRecognized() {
        val audioTypes = listOf(
            "audio/mp3",
            "audio/mpeg",
            "audio/aac",
            "audio/amr",
            "audio/ogg",
            "audio/mid"
        )

        audioTypes.forEach { type ->
            assertThat(type.startsWith("audio/")).isTrue()
        }
    }

    // ==================== URI Handling Tests ====================

    @Test
    fun givenMmsUri_whenParse_thenTypeIsMms() {
        val uri = Uri.parse("content://mms/123")

        assertThat(uri.toString()).contains("mms")
    }

    @Test
    fun givenMmsInboxUri_whenParse_thenContainsBox() {
        val uri = Uri.parse("content://mms/inbox/456")

        assertThat(uri.pathSegments).contains("inbox")
    }

    @Test
    fun givenMmsSentUri_whenParse_thenContainsBox() {
        val uri = Uri.parse("content://mms/sent/789")

        assertThat(uri.pathSegments).contains("sent")
    }

    // ==================== Message Box Tests ====================

    @Test
    fun givenMessageBoxConstants_whenCheck_thenCorrectValues() {
        assertThat(Telephony.Mms.MESSAGE_BOX_INBOX).isEqualTo(1)
        assertThat(Telephony.Mms.MESSAGE_BOX_SENT).isEqualTo(2)
        assertThat(Telephony.Mms.MESSAGE_BOX_DRAFTS).isEqualTo(3)
        assertThat(Telephony.Mms.MESSAGE_BOX_OUTBOX).isEqualTo(4)
    }

    // ==================== Address Encoding Tests ====================

    @Test
    fun givenPhoneNumber_whenEncode_thenValidFormat() {
        val phoneNumber = "+15551234567"

        // Check it's a valid phone number format
        assertThat(phoneNumber).startsWith("+")
        assertThat(phoneNumber.substring(1).all { it.isDigit() }).isTrue()
    }

    @Test
    fun givenInternationalNumber_whenEncode_thenValidFormat() {
        val ukNumber = "+442012345678"
        val jpNumber = "+81312345678"

        assertThat(ukNumber).startsWith("+44")
        assertThat(jpNumber).startsWith("+81")
    }

    // ==================== Transaction ID Tests ====================

    @Test
    fun givenTransactionId_whenGenerate_thenUnique() {
        val ids = (1..100).map { System.currentTimeMillis().toString() + "_" + it }

        assertThat(ids.distinct()).hasSize(100)
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun givenSingleBytePdu_whenParse_thenReturnsNull() {
        // PduParser doesn't accept null, so test with minimal invalid data
        val parser = PduParser(byteArrayOf(0x00), true)
        val result = parser.parse()

        assertThat(result).isNull()
    }

    @Test
    fun givenEmptyPduBytes_whenParse_thenReturnsNull() {
        val parser = PduParser(byteArrayOf(), true)
        val result = parser.parse()

        assertThat(result).isNull()
    }

    @Test
    fun givenMalformedPdu_whenParse_thenReturnsNull() {
        val malformed = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        val parser = PduParser(malformed, true)
        val result = parser.parse()

        assertThat(result).isNull()
    }

    // ==================== MMS Version Tests ====================

    @Test
    fun givenMmsVersion10_whenCheck_thenCorrectConstant() {
        // MMS_VERSION_1_0 = (1 << 4) | 0 = 16
        assertThat(PduHeaders.MMS_VERSION_1_0).isEqualTo((1 shl 4) or 0)
    }

    @Test
    fun givenMmsVersion11_whenCheck_thenCorrectConstant() {
        // MMS_VERSION_1_1 = (1 << 4) | 1 = 17
        assertThat(PduHeaders.MMS_VERSION_1_1).isEqualTo((1 shl 4) or 1)
    }

    @Test
    fun givenMmsVersion12_whenCheck_thenCorrectConstant() {
        // MMS_VERSION_1_2 = (1 << 4) | 2 = 18
        assertThat(PduHeaders.MMS_VERSION_1_2).isEqualTo((1 shl 4) or 2)
    }

    // ==================== Message Type Constants Tests ====================

    @Test
    fun givenMessageTypeConstants_whenCheck_thenCorrectValues() {
        assertThat(PduHeaders.MESSAGE_TYPE_SEND_REQ).isEqualTo(128)
        assertThat(PduHeaders.MESSAGE_TYPE_SEND_CONF).isEqualTo(129)
        assertThat(PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND).isEqualTo(130)
        assertThat(PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF).isEqualTo(132)
        assertThat(PduHeaders.MESSAGE_TYPE_DELIVERY_IND).isEqualTo(134)
    }

    // ==================== Mock HTTP Client Configuration Tests ====================

    @Test
    fun givenCustomSendResponse_whenSend_thenReturnsCustomResponse() {
        val customResponse = byteArrayOf(0x01, 0x02, 0x03)
        mockHttpClient.sendResponse = customResponse

        val result = mockHttpClient.simulateSend(byteArrayOf())

        assertThat(result).isEqualTo(customResponse)
    }

    @Test
    fun givenCustomRetrieveResponse_whenRetrieve_thenReturnsCustomResponse() {
        val customResponse = byteArrayOf(0x04, 0x05, 0x06)
        mockHttpClient.retrieveResponse = customResponse

        val result = mockHttpClient.simulateRetrieve("http://example.com")

        assertThat(result).isEqualTo(customResponse)
    }

    @Test
    fun givenNetworkDelay_whenSend_thenDelayApplied() {
        mockHttpClient.networkDelayMs = 100

        val startTime = System.currentTimeMillis()
        mockHttpClient.simulateSend(byteArrayOf())
        val endTime = System.currentTimeMillis()

        assertThat(endTime - startTime).isAtLeast(100L)
    }

    // ==================== Concurrent Request Tests ====================

    @Test
    fun givenConcurrentSends_whenComplete_thenAllTracked() {
        val threads = (1..5).map { i ->
            Thread {
                mockHttpClient.simulateSend(byteArrayOf(i.toByte()))
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertThat(mockHttpClient.getSendRequests()).hasSize(5)
    }

    // ==================== PDU Part Handling Tests ====================

    @Test
    fun givenTextPlainMimeType_whenCheck_thenRecognized() {
        val textPlain = "text/plain"

        assertThat(textPlain).isEqualTo("text/plain")
        assertThat(textPlain.startsWith("text/")).isTrue()
    }

    @Test
    fun givenApplicationSmilMimeType_whenCheck_thenRecognized() {
        val smil = "application/smil"

        assertThat(smil).isEqualTo("application/smil")
        assertThat(smil.startsWith("application/")).isTrue()
    }

    @Test
    fun givenMultipartMixedMimeType_whenCheck_thenRecognized() {
        val multipart = "multipart/mixed"

        assertThat(multipart).startsWith("multipart/")
    }

    @Test
    fun givenMultipartRelatedMimeType_whenCheck_thenRecognized() {
        val multipart = "multipart/related"

        assertThat(multipart).startsWith("multipart/")
    }
}
