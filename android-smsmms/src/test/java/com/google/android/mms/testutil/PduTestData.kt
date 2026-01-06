/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 *
 * Raw binary PDU test data for MMS parsing tests.
 */
package com.google.android.mms.testutil

import com.google.android.mms.pdu_alt.PduHeaders

/**
 * Provides raw binary PDU data for testing PduParser.
 * These are carefully constructed byte arrays that represent valid and invalid MMS PDUs.
 */
object PduTestData {

    // ==================== Message Type Bytes ====================

    const val MESSAGE_TYPE_SEND_REQ = PduHeaders.MESSAGE_TYPE_SEND_REQ
    const val MESSAGE_TYPE_SEND_CONF = PduHeaders.MESSAGE_TYPE_SEND_CONF
    const val MESSAGE_TYPE_NOTIFICATION_IND = PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND
    const val MESSAGE_TYPE_RETRIEVE_CONF = PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF
    const val MESSAGE_TYPE_DELIVERY_IND = PduHeaders.MESSAGE_TYPE_DELIVERY_IND

    // ==================== Header Field Bytes ====================

    const val HEADER_MESSAGE_TYPE: Byte = 0x8C.toByte()
    const val HEADER_TRANSACTION_ID: Byte = 0x98.toByte()
    const val HEADER_MMS_VERSION: Byte = 0x8D.toByte()
    const val HEADER_FROM: Byte = 0x89.toByte()
    const val HEADER_TO: Byte = 0x97.toByte()
    const val HEADER_CC: Byte = 0x82.toByte()
    const val HEADER_BCC: Byte = 0x81.toByte()
    const val HEADER_SUBJECT: Byte = 0x96.toByte()
    const val HEADER_DATE: Byte = 0x85.toByte()
    const val HEADER_CONTENT_TYPE: Byte = 0x84.toByte()
    const val HEADER_CONTENT_LOCATION: Byte = 0x83.toByte()
    const val HEADER_EXPIRY: Byte = 0x88.toByte()
    const val HEADER_MESSAGE_CLASS: Byte = 0x8A.toByte()
    const val HEADER_MESSAGE_SIZE: Byte = 0x8E.toByte()
    const val HEADER_MESSAGE_ID: Byte = 0x8B.toByte()
    const val HEADER_RESPONSE_STATUS: Byte = 0x92.toByte()
    const val HEADER_STATUS: Byte = 0x95.toByte()
    const val HEADER_PRIORITY: Byte = 0x8F.toByte()
    const val HEADER_DELIVERY_REPORT: Byte = 0x86.toByte()
    const val HEADER_READ_REPORT: Byte = 0x90.toByte()

    // ==================== MMS Version Bytes ====================

    const val MMS_VERSION_1_0: Byte = 0x90.toByte() // (1 << 4) | 0
    const val MMS_VERSION_1_1: Byte = 0x91.toByte() // (1 << 4) | 1
    const val MMS_VERSION_1_2: Byte = 0x92.toByte() // (1 << 4) | 2
    const val MMS_VERSION_1_3: Byte = 0x93.toByte() // (1 << 4) | 3

    // ==================== Status Bytes ====================

    const val RESPONSE_STATUS_OK: Byte = 0x80.toByte()
    const val RESPONSE_STATUS_ERROR: Byte = 0x81.toByte()

    // ==================== Content Type Constants ====================

    // Well-known content type indices (with high bit set)
    const val CONTENT_TYPE_MULTIPART_MIXED: Byte = 0xA3.toByte()
    const val CONTENT_TYPE_MULTIPART_RELATED: Byte = 0xB3.toByte()
    const val CONTENT_TYPE_TEXT_PLAIN: Byte = 0x83.toByte()
    const val CONTENT_TYPE_IMAGE_JPEG: Byte = 0x9E.toByte()
    const val CONTENT_TYPE_IMAGE_GIF: Byte = 0x9D.toByte()
    const val CONTENT_TYPE_IMAGE_PNG: Byte = 0xA0.toByte()
    const val CONTENT_TYPE_APPLICATION_SMIL: Byte = 0xB1.toByte()

    // ==================== Valid PDU Samples ====================

    /**
     * Creates a minimal valid SendReq PDU.
     */
    fun createMinimalSendReqPdu(): ByteArray {
        return byteArrayOf(
            // Message Type: M-Send.req
            HEADER_MESSAGE_TYPE, MESSAGE_TYPE_SEND_REQ.toByte(),

            // Transaction ID
            HEADER_TRANSACTION_ID, *"TEST001".toByteArray(), 0x00,

            // MMS Version: 1.3
            HEADER_MMS_VERSION, MMS_VERSION_1_3,

            // From: +1234567890/TYPE=PLMN
            HEADER_FROM,
            0x0F, // Value-length
            0x80.toByte(), // Address-present-token
            *"+1234567890".toByteArray(), 0x00,

            // To: +0987654321/TYPE=PLMN
            HEADER_TO,
            *"+0987654321".toByteArray(), 0x00,

            // Content-Type: application/vnd.wap.multipart.related
            HEADER_CONTENT_TYPE, CONTENT_TYPE_MULTIPART_RELATED,

            // Number of parts: 1
            0x01,

            // Part 1 header length
            0x10,
            // Part 1 data length
            0x0B,

            // Part 1 Content-Type: text/plain
            CONTENT_TYPE_TEXT_PLAIN,

            // Part 1 headers (Content-Location)
            0xAE.toByte(), // Content-Location
            *"text".toByteArray(), 0x00,

            // Part 1 data
            *"Hello World".toByteArray()
        )
    }

    /**
     * Creates a minimal valid RetrieveConf PDU.
     */
    fun createMinimalRetrieveConfPdu(): ByteArray {
        return byteArrayOf(
            // Message Type: M-Retrieve.conf
            HEADER_MESSAGE_TYPE, MESSAGE_TYPE_RETRIEVE_CONF.toByte(),

            // MMS Version: 1.3
            HEADER_MMS_VERSION, MMS_VERSION_1_3,

            // Message ID
            HEADER_MESSAGE_ID, *"MSG001".toByteArray(), 0x00,

            // Date (4 bytes: current time / 1000)
            HEADER_DATE, 0x04, 0x5F, 0x5E, 0x10, 0x00,

            // From
            HEADER_FROM,
            0x0F,
            0x80.toByte(),
            *"+1234567890".toByteArray(), 0x00,

            // Content-Type: application/vnd.wap.multipart.related
            HEADER_CONTENT_TYPE, CONTENT_TYPE_MULTIPART_RELATED,

            // Number of parts: 1
            0x01,

            // Part 1 header length
            0x10,
            // Part 1 data length
            0x0C,

            // Part 1 Content-Type
            CONTENT_TYPE_TEXT_PLAIN,

            // Part 1 headers
            0xAE.toByte(),
            *"text".toByteArray(), 0x00,

            // Part 1 data
            *"Test message".toByteArray()
        )
    }

    /**
     * Creates a minimal valid NotificationInd PDU.
     */
    fun createMinimalNotificationIndPdu(): ByteArray {
        return byteArrayOf(
            // Message Type
            HEADER_MESSAGE_TYPE, MESSAGE_TYPE_NOTIFICATION_IND.toByte(),

            // Transaction ID
            HEADER_TRANSACTION_ID, *"NOTIF001".toByteArray(), 0x00,

            // MMS Version
            HEADER_MMS_VERSION, MMS_VERSION_1_3,

            // Message Class: Personal
            HEADER_MESSAGE_CLASS, 0x80.toByte(),

            // Message Size (4 bytes for 50000)
            HEADER_MESSAGE_SIZE, 0x04, 0x00, 0x00, 0xC3.toByte(), 0x50,

            // Expiry (relative, 86400 seconds = 24 hours)
            HEADER_EXPIRY, 0x04, 0x81.toByte(), 0x00, 0x01, 0x51, 0x80.toByte(),

            // Content Location
            HEADER_CONTENT_LOCATION,
            *"http://mmsc.example.com/mms/download/123".toByteArray(), 0x00,

            // From
            HEADER_FROM,
            0x0F,
            0x80.toByte(),
            *"+1234567890".toByteArray(), 0x00
        )
    }

    /**
     * Creates a minimal valid SendConf PDU.
     */
    fun createMinimalSendConfPdu(): ByteArray {
        return byteArrayOf(
            // Message Type
            HEADER_MESSAGE_TYPE, MESSAGE_TYPE_SEND_CONF.toByte(),

            // Transaction ID
            HEADER_TRANSACTION_ID, *"TEST001".toByteArray(), 0x00,

            // MMS Version
            HEADER_MMS_VERSION, MMS_VERSION_1_3,

            // Response Status: OK
            HEADER_RESPONSE_STATUS, RESPONSE_STATUS_OK,

            // Message ID
            HEADER_MESSAGE_ID, *"SENT001".toByteArray(), 0x00
        )
    }

    /**
     * Creates a minimal valid DeliveryInd PDU.
     */
    fun createMinimalDeliveryIndPdu(): ByteArray {
        return byteArrayOf(
            // Message Type
            HEADER_MESSAGE_TYPE, MESSAGE_TYPE_DELIVERY_IND.toByte(),

            // MMS Version
            HEADER_MMS_VERSION, MMS_VERSION_1_3,

            // Message ID
            HEADER_MESSAGE_ID, *"MSG001".toByteArray(), 0x00,

            // To
            HEADER_TO, *"+0987654321".toByteArray(), 0x00,

            // Status: Retrieved
            HEADER_STATUS, 0x81.toByte(),

            // Date
            HEADER_DATE, 0x04, 0x5F, 0x5E, 0x10, 0x00
        )
    }

    // ==================== Invalid/Edge Case PDU Samples ====================

    /**
     * Empty PDU - should fail parsing.
     */
    fun createEmptyPdu(): ByteArray = byteArrayOf()

    /**
     * Null bytes only - should fail parsing.
     */
    fun createNullPdu(): ByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x00)

    /**
     * Truncated PDU - header without value.
     */
    fun createTruncatedPdu(): ByteArray = byteArrayOf(
        HEADER_MESSAGE_TYPE // No value following
    )

    /**
     * PDU with invalid message type.
     */
    fun createInvalidMessageTypePdu(): ByteArray = byteArrayOf(
        HEADER_MESSAGE_TYPE, 0xFF.toByte(), // Invalid message type
        HEADER_MMS_VERSION, MMS_VERSION_1_3
    )

    /**
     * PDU missing mandatory MMS version header.
     */
    fun createMissingVersionPdu(): ByteArray = byteArrayOf(
        HEADER_MESSAGE_TYPE, MESSAGE_TYPE_SEND_REQ.toByte(),
        HEADER_TRANSACTION_ID, *"TEST001".toByteArray(), 0x00,
        HEADER_FROM, 0x0F, 0x80.toByte(), *"+1234567890".toByteArray(), 0x00,
        HEADER_CONTENT_TYPE, CONTENT_TYPE_MULTIPART_RELATED
        // Missing MMS_VERSION
    )

    /**
     * PDU with malformed content-type header.
     */
    fun createMalformedContentTypePdu(): ByteArray = byteArrayOf(
        HEADER_MESSAGE_TYPE, MESSAGE_TYPE_SEND_REQ.toByte(),
        HEADER_MMS_VERSION, MMS_VERSION_1_3,
        HEADER_TRANSACTION_ID, *"TEST001".toByteArray(), 0x00,
        HEADER_FROM, 0x0F, 0x80.toByte(), *"+1234567890".toByteArray(), 0x00,
        HEADER_CONTENT_TYPE, 0x00 // Invalid content type value
    )

    // ==================== Multi-Part PDU Samples ====================

    /**
     * Creates a PDU with multiple parts (text + image).
     */
    fun createMultiPartPdu(): ByteArray {
        val textData = "Hello World".toByteArray()
        val imageData = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) // JPEG start

        return byteArrayOf(
            // Headers
            HEADER_MESSAGE_TYPE, MESSAGE_TYPE_RETRIEVE_CONF.toByte(),
            HEADER_MMS_VERSION, MMS_VERSION_1_3,
            HEADER_MESSAGE_ID, *"MSG001".toByteArray(), 0x00,
            HEADER_DATE, 0x04, 0x5F, 0x5E, 0x10, 0x00,
            HEADER_FROM, 0x0F, 0x80.toByte(), *"+1234567890".toByteArray(), 0x00,
            HEADER_CONTENT_TYPE, CONTENT_TYPE_MULTIPART_RELATED,

            // Number of parts: 2
            0x02,

            // Part 1: Text
            0x10, // Header length
            textData.size.toByte(), // Data length
            CONTENT_TYPE_TEXT_PLAIN,
            0xAE.toByte(), *"text.txt".toByteArray(), 0x00,
            *textData,

            // Part 2: Image
            0x12, // Header length
            imageData.size.toByte(), // Data length
            CONTENT_TYPE_IMAGE_JPEG,
            0xAE.toByte(), *"image.jpg".toByteArray(), 0x00,
            *imageData
        )
    }

    /**
     * Creates a PDU with SMIL and media parts.
     */
    fun createSmilPdu(): ByteArray {
        val smilData = """<smil><head><layout><root-layout/></layout></head><body><par><text src="text.txt"/></par></body></smil>""".toByteArray()
        val textData = "Hello".toByteArray()

        return byteArrayOf(
            HEADER_MESSAGE_TYPE, MESSAGE_TYPE_RETRIEVE_CONF.toByte(),
            HEADER_MMS_VERSION, MMS_VERSION_1_3,
            HEADER_MESSAGE_ID, *"SMIL001".toByteArray(), 0x00,
            HEADER_DATE, 0x04, 0x5F, 0x5E, 0x10, 0x00,
            HEADER_FROM, 0x0F, 0x80.toByte(), *"+1234567890".toByteArray(), 0x00,
            HEADER_CONTENT_TYPE, CONTENT_TYPE_MULTIPART_RELATED,

            // Number of parts: 2
            0x02,

            // Part 1: SMIL
            0x10,
            smilData.size.toByte(),
            CONTENT_TYPE_APPLICATION_SMIL,
            0xAE.toByte(), *"smil.xml".toByteArray(), 0x00,
            *smilData,

            // Part 2: Text
            0x10,
            textData.size.toByte(),
            CONTENT_TYPE_TEXT_PLAIN,
            0xAE.toByte(), *"text.txt".toByteArray(), 0x00,
            *textData
        )
    }

    // ==================== Character Encoding PDU Samples ====================

    /**
     * Creates a PDU with UTF-8 encoded text.
     */
    fun createUtf8TextPdu(): ByteArray {
        val utf8Text = "Hello 你好 مرحبا 🎉".toByteArray(Charsets.UTF_8)

        return byteArrayOf(
            HEADER_MESSAGE_TYPE, MESSAGE_TYPE_RETRIEVE_CONF.toByte(),
            HEADER_MMS_VERSION, MMS_VERSION_1_3,
            HEADER_MESSAGE_ID, *"UTF8-001".toByteArray(), 0x00,
            HEADER_DATE, 0x04, 0x5F, 0x5E, 0x10, 0x00,
            HEADER_FROM, 0x0F, 0x80.toByte(), *"+1234567890".toByteArray(), 0x00,
            HEADER_CONTENT_TYPE, CONTENT_TYPE_MULTIPART_RELATED,

            0x01,

            0x14,
            utf8Text.size.toByte(),

            // Content-Type with charset parameter
            0x03, // Value-length for content-type
            CONTENT_TYPE_TEXT_PLAIN,
            0x81.toByte(), // Charset parameter
            0x6A, // UTF-8 charset code (106)

            0xAE.toByte(), *"text.txt".toByteArray(), 0x00,
            *utf8Text
        )
    }

    /**
     * Creates a PDU with emoji in subject.
     */
    fun createEmojiSubjectPdu(): ByteArray {
        val subjectBytes = "Party time! 🎉🎊".toByteArray(Charsets.UTF_8)

        return byteArrayOf(
            HEADER_MESSAGE_TYPE, MESSAGE_TYPE_RETRIEVE_CONF.toByte(),
            HEADER_MMS_VERSION, MMS_VERSION_1_3,
            HEADER_MESSAGE_ID, *"EMOJI-001".toByteArray(), 0x00,
            HEADER_DATE, 0x04, 0x5F, 0x5E, 0x10, 0x00,
            HEADER_FROM, 0x0F, 0x80.toByte(), *"+1234567890".toByteArray(), 0x00,

            // Subject with emoji
            HEADER_SUBJECT,
            (subjectBytes.size + 2).toByte(), // Value-length
            0x6A, // UTF-8 charset
            *subjectBytes, 0x00,

            HEADER_CONTENT_TYPE, CONTENT_TYPE_MULTIPART_RELATED,

            0x01,
            0x10,
            0x04,
            CONTENT_TYPE_TEXT_PLAIN,
            0xAE.toByte(), *"t".toByteArray(), 0x00,
            *"test".toByteArray()
        )
    }

    // ==================== Large/Boundary Condition PDUs ====================

    /**
     * Creates a PDU at the maximum reasonable size for testing.
     */
    fun createLargePdu(partDataSize: Int = 300000): ByteArray {
        val largeData = ByteArray(partDataSize) { (it % 256).toByte() }

        // For sizes over 127, we need uintvar encoding
        val sizeBytes = encodeUintvar(partDataSize)

        return byteArrayOf(
            HEADER_MESSAGE_TYPE, MESSAGE_TYPE_RETRIEVE_CONF.toByte(),
            HEADER_MMS_VERSION, MMS_VERSION_1_3,
            HEADER_MESSAGE_ID, *"LARGE-001".toByteArray(), 0x00,
            HEADER_DATE, 0x04, 0x5F, 0x5E, 0x10, 0x00,
            HEADER_FROM, 0x0F, 0x80.toByte(), *"+1234567890".toByteArray(), 0x00,
            HEADER_CONTENT_TYPE, CONTENT_TYPE_MULTIPART_RELATED,

            0x01, // Number of parts

            0x12, // Header length
            *sizeBytes, // Data length (uintvar encoded)

            CONTENT_TYPE_IMAGE_JPEG,
            0xAE.toByte(), *"large.jpg".toByteArray(), 0x00,
            *largeData
        )
    }

    /**
     * Encodes an integer as a uintvar (variable-length unsigned integer).
     */
    private fun encodeUintvar(value: Int): ByteArray {
        if (value < 128) {
            return byteArrayOf(value.toByte())
        }

        val bytes = mutableListOf<Byte>()
        var remaining = value

        while (remaining > 0) {
            val b = (remaining and 0x7F).toByte()
            remaining = remaining shr 7
            bytes.add(0, b)
        }

        // Set continuation bits on all bytes except the last
        for (i in 0 until bytes.size - 1) {
            bytes[i] = (bytes[i].toInt() or 0x80).toByte()
        }

        return bytes.toByteArray()
    }
}
