/*
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.google.android.mms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentTypeTest {

    // Image Type Tests
    @Test
    fun givenJpegContentType_whenIsImageType_thenReturnsTrue() {
        assertTrue(ContentType.isImageType(ContentType.IMAGE_JPEG))
    }

    @Test
    fun givenPngContentType_whenIsImageType_thenReturnsTrue() {
        assertTrue(ContentType.isImageType(ContentType.IMAGE_PNG))
    }

    @Test
    fun givenGifContentType_whenIsImageType_thenReturnsTrue() {
        assertTrue(ContentType.isImageType(ContentType.IMAGE_GIF))
    }

    @Test
    fun givenBmpContentType_whenIsImageType_thenReturnsTrue() {
        assertTrue(ContentType.isImageType(ContentType.IMAGE_X_MS_BMP))
    }

    @Test
    fun givenWbmpContentType_whenIsImageType_thenReturnsTrue() {
        assertTrue(ContentType.isImageType(ContentType.IMAGE_WBMP))
    }

    @Test
    fun givenImageUnspecified_whenIsImageType_thenReturnsTrue() {
        assertTrue(ContentType.isImageType(ContentType.IMAGE_UNSPECIFIED))
    }

    @Test
    fun givenAudioType_whenIsImageType_thenReturnsFalse() {
        assertFalse(ContentType.isImageType(ContentType.AUDIO_MP3))
    }

    // Audio Type Tests
    @Test
    fun givenMp3ContentType_whenIsAudioType_thenReturnsTrue() {
        assertTrue(ContentType.isAudioType(ContentType.AUDIO_MP3))
    }

    @Test
    fun givenAacContentType_whenIsAudioType_thenReturnsTrue() {
        assertTrue(ContentType.isAudioType(ContentType.AUDIO_AAC))
    }

    @Test
    fun givenAmrContentType_whenIsAudioType_thenReturnsTrue() {
        assertTrue(ContentType.isAudioType(ContentType.AUDIO_AMR))
    }

    @Test
    fun givenMidiContentType_whenIsAudioType_thenReturnsTrue() {
        assertTrue(ContentType.isAudioType(ContentType.AUDIO_MIDI))
    }

    @Test
    fun givenWavContentType_whenIsAudioType_thenReturnsTrue() {
        assertTrue(ContentType.isAudioType(ContentType.AUDIO_X_WAV))
    }

    @Test
    fun givenOggContentType_whenIsAudioType_thenReturnsFalse() {
        // OGG is "application/ogg" not "audio/*"
        assertFalse(ContentType.isAudioType(ContentType.AUDIO_OGG))
    }

    @Test
    fun givenVideoType_whenIsAudioType_thenReturnsFalse() {
        assertFalse(ContentType.isAudioType(ContentType.VIDEO_MP4))
    }

    // Video Type Tests
    @Test
    fun givenMp4ContentType_whenIsVideoType_thenReturnsTrue() {
        assertTrue(ContentType.isVideoType(ContentType.VIDEO_MP4))
    }

    @Test
    fun given3gppContentType_whenIsVideoType_thenReturnsTrue() {
        assertTrue(ContentType.isVideoType(ContentType.VIDEO_3GPP))
    }

    @Test
    fun given3g2ContentType_whenIsVideoType_thenReturnsTrue() {
        assertTrue(ContentType.isVideoType(ContentType.VIDEO_3G2))
    }

    @Test
    fun givenH263ContentType_whenIsVideoType_thenReturnsTrue() {
        assertTrue(ContentType.isVideoType(ContentType.VIDEO_H263))
    }

    @Test
    fun givenImageType_whenIsVideoType_thenReturnsFalse() {
        assertFalse(ContentType.isVideoType(ContentType.IMAGE_JPEG))
    }

    // Text Type Tests
    @Test
    fun givenTextPlain_whenIsTextType_thenReturnsTrue() {
        assertTrue(ContentType.isTextType(ContentType.TEXT_PLAIN))
    }

    @Test
    fun givenTextHtml_whenIsTextType_thenReturnsTrue() {
        assertTrue(ContentType.isTextType(ContentType.TEXT_HTML))
    }

    @Test
    fun givenTextVCard_whenIsTextType_thenReturnsTrue() {
        assertTrue(ContentType.isTextType(ContentType.TEXT_VCARD))
    }

    @Test
    fun givenTextVCalendar_whenIsTextType_thenReturnsTrue() {
        assertTrue(ContentType.isTextType(ContentType.TEXT_VCALENDAR))
    }

    @Test
    fun givenImageType_whenIsTextType_thenReturnsFalse() {
        assertFalse(ContentType.isTextType(ContentType.IMAGE_JPEG))
    }

    // DRM Type Tests
    @Test
    fun givenDrmContent_whenIsDrmType_thenReturnsTrue() {
        assertTrue(ContentType.isDrmType(ContentType.APP_DRM_CONTENT))
    }

    @Test
    fun givenDrmMessage_whenIsDrmType_thenReturnsTrue() {
        assertTrue(ContentType.isDrmType(ContentType.APP_DRM_MESSAGE))
    }

    @Test
    fun givenNonDrmType_whenIsDrmType_thenReturnsFalse() {
        assertFalse(ContentType.isDrmType(ContentType.IMAGE_JPEG))
    }

    // Supported Type Tests
    @Test
    fun givenJpegImage_whenIsSupportedType_thenReturnsTrue() {
        assertTrue(ContentType.isSupportedType(ContentType.IMAGE_JPEG))
    }

    @Test
    fun givenMp3Audio_whenIsSupportedType_thenReturnsTrue() {
        assertTrue(ContentType.isSupportedType(ContentType.AUDIO_MP3))
    }

    @Test
    fun givenMp4Video_whenIsSupportedType_thenReturnsTrue() {
        assertTrue(ContentType.isSupportedType(ContentType.VIDEO_MP4))
    }

    @Test
    fun givenSmilApp_whenIsSupportedType_thenReturnsTrue() {
        assertTrue(ContentType.isSupportedType(ContentType.APP_SMIL))
    }

    @Test
    fun givenUnknownType_whenIsSupportedType_thenReturnsFalse() {
        assertFalse(ContentType.isSupportedType("application/unknown"))
    }

    // Supported Image Type Tests
    @Test
    fun givenJpeg_whenIsSupportedImageType_thenReturnsTrue() {
        assertTrue(ContentType.isSupportedImageType(ContentType.IMAGE_JPEG))
    }

    @Test
    fun givenPng_whenIsSupportedImageType_thenReturnsTrue() {
        assertTrue(ContentType.isSupportedImageType(ContentType.IMAGE_PNG))
    }

    // Supported Audio Type Tests
    @Test
    fun givenMp3_whenIsSupportedAudioType_thenReturnsTrue() {
        assertTrue(ContentType.isSupportedAudioType(ContentType.AUDIO_MP3))
    }

    @Test
    fun givenAmr_whenIsSupportedAudioType_thenReturnsTrue() {
        assertTrue(ContentType.isSupportedAudioType(ContentType.AUDIO_AMR))
    }

    // Supported Video Type Tests
    @Test
    fun givenMp4Video_whenIsSupportedVideoType_thenReturnsTrue() {
        assertTrue(ContentType.isSupportedVideoType(ContentType.VIDEO_MP4))
    }

    @Test
    fun given3gpp_whenIsSupportedVideoType_thenReturnsTrue() {
        assertTrue(ContentType.isSupportedVideoType(ContentType.VIDEO_3GPP))
    }

    // Unspecified Type Tests
    @Test
    fun givenImageUnspecified_whenIsUnspecified_thenReturnsTrue() {
        assertTrue(ContentType.isUnspecified(ContentType.IMAGE_UNSPECIFIED))
    }

    @Test
    fun givenAudioUnspecified_whenIsUnspecified_thenReturnsTrue() {
        assertTrue(ContentType.isUnspecified(ContentType.AUDIO_UNSPECIFIED))
    }

    @Test
    fun givenVideoUnspecified_whenIsUnspecified_thenReturnsTrue() {
        assertTrue(ContentType.isUnspecified(ContentType.VIDEO_UNSPECIFIED))
    }

    @Test
    fun givenSpecificType_whenIsUnspecified_thenReturnsFalse() {
        assertFalse(ContentType.isUnspecified(ContentType.IMAGE_JPEG))
    }

    // Null Handling Tests
    @Test
    fun givenNullContentType_whenIsImageType_thenReturnsFalse() {
        assertFalse(ContentType.isImageType(null))
    }

    @Test
    fun givenNullContentType_whenIsAudioType_thenReturnsFalse() {
        assertFalse(ContentType.isAudioType(null))
    }

    @Test
    fun givenNullContentType_whenIsVideoType_thenReturnsFalse() {
        assertFalse(ContentType.isVideoType(null))
    }

    @Test
    fun givenNullContentType_whenIsTextType_thenReturnsFalse() {
        assertFalse(ContentType.isTextType(null))
    }

    @Test
    fun givenNullContentType_whenIsSupportedType_thenReturnsFalse() {
        assertFalse(ContentType.isSupportedType(null))
    }

    // Get Types Tests
    @Test
    fun givenGetImageTypes_whenCalled_thenReturnsNonEmptyList() {
        val imageTypes = ContentType.getImageTypes()
        assertNotNull(imageTypes)
        assertTrue(imageTypes.isNotEmpty())
        assertTrue(imageTypes.contains(ContentType.IMAGE_JPEG))
        assertTrue(imageTypes.contains(ContentType.IMAGE_PNG))
    }

    @Test
    fun givenGetAudioTypes_whenCalled_thenReturnsNonEmptyList() {
        val audioTypes = ContentType.getAudioTypes()
        assertNotNull(audioTypes)
        assertTrue(audioTypes.isNotEmpty())
        assertTrue(audioTypes.contains(ContentType.AUDIO_MP3))
        assertTrue(audioTypes.contains(ContentType.AUDIO_AMR))
    }

    @Test
    fun givenGetVideoTypes_whenCalled_thenReturnsNonEmptyList() {
        val videoTypes = ContentType.getVideoTypes()
        assertNotNull(videoTypes)
        assertTrue(videoTypes.isNotEmpty())
        assertTrue(videoTypes.contains(ContentType.VIDEO_MP4))
        assertTrue(videoTypes.contains(ContentType.VIDEO_3GPP))
    }

    @Test
    fun givenGetSupportedTypes_whenCalled_thenReturnsNonEmptyList() {
        val supportedTypes = ContentType.getSupportedTypes()
        assertNotNull(supportedTypes)
        assertTrue(supportedTypes.isNotEmpty())
    }

    // Constant Value Tests
    @Test
    fun givenMmsMessage_thenHasCorrectValue() {
        assertEquals("application/vnd.wap.mms-message", ContentType.MMS_MESSAGE)
    }

    @Test
    fun givenMultipartMixed_thenHasCorrectValue() {
        assertEquals("application/vnd.wap.multipart.mixed", ContentType.MULTIPART_MIXED)
    }

    @Test
    fun givenMultipartRelated_thenHasCorrectValue() {
        assertEquals("application/vnd.wap.multipart.related", ContentType.MULTIPART_RELATED)
    }

    @Test
    fun givenAppSmil_thenHasCorrectValue() {
        assertEquals("application/smil", ContentType.APP_SMIL)
    }

    @Test
    fun givenTextPlain_thenHasCorrectValue() {
        assertEquals("text/plain", ContentType.TEXT_PLAIN)
    }

    @Test
    fun givenTextVCard_thenHasCorrectValue() {
        assertEquals("text/x-vCard", ContentType.TEXT_VCARD)
    }
}
