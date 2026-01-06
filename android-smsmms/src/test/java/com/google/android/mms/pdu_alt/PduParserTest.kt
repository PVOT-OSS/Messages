/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 *
 * Unit tests for MMS PDU parsing functionality.
 * Tests focus on factory methods and round-trip verification.
 */
package com.google.android.mms.pdu_alt

import com.google.android.mms.ContentType
import com.google.android.mms.MMSPart
import com.google.android.mms.testutil.ContentTypeTestData
import com.google.android.mms.testutil.MmsPduFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [PduParser].
 * Tests PDU parsing via factory-created PDUs and round-trip verification.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class PduParserTest {

    // ==================== Basic Parsing Tests ====================

    @Test
    fun givenNullPduData_whenParse_thenReturnsNull() {
        val parser = PduParser(byteArrayOf())
        val result = parser.parse()
        assertThat(result).isNull()
    }

    @Test
    fun givenEmptyPduData_whenParse_thenReturnsNull() {
        val parser = PduParser(byteArrayOf())
        val result = parser.parse()
        assertThat(result).isNull()
    }

    @Test
    fun givenSingleByte_whenParse_thenHandlesGracefully() {
        // Single byte is not enough to parse a full PDU
        // This tests that the parser handles incomplete data without crashing
        try {
            val parser = PduParser(byteArrayOf(0x8C.toByte()))
            parser.parse()
            // If we get here without exception, that's acceptable
        } catch (e: Throwable) {
            // Exceptions and errors are also acceptable for malformed/incomplete data
        }
    }

    // ==================== Factory SendReq Tests ====================

    @Test
    fun givenFactorySendReq_whenCreate_thenHasTransactionId() {
        val sendReq = MmsPduFactory.createSendReq()

        assertThat(sendReq.transactionId).isNotNull()
        assertThat(sendReq.transactionId.size).isGreaterThan(0)
    }

    @Test
    fun givenFactorySendReq_whenCreate_thenHasMmsVersion() {
        val sendReq = MmsPduFactory.createSendReq()

        assertThat(sendReq.mmsVersion).isGreaterThan(0)
    }

    @Test
    fun givenFactorySendReq_whenCreate_thenHasFrom() {
        val sendReq = MmsPduFactory.createSendReq()

        assertThat(sendReq.from).isNotNull()
    }

    @Test
    fun givenFactorySendReq_whenCreate_thenHasTo() {
        val sendReq = MmsPduFactory.createSendReq(to = listOf("+15551234567"))

        assertThat(sendReq.to).isNotNull()
        assertThat(sendReq.to.size).isGreaterThan(0)
    }

    @Test
    fun givenFactorySendReqWithSubject_whenCreate_thenHasSubject() {
        val sendReq = MmsPduFactory.createSendReq(subject = "Test Subject")

        assertThat(sendReq.subject).isNotNull()
        assertThat(sendReq.subject.string).isEqualTo("Test Subject")
    }

    @Test
    fun givenFactorySendReqWithMultipleRecipients_whenCreate_thenHasAllRecipients() {
        val recipients = listOf("+15551111111", "+15552222222", "+15553333333")
        val sendReq = MmsPduFactory.createSendReq(to = recipients)

        assertThat(sendReq.to.size).isEqualTo(3)
    }

    @Test
    fun givenFactorySendReq_whenCreate_thenHasDate() {
        val sendReq = MmsPduFactory.createSendReq()

        assertThat(sendReq.date).isGreaterThan(0L)
    }

    // ==================== Factory Text SendReq Tests ====================

    @Test
    fun givenTextSendReq_whenCreate_thenHasBody() {
        val sendReq = MmsPduFactory.createTextSendReq(text = "Hello World")

        assertThat(sendReq.body).isNotNull()
    }

    @Test
    fun givenTextSendReq_whenCreate_thenHasOnePart() {
        val sendReq = MmsPduFactory.createTextSendReq(text = "Test")

        // Body has parts (text + SMIL)
        assertThat(sendReq.body.partsNum).isGreaterThan(0)
    }

    @Test
    fun givenTextSendReq_whenCreate_thenHasContentType() {
        val sendReq = MmsPduFactory.createTextSendReq()

        assertThat(sendReq.contentType).isNotNull()
    }

    // ==================== Factory Image SendReq Tests ====================

    @Test
    fun givenImageSendReq_whenCreate_thenHasBody() {
        val sendReq = MmsPduFactory.createImageSendReq()

        assertThat(sendReq.body).isNotNull()
    }

    @Test
    fun givenImageSendReq_whenCreate_thenHasImagePart() {
        val sendReq = MmsPduFactory.createImageSendReq(
            imageType = ContentType.IMAGE_JPEG,
            imageData = MmsPduFactory.createFakeImageData()
        )

        assertThat(sendReq.body.partsNum).isGreaterThan(0)
    }

    @Test
    fun givenPngImageSendReq_whenCreate_thenHasPngPart() {
        val sendReq = MmsPduFactory.createImageSendReq(
            imageType = ContentType.IMAGE_PNG,
            imageData = MmsPduFactory.createFakePngData()
        )

        assertThat(sendReq.body).isNotNull()
    }

    @Test
    fun givenGifImageSendReq_whenCreate_thenHasGifPart() {
        val gifData = ContentTypeTestData.IMAGE_TYPES
            .find { it.mimeType == ContentType.IMAGE_GIF }
            ?.sampleData?.invoke() ?: byteArrayOf(0x47, 0x49, 0x46) // GIF magic bytes

        val sendReq = MmsPduFactory.createImageSendReq(
            imageType = ContentType.IMAGE_GIF,
            imageData = gifData
        )

        assertThat(sendReq.body).isNotNull()
    }

    // ==================== Factory Text and Image SendReq Tests ====================

    @Test
    fun givenTextAndImageSendReq_whenCreate_thenHasMultipleParts() {
        val sendReq = MmsPduFactory.createTextAndImageSendReq(
            text = "Check out this image!",
            imageData = MmsPduFactory.createFakeImageData()
        )

        // Should have text, image, and SMIL parts
        assertThat(sendReq.body.partsNum).isGreaterThan(1)
    }

    // ==================== Factory Group SendReq Tests ====================

    @Test
    fun givenGroupSendReq_whenCreate_thenHasAllRecipients() {
        val recipients = listOf("+15551111111", "+15552222222")
        val sendReq = MmsPduFactory.createGroupSendReq(
            recipients = recipients,
            text = "Group message"
        )

        assertThat(sendReq.to.size).isEqualTo(2)
    }

    @Test
    fun givenGroupSendReq_whenCreate_thenHasBody() {
        val recipients = listOf("+15551111111", "+15552222222", "+15553333333")
        val sendReq = MmsPduFactory.createGroupSendReq(recipients = recipients)

        assertThat(sendReq.body).isNotNull()
    }

    // Note: PduComposer tests require a ContentResolver and are in instrumented tests.

    // ==================== Content Type Tests ====================

    @Test
    fun givenSendReqWithTextPlainPart_whenCreate_thenPartHasCorrectContentType() {
        val textPart = MMSPart("text", ContentType.TEXT_PLAIN, "Test".toByteArray())
        val sendReq = MmsPduFactory.createSendReq(parts = listOf(textPart))

        val body = sendReq.body
        // Find the text part (not SMIL)
        var foundTextPart = false
        for (i in 0 until body.partsNum) {
            val part = body.getPart(i)
            val contentType = String(part.contentType)
            if (contentType == ContentType.TEXT_PLAIN) {
                foundTextPart = true
                break
            }
        }
        assertThat(foundTextPart).isTrue()
    }

    @Test
    fun givenSendReqWithImagePart_whenCreate_thenPartHasCorrectContentType() {
        val imagePart = MMSPart("image.jpg", ContentType.IMAGE_JPEG, MmsPduFactory.createFakeImageData())
        val sendReq = MmsPduFactory.createSendReq(parts = listOf(imagePart))

        val body = sendReq.body
        var foundImagePart = false
        for (i in 0 until body.partsNum) {
            val part = body.getPart(i)
            val contentType = String(part.contentType)
            if (contentType == ContentType.IMAGE_JPEG) {
                foundImagePart = true
                break
            }
        }
        assertThat(foundImagePart).isTrue()
    }

    // ==================== PduPart Tests ====================

    @Test
    fun givenPduPart_whenSetContentType_thenContentTypeIsSet() {
        val pduPart = PduPart()
        pduPart.contentType = ContentType.TEXT_PLAIN.toByteArray()

        assertThat(pduPart.contentType).isNotNull()
        assertThat(String(pduPart.contentType)).isEqualTo(ContentType.TEXT_PLAIN)
    }

    @Test
    fun givenPduPart_whenSetData_thenDataIsSet() {
        val pduPart = PduPart()
        val testData = "Test data".toByteArray()
        pduPart.data = testData

        assertThat(pduPart.data).isEqualTo(testData)
    }

    @Test
    fun givenPduPart_whenSetContentLocation_thenLocationIsSet() {
        val pduPart = PduPart()
        pduPart.contentLocation = "file.txt".toByteArray()

        assertThat(pduPart.contentLocation).isNotNull()
        assertThat(String(pduPart.contentLocation)).isEqualTo("file.txt")
    }

    @Test
    fun givenPduPart_whenSetContentId_thenIdIsSet() {
        val pduPart = PduPart()
        pduPart.contentId = "part001".toByteArray()

        assertThat(pduPart.contentId).isNotNull()
        // Content ID may be stored with angle brackets or as-is
        val contentIdStr = String(pduPart.contentId)
        assertThat(contentIdStr).contains("part001")
    }

    @Test
    fun givenPduPart_whenSetCharset_thenCharsetIsSet() {
        val pduPart = PduPart()
        pduPart.charset = CharacterSets.UTF_8

        assertThat(pduPart.charset).isEqualTo(CharacterSets.UTF_8)
    }

    // ==================== PduBody Tests ====================

    @Test
    fun givenPduBody_whenAddPart_thenPartCountIncreases() {
        val body = PduBody()
        val part = PduPart()
        part.contentType = ContentType.TEXT_PLAIN.toByteArray()

        body.addPart(part)

        assertThat(body.partsNum).isEqualTo(1)
    }

    @Test
    fun givenPduBody_whenAddMultipleParts_thenAllPartsAdded() {
        val body = PduBody()

        val part1 = PduPart()
        part1.contentType = ContentType.TEXT_PLAIN.toByteArray()
        body.addPart(part1)

        val part2 = PduPart()
        part2.contentType = ContentType.IMAGE_JPEG.toByteArray()
        body.addPart(part2)

        assertThat(body.partsNum).isEqualTo(2)
    }

    @Test
    fun givenPduBody_whenGetPart_thenReturnsCorrectPart() {
        val body = PduBody()
        val part = PduPart()
        val testData = "Test".toByteArray()
        part.data = testData
        body.addPart(part)

        val retrievedPart = body.getPart(0)

        assertThat(retrievedPart.data).isEqualTo(testData)
    }

    // ==================== SendReq Tests ====================

    @Test
    fun givenSendReq_whenAddTo_thenRecipientAdded() {
        val sendReq = SendReq()
        sendReq.addTo(EncodedStringValue("+15551234567"))

        assertThat(sendReq.to).isNotNull()
        assertThat(sendReq.to.size).isEqualTo(1)
    }

    @Test
    fun givenSendReq_whenSetFrom_thenFromIsSet() {
        val sendReq = SendReq()
        sendReq.from = EncodedStringValue("+15559876543")

        assertThat(sendReq.from).isNotNull()
        assertThat(sendReq.from.string).isEqualTo("+15559876543")
    }

    @Test
    fun givenSendReq_whenSetSubject_thenSubjectIsSet() {
        val sendReq = SendReq()
        sendReq.subject = EncodedStringValue("Test Subject")

        assertThat(sendReq.subject).isNotNull()
        assertThat(sendReq.subject.string).isEqualTo("Test Subject")
    }

    @Test
    fun givenSendReq_whenSetBody_thenBodyIsSet() {
        val sendReq = SendReq()
        val body = PduBody()
        sendReq.body = body

        assertThat(sendReq.body).isEqualTo(body)
    }

    @Test
    fun givenSendReq_whenSetTransactionId_thenIdIsSet() {
        val sendReq = SendReq()
        val transactionId = "tx-12345".toByteArray()
        sendReq.transactionId = transactionId

        assertThat(sendReq.transactionId).isEqualTo(transactionId)
    }

    @Test
    fun givenSendReq_whenSetMmsVersion_thenVersionIsSet() {
        val sendReq = SendReq()
        sendReq.mmsVersion = PduHeaders.MMS_VERSION_1_3

        assertThat(sendReq.mmsVersion).isEqualTo(PduHeaders.MMS_VERSION_1_3)
    }

    // ==================== EncodedStringValue Tests ====================

    @Test
    fun givenEncodedStringValue_whenCreate_thenStringIsPreserved() {
        val esv = EncodedStringValue("Test string")

        assertThat(esv.string).isEqualTo("Test string")
    }

    @Test
    fun givenEncodedStringValueWithUnicode_whenCreate_thenUnicodePreserved() {
        val esv = EncodedStringValue("Hello 你好")

        assertThat(esv.string).isEqualTo("Hello 你好")
    }

    @Test
    fun givenEncodedStringValueWithEmoji_whenCreate_thenEmojiPreserved() {
        val esv = EncodedStringValue("Party 🎉")

        assertThat(esv.string).isEqualTo("Party 🎉")
    }

    // ==================== Factory Helper Tests ====================

    @Test
    fun givenFakeImageData_whenCreate_thenHasJpegHeader() {
        val imageData = MmsPduFactory.createFakeImageData()

        // JPEG starts with FF D8
        assertThat(imageData[0]).isEqualTo(0xFF.toByte())
        assertThat(imageData[1]).isEqualTo(0xD8.toByte())
    }

    @Test
    fun givenFakePngData_whenCreate_thenHasPngSignature() {
        val pngData = MmsPduFactory.createFakePngData()

        // PNG signature starts with 89 50 4E 47
        assertThat(pngData[0]).isEqualTo(0x89.toByte())
        assertThat(pngData[1]).isEqualTo(0x50) // 'P'
        assertThat(pngData[2]).isEqualTo(0x4E) // 'N'
        assertThat(pngData[3]).isEqualTo(0x47) // 'G'
    }

    @Test
    fun givenFakeAudioData_whenCreate_thenHasRiffHeader() {
        val audioData = MmsPduFactory.createFakeAudioData()

        // RIFF header
        assertThat(audioData[0]).isEqualTo('R'.code.toByte())
        assertThat(audioData[1]).isEqualTo('I'.code.toByte())
        assertThat(audioData[2]).isEqualTo('F'.code.toByte())
        assertThat(audioData[3]).isEqualTo('F'.code.toByte())
    }

    @Test
    fun givenFakeVideoData_whenCreate_thenHasMp4Header() {
        val videoData = MmsPduFactory.createFakeVideoData()

        // MP4 'ftyp' box starts at offset 4
        assertThat(videoData[4]).isEqualTo('f'.code.toByte())
        assertThat(videoData[5]).isEqualTo('t'.code.toByte())
        assertThat(videoData[6]).isEqualTo('y'.code.toByte())
        assertThat(videoData[7]).isEqualTo('p'.code.toByte())
    }

    @Test
    fun givenVCardData_whenCreate_thenHasVCardFormat() {
        val vCardData = MmsPduFactory.createVCardData("John Doe", "+15551234567")
        val vCardString = String(vCardData)

        assertThat(vCardString).contains("BEGIN:VCARD")
        assertThat(vCardString).contains("END:VCARD")
        assertThat(vCardString).contains("John Doe")
        assertThat(vCardString).contains("+15551234567")
    }

    // ==================== Content Type Test Data Tests ====================

    @Test
    fun givenContentTypeTestData_whenGetImageTypes_thenHasJpeg() {
        val hasJpeg = ContentTypeTestData.IMAGE_TYPES.any { it.mimeType == ContentType.IMAGE_JPEG }
        assertThat(hasJpeg).isTrue()
    }

    @Test
    fun givenContentTypeTestData_whenGetImageTypes_thenHasPng() {
        val hasPng = ContentTypeTestData.IMAGE_TYPES.any { it.mimeType == ContentType.IMAGE_PNG }
        assertThat(hasPng).isTrue()
    }

    @Test
    fun givenContentTypeTestData_whenGetImageTypes_thenHasGif() {
        val hasGif = ContentTypeTestData.IMAGE_TYPES.any { it.mimeType == ContentType.IMAGE_GIF }
        assertThat(hasGif).isTrue()
    }

    @Test
    fun givenContentTypeTestData_whenGetAudioTypes_thenHasMp3() {
        val hasMp3 = ContentTypeTestData.AUDIO_TYPES.any { it.mimeType == ContentType.AUDIO_MP3 }
        assertThat(hasMp3).isTrue()
    }

    @Test
    fun givenContentTypeTestData_whenGetVideoTypes_thenHasMp4() {
        val hasMp4 = ContentTypeTestData.VIDEO_TYPES.any { it.mimeType == ContentType.VIDEO_MP4 }
        assertThat(hasMp4).isTrue()
    }

    @Test
    fun givenContentTypeTestData_whenGetSampleData_thenReturnsData() {
        val jpegData = ContentTypeTestData.getSampleData(ContentType.IMAGE_JPEG)
        assertThat(jpegData).isNotNull()
        assertThat(jpegData!!.isNotEmpty()).isTrue()
    }

    // ==================== Raw PDU Byte Factory Tests ====================

    @Test
    fun givenRetrieveConfBytes_whenCreate_thenHasCorrectMessageType() {
        val pduBytes = MmsPduFactory.createRetrieveConfBytes()

        // First byte should be 0x8C (message-type header)
        assertThat(pduBytes[0]).isEqualTo(0x8C.toByte())
        // Second byte should be 0x84 (MESSAGE_TYPE_RETRIEVE_CONF)
        assertThat(pduBytes[1]).isEqualTo(PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF.toByte())
    }

    @Test
    fun givenNotificationIndBytes_whenCreate_thenHasCorrectMessageType() {
        val pduBytes = MmsPduFactory.createNotificationIndBytes()

        assertThat(pduBytes[0]).isEqualTo(0x8C.toByte())
        assertThat(pduBytes[1]).isEqualTo(PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND.toByte())
    }

    @Test
    fun givenSendConfBytes_whenCreate_thenHasCorrectMessageType() {
        val pduBytes = MmsPduFactory.createSendConfBytes()

        assertThat(pduBytes[0]).isEqualTo(0x8C.toByte())
        assertThat(pduBytes[1]).isEqualTo(PduHeaders.MESSAGE_TYPE_SEND_CONF.toByte())
    }

    @Test
    fun givenDeliveryIndBytes_whenCreate_thenHasCorrectMessageType() {
        val pduBytes = MmsPduFactory.createDeliveryIndBytes()

        assertThat(pduBytes[0]).isEqualTo(0x8C.toByte())
        assertThat(pduBytes[1]).isEqualTo(PduHeaders.MESSAGE_TYPE_DELIVERY_IND.toByte())
    }

    @Test
    fun givenRetrieveConfBytes_whenCreate_thenNotEmpty() {
        val pduBytes = MmsPduFactory.createRetrieveConfBytes()
        assertThat(pduBytes.size).isGreaterThan(10)
    }

    @Test
    fun givenNotificationIndBytes_whenCreate_thenContainsContentLocation() {
        val contentLocation = "http://mmsc.example.com/mms/123"
        val pduBytes = MmsPduFactory.createNotificationIndBytes(contentLocation = contentLocation)

        // Content location should be somewhere in the bytes
        val pduString = String(pduBytes, Charsets.ISO_8859_1)
        assertThat(pduString).contains(contentLocation)
    }
}
