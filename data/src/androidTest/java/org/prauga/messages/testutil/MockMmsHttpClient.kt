/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 *
 * Mock HTTP client for MMS testing.
 * Simulates MMS network operations without actual network calls.
 */
package org.prauga.messages.testutil

import com.google.android.mms.pdu_alt.PduHeaders
import com.google.android.mms.pdu_alt.SendConf

/**
 * Mock HTTP client for simulating MMS network operations in tests.
 *
 * This class provides:
 * - Configurable send responses
 * - Configurable retrieve responses
 * - Failure simulation
 * - Request tracking for verification
 */
class MockMmsHttpClient {

    // Configuration
    var sendResponse: ByteArray = createSuccessSendConfPdu()
    var retrieveResponse: ByteArray = createSuccessRetrieveConfPdu()
    var shouldFail: Boolean = false
    var failureMessage: String = "Network error"
    var networkDelayMs: Long = 0

    // Request tracking
    private val sendRequests = mutableListOf<ByteArray>()
    private val retrieveRequests = mutableListOf<String>()

    /**
     * Simulates sending an MMS PDU.
     * @param pduData The PDU data to "send"
     * @return Response PDU (typically SendConf) or throws if configured to fail
     */
    fun simulateSend(pduData: ByteArray): ByteArray {
        sendRequests.add(pduData)

        if (networkDelayMs > 0) {
            Thread.sleep(networkDelayMs)
        }

        if (shouldFail) {
            throw MockMmsException(failureMessage)
        }

        return sendResponse
    }

    /**
     * Simulates retrieving an MMS from a content location.
     * @param contentLocation The URL to "retrieve" from
     * @return Response PDU (typically RetrieveConf) or throws if configured to fail
     */
    fun simulateRetrieve(contentLocation: String): ByteArray {
        retrieveRequests.add(contentLocation)

        if (networkDelayMs > 0) {
            Thread.sleep(networkDelayMs)
        }

        if (shouldFail) {
            throw MockMmsException(failureMessage)
        }

        return retrieveResponse
    }

    /**
     * Gets all send requests made to this mock.
     */
    fun getSendRequests(): List<ByteArray> = sendRequests.toList()

    /**
     * Gets all retrieve requests made to this mock.
     */
    fun getRetrieveRequests(): List<String> = retrieveRequests.toList()

    /**
     * Clears all tracked requests.
     */
    fun clearRequests() {
        sendRequests.clear()
        retrieveRequests.clear()
    }

    /**
     * Resets all configuration to defaults.
     */
    fun reset() {
        sendResponse = createSuccessSendConfPdu()
        retrieveResponse = createSuccessRetrieveConfPdu()
        shouldFail = false
        failureMessage = "Network error"
        networkDelayMs = 0
        clearRequests()
    }

    companion object {
        /**
         * Creates a minimal successful SendConf PDU.
         */
        fun createSuccessSendConfPdu(): ByteArray {
            return byteArrayOf(
                // Message type: SendConf (0x81)
                0x8C.toByte(), PduHeaders.MESSAGE_TYPE_SEND_CONF.toByte(),
                // Transaction ID
                0x98.toByte(), 't'.code.toByte(), 'x'.code.toByte(), 'n'.code.toByte(),
                '1'.code.toByte(), '2'.code.toByte(), '3'.code.toByte(), 0x00,
                // MMS Version: 1.0
                0x8D.toByte(), 0x90.toByte(),
                // Response status: OK
                0x92.toByte(), 0x80.toByte(),
                // Message ID
                0x8B.toByte(), 'm'.code.toByte(), 's'.code.toByte(), 'g'.code.toByte(),
                '_'.code.toByte(), 'i'.code.toByte(), 'd'.code.toByte(), 0x00
            )
        }

        /**
         * Creates a minimal successful RetrieveConf PDU.
         */
        fun createSuccessRetrieveConfPdu(): ByteArray {
            return byteArrayOf(
                // Message type: RetrieveConf (0x84)
                0x8C.toByte(), PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF.toByte(),
                // Transaction ID
                0x98.toByte(), 't'.code.toByte(), 'x'.code.toByte(), 'n'.code.toByte(),
                '4'.code.toByte(), '5'.code.toByte(), '6'.code.toByte(), 0x00,
                // MMS Version: 1.0
                0x8D.toByte(), 0x90.toByte(),
                // Date
                0x85.toByte(), 0x04, 0x00, 0x00, 0x00, 0x01,
                // From
                0x89.toByte(), 0x0E,
                0x80.toByte(),
                '+'.code.toByte(), '1'.code.toByte(), '5'.code.toByte(),
                '5'.code.toByte(), '5'.code.toByte(), '1'.code.toByte(),
                '2'.code.toByte(), '3'.code.toByte(), '4'.code.toByte(),
                '5'.code.toByte(), '6'.code.toByte(), '7'.code.toByte(),
                0x00,
                // Content type
                0x84.toByte(), 0x9F.toByte() // multipart/related
            )
        }

        /**
         * Creates a SendConf PDU with an error status.
         */
        fun createErrorSendConfPdu(errorStatus: Int = 0x81): ByteArray {
            return byteArrayOf(
                // Message type: SendConf (0x81)
                0x8C.toByte(), PduHeaders.MESSAGE_TYPE_SEND_CONF.toByte(),
                // Transaction ID
                0x98.toByte(), 't'.code.toByte(), 'x'.code.toByte(), 'n'.code.toByte(),
                '1'.code.toByte(), '2'.code.toByte(), '3'.code.toByte(), 0x00,
                // MMS Version: 1.0
                0x8D.toByte(), 0x90.toByte(),
                // Response status: Error
                0x92.toByte(), errorStatus.toByte()
            )
        }

        /**
         * Creates a NotificationInd PDU for receive testing.
         */
        fun createNotificationIndPdu(contentLocation: String): ByteArray {
            val locationBytes = contentLocation.toByteArray()
            val result = mutableListOf<Byte>()

            // Message type: NotificationInd (0x82)
            result.add(0x8C.toByte())
            result.add(PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND.toByte())

            // Transaction ID
            result.add(0x98.toByte())
            result.addAll("txn789".toByteArray().toList())
            result.add(0x00)

            // MMS Version: 1.0
            result.add(0x8D.toByte())
            result.add(0x90.toByte())

            // Content location
            result.add(0x83.toByte())
            result.addAll(locationBytes.toList())
            result.add(0x00)

            // Message class: personal
            result.add(0x8A.toByte())
            result.add(0x80.toByte())

            // Message size
            result.add(0x8E.toByte())
            result.add(0x04)
            result.add(0x00)
            result.add(0x01)
            result.add(0x00)
            result.add(0x00)

            // Expiry
            result.add(0x88.toByte())
            result.add(0x05)
            result.add(0x81.toByte())
            result.add(0x03)
            result.add(0x00)
            result.add(0x00)
            result.add(0x00)

            return result.toByteArray()
        }
    }
}

/**
 * Exception thrown by MockMmsHttpClient when configured to fail.
 */
class MockMmsException(message: String) : Exception(message)
