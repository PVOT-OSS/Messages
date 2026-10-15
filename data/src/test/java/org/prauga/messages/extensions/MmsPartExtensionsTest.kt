/*
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.extensions

import com.google.android.mms.ContentType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.messages.model.MmsPart
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class MmsPartExtensionsTest {

    // isSmil Tests
    @Test
    fun givenSmilType_whenIsSmil_thenReturnsTrue() {
        val part = createMmsPart(type = ContentType.APP_SMIL)
        assertTrue(part.isSmil())
    }

    @Test
    fun givenSmilTypeUpperCase_whenIsSmil_thenReturnsTrue() {
        val part = createMmsPart(type = "APPLICATION/SMIL")
        assertTrue(part.isSmil())
    }

    @Test
    fun givenSmilTypeMixedCase_whenIsSmil_thenReturnsTrue() {
        val part = createMmsPart(type = "Application/Smil")
        assertTrue(part.isSmil())
    }

    @Test
    fun givenNonSmilType_whenIsSmil_thenReturnsFalse() {
        val part = createMmsPart(type = ContentType.TEXT_PLAIN)
        assertFalse(part.isSmil())
    }

    @Test
    fun givenImageType_whenIsSmil_thenReturnsFalse() {
        val part = createMmsPart(type = ContentType.IMAGE_JPEG)
        assertFalse(part.isSmil())
    }

    // isImage Tests
    @Test
    fun givenJpegType_whenIsImage_thenReturnsTrue() {
        val part = createMmsPart(type = ContentType.IMAGE_JPEG)
        assertTrue(part.isImage())
    }

    @Test
    fun givenPngType_whenIsImage_thenReturnsTrue() {
        val part = createMmsPart(type = ContentType.IMAGE_PNG)
        assertTrue(part.isImage())
    }

    @Test
    fun givenGifType_whenIsImage_thenReturnsTrue() {
        val part = createMmsPart(type = ContentType.IMAGE_GIF)
        assertTrue(part.isImage())
    }

    @Test
    fun givenBmpType_whenIsImage_thenReturnsTrue() {
        val part = createMmsPart(type = ContentType.IMAGE_X_MS_BMP)
        assertTrue(part.isImage())
    }

    @Test
    fun givenWbmpType_whenIsImage_thenReturnsTrue() {
        val part = createMmsPart(type = ContentType.IMAGE_WBMP)
        assertTrue(part.isImage())
    }

    @Test
    fun givenJpgType_whenIsImage_thenReturnsTrue() {
        val part = createMmsPart(type = ContentType.IMAGE_JPG)
        assertTrue(part.isImage())
    }

    @Test
    fun givenImageTypeUpperCase_whenIsImage_thenReturnsTrue() {
        val part = createMmsPart(type = "IMAGE/JPEG")
        assertTrue(part.isImage())
    }

    @Test
    fun givenAudioType_whenIsImage_thenReturnsFalse() {
        val part = createMmsPart(type = ContentType.AUDIO_MP3)
        assertFalse(part.isImage())
    }

    @Test
    fun givenVideoType_whenIsImage_thenReturnsFalse() {
        val part = createMmsPart(type = ContentType.VIDEO_MP4)
        assertFalse(part.isImage())
    }

    // isVideo Tests
    @Test
    fun givenMp4Type_whenIsVideo_thenReturnsTrue() {
        val part = createMmsPart(type = ContentType.VIDEO_MP4)
        assertTrue(part.isVideo())
    }

    @Test
    fun given3gppType_whenIsVideo_thenReturnsTrue() {
        val part = createMmsPart(type = ContentType.VIDEO_3GPP)
        assertTrue(part.isVideo())
    }

    @Test
    fun given3g2Type_whenIsVideo_thenReturnsTrue() {
        val part = createMmsPart(type = ContentType.VIDEO_3G2)
        assertTrue(part.isVideo())
    }

    @Test
    fun givenH263Type_whenIsVideo_thenReturnsTrue() {
        val part = createMmsPart(type = ContentType.VIDEO_H263)
        assertTrue(part.isVideo())
    }

    @Test
    fun givenVideoTypeUpperCase_whenIsVideo_thenReturnsTrue() {
        val part = createMmsPart(type = "VIDEO/MP4")
        assertTrue(part.isVideo())
    }

    @Test
    fun givenImageType_whenIsVideo_thenReturnsFalse() {
        val part = createMmsPart(type = ContentType.IMAGE_JPEG)
        assertFalse(part.isVideo())
    }

    @Test
    fun givenAudioType_whenIsVideo_thenReturnsFalse() {
        val part = createMmsPart(type = ContentType.AUDIO_MP3)
        assertFalse(part.isVideo())
    }

    // isAudio Tests
    @Test
    fun givenMp3Type_whenIsAudio_thenReturnsTrue() {
        val part = createMmsPart(type = ContentType.AUDIO_MP3)
        assertTrue(part.isAudio())
    }

    @Test
    fun givenAacType_whenIsAudio_thenReturnsTrue() {
        val part = createMmsPart(type = ContentType.AUDIO_AAC)
        assertTrue(part.isAudio())
    }

    @Test
    fun givenAmrType_whenIsAudio_thenReturnsTrue() {
        val part = createMmsPart(type = ContentType.AUDIO_AMR)
        assertTrue(part.isAudio())
    }

    @Test
    fun givenMidiType_whenIsAudio_thenReturnsTrue() {
        val part = createMmsPart(type = ContentType.AUDIO_MIDI)
        assertTrue(part.isAudio())
    }

    @Test
    fun givenWavType_whenIsAudio_thenReturnsTrue() {
        val part = createMmsPart(type = ContentType.AUDIO_X_WAV)
        assertTrue(part.isAudio())
    }

    @Test
    fun givenAudio3gppType_whenIsAudio_thenReturnsTrue() {
        val part = createMmsPart(type = ContentType.AUDIO_3GPP)
        assertTrue(part.isAudio())
    }

    @Test
    fun givenAudioTypeUpperCase_whenIsAudio_thenReturnsTrue() {
        val part = createMmsPart(type = "AUDIO/MP3")
        assertTrue(part.isAudio())
    }

    @Test
    fun givenImageType_whenIsAudio_thenReturnsFalse() {
        val part = createMmsPart(type = ContentType.IMAGE_JPEG)
        assertFalse(part.isAudio())
    }

    @Test
    fun givenVideoType_whenIsAudio_thenReturnsFalse() {
        val part = createMmsPart(type = ContentType.VIDEO_MP4)
        assertFalse(part.isAudio())
    }

    // isText Tests
    @Test
    fun givenTextPlainType_whenIsText_thenReturnsTrue() {
        val part = createMmsPart(type = ContentType.TEXT_PLAIN)
        assertTrue(part.isText())
    }

    @Test
    fun givenTextPlainUpperCase_whenIsText_thenReturnsTrue() {
        val part = createMmsPart(type = "TEXT/PLAIN")
        assertTrue(part.isText())
    }

    @Test
    fun givenTextPlainMixedCase_whenIsText_thenReturnsTrue() {
        val part = createMmsPart(type = "Text/Plain")
        assertTrue(part.isText())
    }

    @Test
    fun givenTextHtmlType_whenIsText_thenReturnsFalse() {
        // isText specifically checks for text/plain only
        val part = createMmsPart(type = ContentType.TEXT_HTML)
        assertFalse(part.isText())
    }

    @Test
    fun givenImageType_whenIsText_thenReturnsFalse() {
        val part = createMmsPart(type = ContentType.IMAGE_JPEG)
        assertFalse(part.isText())
    }

    @Test
    fun givenVCardType_whenIsText_thenReturnsFalse() {
        val part = createMmsPart(type = ContentType.TEXT_VCARD)
        assertFalse(part.isText())
    }

    // isVCard Tests
    @Test
    fun givenVCardType_whenIsVCard_thenReturnsTrue() {
        val part = createMmsPart(type = ContentType.TEXT_VCARD)
        assertTrue(part.isVCard())
    }

    @Test
    fun givenVCardTypeUpperCase_whenIsVCard_thenReturnsTrue() {
        val part = createMmsPart(type = "TEXT/X-VCARD")
        assertTrue(part.isVCard())
    }

    @Test
    fun givenVCardTypeMixedCase_whenIsVCard_thenReturnsTrue() {
        val part = createMmsPart(type = "Text/X-VCard")
        assertTrue(part.isVCard())
    }

    @Test
    fun givenTextPlainType_whenIsVCard_thenReturnsFalse() {
        val part = createMmsPart(type = ContentType.TEXT_PLAIN)
        assertFalse(part.isVCard())
    }

    @Test
    fun givenImageType_whenIsVCard_thenReturnsFalse() {
        val part = createMmsPart(type = ContentType.IMAGE_JPEG)
        assertFalse(part.isVCard())
    }

    @Test
    fun givenVCalendarType_whenIsVCard_thenReturnsFalse() {
        val part = createMmsPart(type = ContentType.TEXT_VCALENDAR)
        assertFalse(part.isVCard())
    }

    // Edge Cases
    @Test
    fun givenEmptyType_whenIsImage_thenReturnsFalse() {
        val part = createMmsPart(type = "")
        assertFalse(part.isImage())
    }

    @Test
    fun givenEmptyType_whenIsAudio_thenReturnsFalse() {
        val part = createMmsPart(type = "")
        assertFalse(part.isAudio())
    }

    @Test
    fun givenEmptyType_whenIsVideo_thenReturnsFalse() {
        val part = createMmsPart(type = "")
        assertFalse(part.isVideo())
    }

    @Test
    fun givenEmptyType_whenIsText_thenReturnsFalse() {
        val part = createMmsPart(type = "")
        assertFalse(part.isText())
    }

    @Test
    fun givenEmptyType_whenIsVCard_thenReturnsFalse() {
        val part = createMmsPart(type = "")
        assertFalse(part.isVCard())
    }

    @Test
    fun givenEmptyType_whenIsSmil_thenReturnsFalse() {
        val part = createMmsPart(type = "")
        assertFalse(part.isSmil())
    }

    // Helper function
    private fun createMmsPart(type: String): MmsPart {
        return MmsPart().apply { this.type = type }
    }
}
