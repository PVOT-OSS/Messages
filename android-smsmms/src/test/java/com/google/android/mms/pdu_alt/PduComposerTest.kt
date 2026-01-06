/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 *
 * Unit tests for MMS PDU encoding/composition functionality.
 * Note: PduComposer.make() requires a ContentResolver for body parts,
 * so these tests focus on structure validation and factory methods.
 * Full composition tests are in instrumented tests.
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
 * Unit tests for [PduComposer].
 * Tests focus on SendReq structure validation and factory methods.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class PduComposerTest {

    // ==================== SendReq Structure Tests ====================

    @Test
    fun givenSendReqWithRecipient_whenCreate_thenHasTo() {
        val sendReq = MmsPduFactory.createSendReq(to = listOf("+15551234567"))

        assertThat(sendReq.to).isNotNull()
        assertThat(sendReq.to.size).isEqualTo(1)
        assertThat(sendReq.to[0].string).isEqualTo("+15551234567")
    }

    @Test
    fun givenSendReqWithMultipleRecipients_whenCreate_thenHasAllRecipients() {
        val recipients = listOf("+15551111111", "+15552222222", "+15553333333")
        val sendReq = MmsPduFactory.createSendReq(to = recipients)

        assertThat(sendReq.to.size).isEqualTo(3)
        val toAddresses = sendReq.to.map { it.string }
        assertThat(toAddresses).containsExactlyElementsIn(recipients)
    }

    @Test
    fun givenSendReqWithSubject_whenCreate_thenHasSubject() {
        val sendReq = MmsPduFactory.createSendReq(
            to = listOf("+15551234567"),
            subject = "Test Subject"
        )

        assertThat(sendReq.subject).isNotNull()
        assertThat(sendReq.subject.string).isEqualTo("Test Subject")
    }

    @Test
    fun givenSendReqWithFrom_whenCreate_thenHasFrom() {
        val sendReq = MmsPduFactory.createSendReq(from = "+15559876543")

        assertThat(sendReq.from).isNotNull()
        assertThat(sendReq.from.string).isEqualTo("+15559876543")
    }

    @Test
    fun givenSendReq_whenCreate_thenHasTransactionId() {
        val sendReq = MmsPduFactory.createSendReq()

        assertThat(sendReq.transactionId).isNotNull()
        assertThat(sendReq.transactionId.size).isGreaterThan(0)
    }

    @Test
    fun givenSendReq_whenCreate_thenHasMmsVersion() {
        val sendReq = MmsPduFactory.createSendReq()

        assertThat(sendReq.mmsVersion).isEqualTo(PduHeaders.MMS_VERSION_1_3)
    }

    @Test
    fun givenSendReq_whenCreate_thenHasDate() {
        val beforeTime = System.currentTimeMillis() / 1000
        val sendReq = MmsPduFactory.createSendReq()
        val afterTime = System.currentTimeMillis() / 1000

        assertThat(sendReq.date).isAtLeast(beforeTime)
        assertThat(sendReq.date).isAtMost(afterTime)
    }

    @Test
    fun givenSendReq_whenCreate_thenHasMessageClass() {
        val sendReq = MmsPduFactory.createSendReq()

        assertThat(sendReq.messageClass).isNotNull()
    }

    // ==================== Body Structure Tests ====================

    @Test
    fun givenSendReqWithTextPart_whenCreate_thenBodyHasTextPart() {
        val textPart = MMSPart("text", ContentType.TEXT_PLAIN, "Test message".toByteArray())
        val sendReq = MmsPduFactory.createSendReq(parts = listOf(textPart))

        val body = sendReq.body
        assertThat(body).isNotNull()
        assertThat(body.partsNum).isGreaterThan(0)

        // Find text part
        var foundText = false
        for (i in 0 until body.partsNum) {
            val part = body.getPart(i)
            if (String(part.contentType) == ContentType.TEXT_PLAIN) {
                foundText = true
                break
            }
        }
        assertThat(foundText).isTrue()
    }

    @Test
    fun givenSendReqWithImagePart_whenCreate_thenBodyHasImagePart() {
        val imagePart = MMSPart("image.jpg", ContentType.IMAGE_JPEG, MmsPduFactory.createFakeImageData())
        val sendReq = MmsPduFactory.createSendReq(parts = listOf(imagePart))

        val body = sendReq.body
        assertThat(body).isNotNull()

        var foundImage = false
        for (i in 0 until body.partsNum) {
            val part = body.getPart(i)
            if (String(part.contentType) == ContentType.IMAGE_JPEG) {
                foundImage = true
                break
            }
        }
        assertThat(foundImage).isTrue()
    }

    @Test
    fun givenSendReqWithMultipleParts_whenCreate_thenBodyHasAllParts() {
        val textPart = MMSPart("text", ContentType.TEXT_PLAIN, "Test".toByteArray())
        val imagePart = MMSPart("image.jpg", ContentType.IMAGE_JPEG, MmsPduFactory.createFakeImageData())
        val sendReq = MmsPduFactory.createSendReq(parts = listOf(textPart, imagePart))

        val body = sendReq.body
        assertThat(body).isNotNull()
        // Should have text, image, and SMIL parts
        assertThat(body.partsNum).isGreaterThan(2)
    }

    @Test
    fun givenSendReqWithParts_whenCreate_thenHasSmilPart() {
        val textPart = MMSPart("text", ContentType.TEXT_PLAIN, "Test".toByteArray())
        val sendReq = MmsPduFactory.createSendReq(parts = listOf(textPart))

        val body = sendReq.body
        assertThat(body).isNotNull()

        var foundSmil = false
        for (i in 0 until body.partsNum) {
            val part = body.getPart(i)
            if (String(part.contentType) == ContentType.APP_SMIL) {
                foundSmil = true
                break
            }
        }
        assertThat(foundSmil).isTrue()
    }

    // ==================== Content Type Tests ====================

    @Test
    fun givenSendReq_whenCreate_thenContentTypeIsMultipartRelated() {
        val sendReq = MmsPduFactory.createSendReq()

        assertThat(sendReq.contentType).isNotNull()
        assertThat(String(sendReq.contentType)).isEqualTo(ContentType.MULTIPART_RELATED)
    }

    @Test
    fun givenTextPart_whenCreate_thenContentTypeIsTextPlain() {
        val textPart = MMSPart("text", ContentType.TEXT_PLAIN, "Test".toByteArray())
        val sendReq = MmsPduFactory.createSendReq(parts = listOf(textPart))

        val body = sendReq.body
        for (i in 0 until body.partsNum) {
            val part = body.getPart(i)
            if (String(part.contentType) == ContentType.TEXT_PLAIN) {
                assertThat(part.data).isEqualTo("Test".toByteArray())
                return
            }
        }
        // Should have found the text part
        assertThat(true).isFalse() // Fail if text part not found
    }

    @Test
    fun givenJpegPart_whenCreate_thenContentTypeIsImageJpeg() {
        val imageData = MmsPduFactory.createFakeImageData()
        val imagePart = MMSPart("image.jpg", ContentType.IMAGE_JPEG, imageData)
        val sendReq = MmsPduFactory.createSendReq(parts = listOf(imagePart))

        val body = sendReq.body
        for (i in 0 until body.partsNum) {
            val part = body.getPart(i)
            if (String(part.contentType) == ContentType.IMAGE_JPEG) {
                assertThat(part.data).isEqualTo(imageData)
                return
            }
        }
        assertThat(true).isFalse() // Fail if image part not found
    }

    @Test
    fun givenPngPart_whenCreate_thenContentTypeIsImagePng() {
        val pngData = MmsPduFactory.createFakePngData()
        val pngPart = MMSPart("image.png", ContentType.IMAGE_PNG, pngData)
        val sendReq = MmsPduFactory.createSendReq(parts = listOf(pngPart))

        val body = sendReq.body
        for (i in 0 until body.partsNum) {
            val part = body.getPart(i)
            if (String(part.contentType) == ContentType.IMAGE_PNG) {
                return // Found the PNG part
            }
        }
        assertThat(true).isFalse() // Fail if PNG part not found
    }

    @Test
    fun givenGifPart_whenCreate_thenContentTypeIsImageGif() {
        val gifData = ContentTypeTestData.getSampleData(ContentType.IMAGE_GIF) ?: byteArrayOf(0x47, 0x49, 0x46)
        val gifPart = MMSPart("image.gif", ContentType.IMAGE_GIF, gifData)
        val sendReq = MmsPduFactory.createSendReq(parts = listOf(gifPart))

        val body = sendReq.body
        for (i in 0 until body.partsNum) {
            val part = body.getPart(i)
            if (String(part.contentType) == ContentType.IMAGE_GIF) {
                return // Found the GIF part
            }
        }
        assertThat(true).isFalse() // Fail if GIF part not found
    }

    @Test
    fun givenAudioMp3Part_whenCreate_thenContentTypeIsAudioMp3() {
        val audioData = MmsPduFactory.createFakeAudioData()
        val audioPart = MMSPart("audio.mp3", ContentType.AUDIO_MP3, audioData)
        val sendReq = MmsPduFactory.createSendReq(parts = listOf(audioPart))

        val body = sendReq.body
        for (i in 0 until body.partsNum) {
            val part = body.getPart(i)
            if (String(part.contentType) == ContentType.AUDIO_MP3) {
                return // Found the audio part
            }
        }
        assertThat(true).isFalse() // Fail if audio part not found
    }

    @Test
    fun givenVideoMp4Part_whenCreate_thenContentTypeIsVideoMp4() {
        val videoData = MmsPduFactory.createFakeVideoData()
        val videoPart = MMSPart("video.mp4", ContentType.VIDEO_MP4, videoData)
        val sendReq = MmsPduFactory.createSendReq(parts = listOf(videoPart))

        val body = sendReq.body
        for (i in 0 until body.partsNum) {
            val part = body.getPart(i)
            if (String(part.contentType) == ContentType.VIDEO_MP4) {
                return // Found the video part
            }
        }
        assertThat(true).isFalse() // Fail if video part not found
    }

    @Test
    fun givenVCardPart_whenCreate_thenContentTypeIsTextVCard() {
        val vCardData = MmsPduFactory.createVCardData("John Doe", "+15551234567")
        val vCardPart = MMSPart("contact.vcf", ContentType.TEXT_VCARD, vCardData)
        val sendReq = MmsPduFactory.createSendReq(parts = listOf(vCardPart))

        val body = sendReq.body
        for (i in 0 until body.partsNum) {
            val part = body.getPart(i)
            if (String(part.contentType) == ContentType.TEXT_VCARD) {
                return // Found the vCard part
            }
        }
        assertThat(true).isFalse() // Fail if vCard part not found
    }

    // ==================== PduPart Structure Tests ====================

    @Test
    fun givenPduPart_whenSetContentType_thenContentTypeSet() {
        val part = PduPart()
        part.contentType = ContentType.TEXT_PLAIN.toByteArray()

        assertThat(part.contentType).isNotNull()
        assertThat(String(part.contentType)).isEqualTo(ContentType.TEXT_PLAIN)
    }

    @Test
    fun givenPduPart_whenSetData_thenDataSet() {
        val part = PduPart()
        val testData = "Test data content".toByteArray()
        part.data = testData

        assertThat(part.data).isEqualTo(testData)
    }

    @Test
    fun givenPduPart_whenSetContentLocation_thenLocationSet() {
        val part = PduPart()
        part.contentLocation = "file.txt".toByteArray()

        assertThat(String(part.contentLocation)).isEqualTo("file.txt")
    }

    @Test
    fun givenPduPart_whenSetCharset_thenCharsetSet() {
        val part = PduPart()
        part.charset = CharacterSets.UTF_8

        assertThat(part.charset).isEqualTo(CharacterSets.UTF_8)
    }

    // ==================== Unicode/Emoji Tests ====================

    @Test
    fun givenSubjectWithUnicode_whenCreate_thenSubjectPreserved() {
        val subject = "Test 你好 مرحبا"
        val sendReq = MmsPduFactory.createSendReq(
            to = listOf("+15551234567"),
            subject = subject
        )

        assertThat(sendReq.subject.string).isEqualTo(subject)
    }

    @Test
    fun givenSubjectWithEmoji_whenCreate_thenSubjectPreserved() {
        val subject = "Party time 🎉🎊"
        val sendReq = MmsPduFactory.createSendReq(
            to = listOf("+15551234567"),
            subject = subject
        )

        assertThat(sendReq.subject.string).isEqualTo(subject)
    }

    @Test
    fun givenTextPartWithUnicode_whenCreate_thenTextPreserved() {
        val text = "Hello 你好 世界"
        val textPart = MMSPart("text", ContentType.TEXT_PLAIN, text.toByteArray(Charsets.UTF_8))
        val sendReq = MmsPduFactory.createSendReq(parts = listOf(textPart))

        val body = sendReq.body
        for (i in 0 until body.partsNum) {
            val part = body.getPart(i)
            if (String(part.contentType) == ContentType.TEXT_PLAIN) {
                assertThat(String(part.data, Charsets.UTF_8)).isEqualTo(text)
                return
            }
        }
        assertThat(true).isFalse() // Fail if text part not found
    }

    @Test
    fun givenTextPartWithEmoji_whenCreate_thenTextPreserved() {
        val text = "Great news! 🎈🎁🥳"
        val textPart = MMSPart("text", ContentType.TEXT_PLAIN, text.toByteArray(Charsets.UTF_8))
        val sendReq = MmsPduFactory.createSendReq(parts = listOf(textPart))

        val body = sendReq.body
        for (i in 0 until body.partsNum) {
            val part = body.getPart(i)
            if (String(part.contentType) == ContentType.TEXT_PLAIN) {
                assertThat(String(part.data, Charsets.UTF_8)).isEqualTo(text)
                return
            }
        }
        assertThat(true).isFalse() // Fail if text part not found
    }

    // ==================== EncodedStringValue Tests ====================

    @Test
    fun givenEncodedStringValue_whenCreate_thenStringPreserved() {
        val value = "Test string value"
        val esv = EncodedStringValue(value)

        assertThat(esv.string).isEqualTo(value)
    }

    @Test
    fun givenEncodedStringValueWithBytes_whenCreate_thenBytesPreserved() {
        val bytes = "Test bytes".toByteArray(Charsets.UTF_8)
        val esv = EncodedStringValue(bytes)

        assertThat(esv.string).isEqualTo("Test bytes")
    }

    @Test
    fun givenEncodedStringValueWithCharset_whenCreate_thenCharsetUsed() {
        val text = "Test with charset"
        val esv = EncodedStringValue(CharacterSets.UTF_8, text.toByteArray(Charsets.UTF_8))

        assertThat(esv.string).isEqualTo(text)
    }

    // ==================== Content Type Detection Tests ====================

    @Test
    fun givenContentTypeTestData_whenGetImageTypes_thenContainsJpeg() {
        val hasJpeg = ContentTypeTestData.IMAGE_TYPES.any { it.mimeType == ContentType.IMAGE_JPEG }
        assertThat(hasJpeg).isTrue()
    }

    @Test
    fun givenContentTypeTestData_whenGetAudioTypes_thenContainsMp3() {
        val hasMp3 = ContentTypeTestData.AUDIO_TYPES.any { it.mimeType == ContentType.AUDIO_MP3 }
        assertThat(hasMp3).isTrue()
    }

    @Test
    fun givenContentTypeTestData_whenGetVideoTypes_thenContainsMp4() {
        val hasMp4 = ContentTypeTestData.VIDEO_TYPES.any { it.mimeType == ContentType.VIDEO_MP4 }
        assertThat(hasMp4).isTrue()
    }

    @Test
    fun givenContentTypeTestData_whenIsImageType_thenJpegSupported() {
        assertThat(ContentTypeTestData.isImageType(ContentType.IMAGE_JPEG)).isTrue()
    }

    @Test
    fun givenContentTypeTestData_whenIsImageType_thenPngSupported() {
        assertThat(ContentTypeTestData.isImageType(ContentType.IMAGE_PNG)).isTrue()
    }

    @Test
    fun givenContentTypeTestData_whenIsImageType_thenGifSupported() {
        assertThat(ContentTypeTestData.isImageType(ContentType.IMAGE_GIF)).isTrue()
    }

    @Test
    fun givenContentTypeTestData_whenIsAudioType_thenMp3Supported() {
        assertThat(ContentTypeTestData.isAudioType(ContentType.AUDIO_MP3)).isTrue()
    }

    @Test
    fun givenContentTypeTestData_whenIsVideoType_thenMp4Supported() {
        assertThat(ContentTypeTestData.isVideoType(ContentType.VIDEO_MP4)).isTrue()
    }

    @Test
    fun givenContentTypeTestData_whenIsTextType_thenVCardSupported() {
        assertThat(ContentTypeTestData.isTextType(ContentType.TEXT_VCARD)).isTrue()
    }
}
