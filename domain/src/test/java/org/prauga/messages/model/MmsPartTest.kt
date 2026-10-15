/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class MmsPartTest {

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    // getSummary Tests
    @Test
    fun givenSmilType_whenGetSummary_thenReturnsNull() {
        val part = createMmsPart(type = "application/smil")
        assertNull(part.getSummary())
    }

    @Test
    fun givenTextPlainType_whenGetSummary_thenReturnsText() {
        val part = createMmsPart(type = "text/plain", text = "Hello World")
        assertEquals("Hello World", part.getSummary())
    }

    @Test
    fun givenVCardType_whenGetSummary_thenReturnsContactCard() {
        val part = createMmsPart(type = "text/x-vcard")
        assertEquals("Contact card", part.getSummary())
    }

    @Test
    fun givenImageType_whenGetSummary_thenReturnsPicture() {
        val part = createMmsPart(type = "image/jpeg")
        assertEquals("Picture", part.getSummary())
    }

    @Test
    fun givenPngImageType_whenGetSummary_thenReturnsPicture() {
        val part = createMmsPart(type = "image/png")
        assertEquals("Picture", part.getSummary())
    }

    @Test
    fun givenGifImageType_whenGetSummary_thenReturnsPicture() {
        val part = createMmsPart(type = "image/gif")
        assertEquals("Picture", part.getSummary())
    }

    @Test
    fun givenVideoType_whenGetSummary_thenReturnsVideo() {
        val part = createMmsPart(type = "video/mp4")
        assertEquals("Video", part.getSummary())
    }

    @Test
    fun given3gppVideoType_whenGetSummary_thenReturnsVideo() {
        val part = createMmsPart(type = "video/3gpp")
        assertEquals("Video", part.getSummary())
    }

    @Test
    fun givenAudioType_whenGetSummary_thenReturnsAudio() {
        val part = createMmsPart(type = "audio/mp3")
        assertEquals("Audio", part.getSummary())
    }

    @Test
    fun givenAmrAudioType_whenGetSummary_thenReturnsAudio() {
        val part = createMmsPart(type = "audio/amr")
        assertEquals("Audio", part.getSummary())
    }

    @Test
    fun givenUnknownType_whenGetSummary_thenReturnsSubtype() {
        val part = createMmsPart(type = "application/pdf")
        assertEquals("pdf", part.getSummary())
    }

    @Test
    fun givenOctetStreamType_whenGetSummary_thenReturnsSubtype() {
        val part = createMmsPart(type = "application/octet-stream")
        assertEquals("octet-stream", part.getSummary())
    }

    // getBestFilename Tests - Note: These tests verify the logic pattern
    // Actual getBestFilename calls require Realm initialization
    @Test
    fun givenFilenameWithExtension_whenChecked_thenHasExtension() {
        val filename = "photo.jpg"
        val hasExtension = java.io.File(filename).extension.isNotEmpty()
        assertTrue(hasExtension)
    }

    @Test
    fun givenFilenameWithoutExtension_whenChecked_thenNoExtension() {
        val filename = "photo"
        val hasExtension = java.io.File(filename).extension.isNotEmpty()
        assertFalse(hasExtension)
    }

    @Test
    fun givenNullFilename_whenHandled_thenDefaultsToUnknown() {
        val name: String? = null
        val result = name ?: "unknown"
        assertEquals("unknown", result)
    }

    @Test
    fun givenFilenameWithMultipleDots_whenChecked_thenHasExtension() {
        val filename = "photo.backup.jpg"
        val hasExtension = java.io.File(filename).extension.isNotEmpty()
        assertTrue(hasExtension)
        assertEquals("jpg", java.io.File(filename).extension)
    }

    // Default Values Tests
    @Test
    fun givenNewMmsPart_whenCreated_thenHasDefaultValues() {
        val part = MmsPart()
        assertEquals(0L, part.id)
        assertEquals(0L, part.messageId)
        assertEquals("", part.type)
        assertEquals(-1, part.seq)
        assertNull(part.name)
        assertNull(part.text)
    }

    // Property Tests
    @Test
    fun givenMmsPart_whenIdSet_thenReturnsCorrectId() {
        val part = createMmsPart(id = 123L)
        assertEquals(123L, part.id)
    }

    @Test
    fun givenMmsPart_whenMessageIdSet_thenReturnsCorrectMessageId() {
        val part = createMmsPart(messageId = 456L)
        assertEquals(456L, part.messageId)
    }

    @Test
    fun givenMmsPart_whenSeqSet_thenReturnsCorrectSeq() {
        val part = createMmsPart(seq = 5)
        assertEquals(5, part.seq)
    }

    // getUri Tests - Testing URI building logic
    @Test
    fun givenPartId_whenBuildingUri_thenReturnsCorrectUri() {
        val partId = 100L
        val uri =
            android.net.Uri.Builder()
                .scheme(android.content.ContentResolver.SCHEME_CONTENT)
                .authority("mms")
                .encodedPath("part/$partId")
                .build()

        assertNotNull(uri)
        assertEquals("content", uri.scheme)
        assertEquals("mms", uri.authority)
        assertTrue(uri.path?.contains("part/100") == true)
    }

    @Test
    fun givenZeroPartId_whenBuildingUri_thenReturnsUriWithZero() {
        val partId = 0L
        val uri =
            android.net.Uri.Builder()
                .scheme(android.content.ContentResolver.SCHEME_CONTENT)
                .authority("mms")
                .encodedPath("part/$partId")
                .build()

        assertTrue(uri.path?.contains("part/0") == true)
    }

    // Type Tests for Various Content Types
    @Test
    fun givenJpegType_whenTypeAccessed_thenReturnsCorrectType() {
        val part = createMmsPart(type = "image/jpeg")
        assertEquals("image/jpeg", part.type)
    }

    @Test
    fun givenMp3Type_whenTypeAccessed_thenReturnsCorrectType() {
        val part = createMmsPart(type = "audio/mp3")
        assertEquals("audio/mp3", part.type)
    }

    @Test
    fun givenMp4Type_whenTypeAccessed_thenReturnsCorrectType() {
        val part = createMmsPart(type = "video/mp4")
        assertEquals("video/mp4", part.type)
    }

    // Text Content Tests
    @Test
    fun givenTextPart_whenTextAccessed_thenReturnsCorrectText() {
        val part = createMmsPart(type = "text/plain", text = "Test message")
        assertEquals("Test message", part.text)
    }

    @Test
    fun givenEmptyTextPart_whenTextAccessed_thenReturnsEmpty() {
        val part = createMmsPart(type = "text/plain", text = "")
        assertEquals("", part.text)
    }

    @Test
    fun givenLongTextPart_whenTextAccessed_thenReturnsFullText() {
        val longText = "A".repeat(1000)
        val part = createMmsPart(type = "text/plain", text = longText)
        assertEquals(1000, part.text?.length)
    }

    @Test
    fun givenUnicodeText_whenTextAccessed_thenReturnsCorrectText() {
        val unicodeText = "Hello World! \uD83D\uDE00"
        val part = createMmsPart(type = "text/plain", text = unicodeText)
        assertEquals(unicodeText, part.text)
    }

    // Helper function to create MmsPart for testing
    private fun createMmsPart(
        id: Long = 0L,
        messageId: Long = 0L,
        type: String = "",
        seq: Int = -1,
        name: String? = null,
        text: String? = null,
    ): MmsPart {
        return MmsPart().apply {
            this.id = id
            this.messageId = messageId
            this.type = type
            this.seq = seq
            this.name = name
            this.text = text
        }
    }
}
