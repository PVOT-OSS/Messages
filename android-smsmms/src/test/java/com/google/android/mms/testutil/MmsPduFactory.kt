/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 *
 * Test utility factory for creating MMS PDU objects for testing purposes.
 */
package com.google.android.mms.testutil

import com.google.android.mms.ContentType
import com.google.android.mms.MMSPart
import com.google.android.mms.pdu_alt.*
import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * Factory for creating test MMS PDU objects.
 * Provides methods to create SendReq PDUs and raw byte arrays for other PDU types
 * (which have package-private constructors) for testing different MMS scenarios.
 */
object MmsPduFactory {

    private const val DEFAULT_PHONE = "+1234567890"
    private const val DEFAULT_TRANSACTION_ID = "test-transaction-001"
    private const val DEFAULT_MESSAGE_ID = "test-message-001"

    // ==================== SendReq Creation ====================

    /**
     * Creates a SendReq PDU with the specified parameters.
     * SendReq has a public constructor so we can use it directly.
     */
    fun createSendReq(
        to: List<String> = listOf(DEFAULT_PHONE),
        from: String = DEFAULT_PHONE,
        subject: String? = null,
        parts: List<MMSPart> = emptyList(),
        transactionId: String = UUID.randomUUID().toString().take(20)
    ): SendReq {
        val sendReq = SendReq()

        // Set transaction ID
        sendReq.transactionId = transactionId.toByteArray()

        // Set MMS version
        sendReq.mmsVersion = PduHeaders.MMS_VERSION_1_3

        // Set from
        sendReq.from = EncodedStringValue(from)

        // Set recipients
        to.forEach { recipient ->
            sendReq.addTo(EncodedStringValue(recipient))
        }

        // Set subject if provided
        subject?.let {
            sendReq.subject = EncodedStringValue(it)
        }

        // Set date
        sendReq.date = System.currentTimeMillis() / 1000

        // Set message class
        sendReq.messageClass = PduHeaders.MESSAGE_CLASS_PERSONAL_STR.toByteArray()

        // Create body with parts
        val body = PduBody()
        parts.forEach { mmsPart ->
            body.addPart(createPduPart(mmsPart))
        }

        // Add SMIL if there are multiple parts
        if (parts.isNotEmpty()) {
            val smilPart = createSmilPart(parts)
            body.addPart(0, smilPart)
        }

        sendReq.body = body

        // Set content type
        sendReq.contentType = ContentType.MULTIPART_RELATED.toByteArray()

        return sendReq
    }

    /**
     * Creates a simple text-only SendReq.
     */
    fun createTextSendReq(
        to: String = DEFAULT_PHONE,
        text: String = "Test message"
    ): SendReq {
        val textPart = MMSPart("text", ContentType.TEXT_PLAIN, text.toByteArray())
        return createSendReq(to = listOf(to), parts = listOf(textPart))
    }

    /**
     * Creates a SendReq with an image attachment.
     */
    fun createImageSendReq(
        to: String = DEFAULT_PHONE,
        imageType: String = ContentType.IMAGE_JPEG,
        imageData: ByteArray = createFakeImageData()
    ): SendReq {
        val imagePart = MMSPart("image.jpg", imageType, imageData)
        return createSendReq(to = listOf(to), parts = listOf(imagePart))
    }

    /**
     * Creates a SendReq with text and image.
     */
    fun createTextAndImageSendReq(
        to: String = DEFAULT_PHONE,
        text: String = "Check out this image!",
        imageData: ByteArray = createFakeImageData()
    ): SendReq {
        val textPart = MMSPart("text", ContentType.TEXT_PLAIN, text.toByteArray())
        val imagePart = MMSPart("image.jpg", ContentType.IMAGE_JPEG, imageData)
        return createSendReq(to = listOf(to), parts = listOf(textPart, imagePart))
    }

    /**
     * Creates a group MMS SendReq with multiple recipients.
     */
    fun createGroupSendReq(
        recipients: List<String>,
        text: String = "Group message"
    ): SendReq {
        val textPart = MMSPart("text", ContentType.TEXT_PLAIN, text.toByteArray())
        return createSendReq(to = recipients, parts = listOf(textPart))
    }

    // ==================== Raw PDU Byte Arrays ====================
    // These methods create raw byte arrays that can be parsed by PduParser
    // because RetrieveConf, NotificationInd, SendConf, and DeliveryInd have
    // package-private constructors.

    /**
     * Creates raw RetrieveConf PDU bytes (incoming MMS).
     */
    fun createRetrieveConfBytes(
        from: String = DEFAULT_PHONE,
        transactionId: String = DEFAULT_TRANSACTION_ID
    ): ByteArray {
        val out = ByteArrayOutputStream()

        // Message type: RetrieveConf (0x84 = 132)
        out.write(0x8C) // Message-type field
        out.write(PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF)

        // Transaction ID
        out.write(0x98) // Transaction-id field
        out.write(transactionId.toByteArray())
        out.write(0x00) // Null terminator

        // MMS Version: 1.0
        out.write(0x8D) // MMS-version field
        out.write(0x90) // Version 1.0

        // Date
        out.write(0x85) // Date field
        val dateValue = (System.currentTimeMillis() / 1000).toInt()
        out.write(0x04) // Long-integer with 4 bytes
        out.write((dateValue shr 24) and 0xFF)
        out.write((dateValue shr 16) and 0xFF)
        out.write((dateValue shr 8) and 0xFF)
        out.write(dateValue and 0xFF)

        // From
        out.write(0x89) // From field
        val fromBytes = from.toByteArray()
        out.write(fromBytes.size + 2) // Length (address-present-token + address + null)
        out.write(0x80) // Address-present-token
        out.write(fromBytes)
        out.write(0x00) // Null terminator

        // Content-Type: multipart/related
        out.write(0x84) // Content-type field
        out.write(0x9F) // multipart/related

        return out.toByteArray()
    }

    /**
     * Creates raw NotificationInd PDU bytes (MMS notification).
     */
    fun createNotificationIndBytes(
        contentLocation: String = "http://mmsc.carrier.com/mms/123",
        messageSize: Long = 50000,
        from: String = DEFAULT_PHONE,
        transactionId: String = DEFAULT_TRANSACTION_ID
    ): ByteArray {
        val out = ByteArrayOutputStream()

        // Message type: NotificationInd (0x82 = 130)
        out.write(0x8C) // Message-type field
        out.write(PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND)

        // Transaction ID
        out.write(0x98) // Transaction-id field
        out.write(transactionId.toByteArray())
        out.write(0x00) // Null terminator

        // MMS Version: 1.0
        out.write(0x8D) // MMS-version field
        out.write(0x90) // Version 1.0

        // From
        out.write(0x89) // From field
        val fromBytes = from.toByteArray()
        out.write(fromBytes.size + 2)
        out.write(0x80) // Address-present-token
        out.write(fromBytes)
        out.write(0x00)

        // Message class: personal
        out.write(0x8A) // Message-class field
        out.write(0x80) // Personal

        // Message size
        out.write(0x8E) // Message-size field
        val sizeValue = messageSize.toInt()
        out.write(0x04) // Long-integer with 4 bytes
        out.write((sizeValue shr 24) and 0xFF)
        out.write((sizeValue shr 16) and 0xFF)
        out.write((sizeValue shr 8) and 0xFF)
        out.write(sizeValue and 0xFF)

        // Content location
        out.write(0x83) // Content-location field
        out.write(contentLocation.toByteArray())
        out.write(0x00)

        // Expiry (relative)
        out.write(0x88) // Expiry field
        out.write(0x05) // Value-length
        out.write(0x81) // Relative-token
        out.write(0x03) // Long-integer with 3 bytes
        out.write(0x01) // 86400 seconds = 24 hours
        out.write(0x51)
        out.write(0x80)

        return out.toByteArray()
    }

    /**
     * Creates raw SendConf PDU bytes (send confirmation).
     */
    fun createSendConfBytes(
        transactionId: String = DEFAULT_TRANSACTION_ID,
        responseStatus: Int = PduHeaders.RESPONSE_STATUS_OK,
        messageId: String = DEFAULT_MESSAGE_ID
    ): ByteArray {
        val out = ByteArrayOutputStream()

        // Message type: SendConf (0x81 = 129)
        out.write(0x8C) // Message-type field
        out.write(PduHeaders.MESSAGE_TYPE_SEND_CONF)

        // Transaction ID
        out.write(0x98) // Transaction-id field
        out.write(transactionId.toByteArray())
        out.write(0x00)

        // MMS Version: 1.0
        out.write(0x8D) // MMS-version field
        out.write(0x90) // Version 1.0

        // Response status
        out.write(0x92) // Response-status field
        out.write(responseStatus)

        // Message ID (only if success)
        if (responseStatus == PduHeaders.RESPONSE_STATUS_OK) {
            out.write(0x8B) // Message-id field
            out.write(messageId.toByteArray())
            out.write(0x00)
        }

        return out.toByteArray()
    }

    /**
     * Creates raw DeliveryInd PDU bytes (delivery report).
     */
    fun createDeliveryIndBytes(
        messageId: String = DEFAULT_MESSAGE_ID,
        to: String = DEFAULT_PHONE,
        status: Int = PduHeaders.STATUS_RETRIEVED
    ): ByteArray {
        val out = ByteArrayOutputStream()

        // Message type: DeliveryInd (0x86 = 134)
        out.write(0x8C) // Message-type field
        out.write(PduHeaders.MESSAGE_TYPE_DELIVERY_IND)

        // MMS Version: 1.0
        out.write(0x8D) // MMS-version field
        out.write(0x90) // Version 1.0

        // Message ID
        out.write(0x8B) // Message-id field
        out.write(messageId.toByteArray())
        out.write(0x00)

        // To
        out.write(0x97) // To field
        out.write(to.toByteArray())
        out.write(0x00)

        // Status
        out.write(0x95) // Status field
        out.write(status)

        // Date
        out.write(0x85) // Date field
        val dateValue = (System.currentTimeMillis() / 1000).toInt()
        out.write(0x04)
        out.write((dateValue shr 24) and 0xFF)
        out.write((dateValue shr 16) and 0xFF)
        out.write((dateValue shr 8) and 0xFF)
        out.write(dateValue and 0xFF)

        return out.toByteArray()
    }

    // ==================== Helper Methods ====================

    /**
     * Converts an MMSPart to a PduPart.
     */
    private fun createPduPart(mmsPart: MMSPart): PduPart {
        val pduPart = PduPart()

        // Set content type
        pduPart.contentType = mmsPart.mimeType.toByteArray()

        // Set content location (filename)
        pduPart.contentLocation = mmsPart.name.toByteArray()

        // Set content ID
        val contentId = mmsPart.name.substringBeforeLast(".")
        pduPart.contentId = contentId.toByteArray()

        // Set charset for text parts
        if (mmsPart.mimeType.startsWith("text/")) {
            pduPart.charset = CharacterSets.UTF_8
        }

        // Set data
        pduPart.data = mmsPart.data

        return pduPart
    }

    /**
     * Creates a SMIL part for the given message parts.
     */
    private fun createSmilPart(parts: List<MMSPart>): PduPart {
        val smilContent = buildSmilDocument(parts)
        val smilPart = PduPart()

        smilPart.contentType = ContentType.APP_SMIL.toByteArray()
        smilPart.contentId = "smil".toByteArray()
        smilPart.contentLocation = "smil.xml".toByteArray()
        smilPart.data = smilContent.toByteArray()

        return smilPart
    }

    /**
     * Builds a SMIL document for the given parts.
     */
    private fun buildSmilDocument(parts: List<MMSPart>): String {
        val sb = StringBuilder()
        sb.append("<smil><head><layout>")
        sb.append("<root-layout width=\"100%\" height=\"100%\"/>")

        // Add regions for different content types
        var hasImage = false
        var hasText = false

        parts.forEach { part ->
            when {
                part.mimeType.startsWith("image/") || part.mimeType.startsWith("video/") -> hasImage = true
                part.mimeType.startsWith("text/") -> hasText = true
            }
        }

        if (hasImage) {
            sb.append("<region id=\"Image\" top=\"0\" left=\"0\" height=\"80%\" width=\"100%\" fit=\"meet\"/>")
        }
        if (hasText) {
            sb.append("<region id=\"Text\" top=\"80%\" left=\"0\" height=\"20%\" width=\"100%\" fit=\"scroll\"/>")
        }

        sb.append("</layout></head><body><par dur=\"5000ms\">")

        // Add references to each part
        parts.forEach { part ->
            val src = part.name
            when {
                part.mimeType.startsWith("image/") -> sb.append("<img src=\"$src\" region=\"Image\"/>")
                part.mimeType.startsWith("video/") -> sb.append("<video src=\"$src\" region=\"Image\"/>")
                part.mimeType.startsWith("audio/") -> sb.append("<audio src=\"$src\"/>")
                part.mimeType.startsWith("text/") -> sb.append("<text src=\"$src\" region=\"Text\"/>")
            }
        }

        sb.append("</par></body></smil>")
        return sb.toString()
    }

    /**
     * Creates fake JPEG image data for testing.
     * This is a minimal valid JPEG header.
     */
    fun createFakeImageData(size: Int = 1024): ByteArray {
        val out = ByteArrayOutputStream()

        // JPEG SOI marker
        out.write(0xFF)
        out.write(0xD8)

        // APP0 marker (JFIF)
        out.write(0xFF)
        out.write(0xE0)
        out.write(0x00)
        out.write(0x10) // Length

        // JFIF identifier
        out.write("JFIF".toByteArray())
        out.write(0x00)

        // Version
        out.write(0x01)
        out.write(0x01)

        // Units (0 = no units)
        out.write(0x00)

        // X density
        out.write(0x00)
        out.write(0x01)

        // Y density
        out.write(0x00)
        out.write(0x01)

        // Thumbnail dimensions
        out.write(0x00)
        out.write(0x00)

        // Fill with padding data
        val padding = ByteArray(size - out.size() - 2) { 0x00 }
        out.write(padding)

        // JPEG EOI marker
        out.write(0xFF)
        out.write(0xD9)

        return out.toByteArray()
    }

    /**
     * Creates fake PNG image data for testing.
     */
    fun createFakePngData(size: Int = 1024): ByteArray {
        val out = ByteArrayOutputStream()

        // PNG signature
        out.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))

        // IHDR chunk (minimal)
        out.write(byteArrayOf(0x00, 0x00, 0x00, 0x0D)) // Length
        out.write("IHDR".toByteArray())
        out.write(byteArrayOf(0x00, 0x00, 0x00, 0x01)) // Width = 1
        out.write(byteArrayOf(0x00, 0x00, 0x00, 0x01)) // Height = 1
        out.write(byteArrayOf(0x08)) // Bit depth = 8
        out.write(byteArrayOf(0x02)) // Color type = RGB
        out.write(byteArrayOf(0x00, 0x00, 0x00)) // Compression, filter, interlace
        out.write(byteArrayOf(0x90.toByte(), 0x77, 0x53, 0xDE.toByte())) // CRC

        // Fill with data
        val padding = ByteArray(maxOf(0, size - out.size() - 12))
        out.write(padding)

        // IEND chunk
        out.write(byteArrayOf(0x00, 0x00, 0x00, 0x00)) // Length
        out.write("IEND".toByteArray())
        out.write(byteArrayOf(0xAE.toByte(), 0x42, 0x60, 0x82.toByte())) // CRC

        return out.toByteArray()
    }

    /**
     * Creates fake audio data for testing.
     */
    fun createFakeAudioData(size: Int = 2048): ByteArray {
        // Simple audio file header (not valid but good for testing)
        return ByteArray(size) { index ->
            when {
                index < 4 -> "RIFF"[index].code.toByte()
                index in 8..11 -> "WAVE"[index - 8].code.toByte()
                else -> 0x00
            }
        }
    }

    /**
     * Creates fake video data for testing.
     */
    fun createFakeVideoData(size: Int = 4096): ByteArray {
        // Simple MP4/3GPP header pattern
        val out = ByteArrayOutputStream()

        // ftyp box
        out.write(byteArrayOf(0x00, 0x00, 0x00, 0x14)) // Size = 20
        out.write("ftyp".toByteArray())
        out.write("3gp4".toByteArray())
        out.write(byteArrayOf(0x00, 0x00, 0x00, 0x00)) // Minor version
        out.write("3gp4".toByteArray()) // Compatible brand

        // Fill remaining with dummy data
        val padding = ByteArray(maxOf(0, size - out.size()))
        out.write(padding)

        return out.toByteArray()
    }

    /**
     * Creates a vCard data string for testing.
     */
    fun createVCardData(
        name: String = "Test Contact",
        phone: String = DEFAULT_PHONE
    ): ByteArray {
        return """
            BEGIN:VCARD
            VERSION:3.0
            N:$name
            FN:$name
            TEL;TYPE=CELL:$phone
            END:VCARD
        """.trimIndent().toByteArray()
    }
}
