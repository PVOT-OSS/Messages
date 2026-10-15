/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.google.android.mms

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MMSPartTest {

    @Test
    fun givenDefaultConstructor_whenCreated_thenHasEmptyDefaults() {
        val part = MMSPart()

        assertEquals("", part.name)
        assertEquals("", part.mimeType)
        assertNull(part.data)
    }

    @Test
    fun givenNameOnly_whenCreated_thenHasCorrectName() {
        val part = MMSPart(name = "image.jpg")

        assertEquals("image.jpg", part.name)
        assertEquals("", part.mimeType)
        assertNull(part.data)
    }

    @Test
    fun givenAllParameters_whenCreated_thenHasAllValues() {
        val testData = "Hello World".toByteArray()
        val part = MMSPart(name = "message.txt", mimeType = ContentType.TEXT_PLAIN, data = testData)

        assertEquals("message.txt", part.name)
        assertEquals(ContentType.TEXT_PLAIN, part.mimeType)
        assertArrayEquals(testData, part.data)
    }

    @Test
    fun givenImagePart_whenCreated_thenHasCorrectMimeType() {
        val imageData = ByteArray(100) { it.toByte() }
        val part = MMSPart(name = "photo.jpg", mimeType = ContentType.IMAGE_JPEG, data = imageData)

        assertEquals("photo.jpg", part.name)
        assertEquals(ContentType.IMAGE_JPEG, part.mimeType)
        assertEquals(100, part.data?.size)
    }

    @Test
    fun givenAudioPart_whenCreated_thenHasCorrectMimeType() {
        val audioData = ByteArray(500) { it.toByte() }
        val part = MMSPart(name = "voice.mp3", mimeType = ContentType.AUDIO_MP3, data = audioData)

        assertEquals("voice.mp3", part.name)
        assertEquals(ContentType.AUDIO_MP3, part.mimeType)
        assertNotNull(part.data)
    }

    @Test
    fun givenVideoPart_whenCreated_thenHasCorrectMimeType() {
        val videoData = ByteArray(1000) { it.toByte() }
        val part = MMSPart(name = "clip.mp4", mimeType = ContentType.VIDEO_MP4, data = videoData)

        assertEquals("clip.mp4", part.name)
        assertEquals(ContentType.VIDEO_MP4, part.mimeType)
        assertEquals(1000, part.data?.size)
    }

    @Test
    fun givenVCardPart_whenCreated_thenHasCorrectMimeType() {
        val vCardData =
            """
            BEGIN:VCARD
            VERSION:3.0
            FN:John Doe
            TEL:+1234567890
            END:VCARD
        """
                .trimIndent()
                .toByteArray()

        val part =
            MMSPart(name = "contact.vcf", mimeType = ContentType.TEXT_VCARD, data = vCardData)

        assertEquals("contact.vcf", part.name)
        assertEquals(ContentType.TEXT_VCARD, part.mimeType)
    }

    @Test
    fun givenSmilPart_whenCreated_thenHasCorrectMimeType() {
        val smilData =
            """
            <smil>
                <body>
                    <par dur="5000ms">
                        <img src="image.jpg"/>
                    </par>
                </body>
            </smil>
        """
                .trimIndent()
                .toByteArray()

        val part = MMSPart(name = "smil.xml", mimeType = ContentType.APP_SMIL, data = smilData)

        assertEquals("smil.xml", part.name)
        assertEquals(ContentType.APP_SMIL, part.mimeType)
    }

    @Test
    fun givenPart_whenNameModified_thenUpdatesCorrectly() {
        val part = MMSPart(name = "old.jpg")

        part.name = "new.jpg"

        assertEquals("new.jpg", part.name)
    }

    @Test
    fun givenPart_whenMimeTypeModified_thenUpdatesCorrectly() {
        val part = MMSPart(mimeType = ContentType.IMAGE_JPEG)

        part.mimeType = ContentType.IMAGE_PNG

        assertEquals(ContentType.IMAGE_PNG, part.mimeType)
    }

    @Test
    fun givenPart_whenDataModified_thenUpdatesCorrectly() {
        val part = MMSPart(data = "old".toByteArray())

        part.data = "new".toByteArray()

        assertArrayEquals("new".toByteArray(), part.data)
    }

    @Test
    fun givenPartWithData_whenDataSetToNull_thenDataIsNull() {
        val part = MMSPart(data = "test".toByteArray())

        part.data = null

        assertNull(part.data)
    }

    @Test
    fun givenEmptyData_whenCreated_thenDataIsEmpty() {
        val part = MMSPart(data = ByteArray(0))

        assertEquals(0, part.data?.size)
    }

    @Test
    fun givenLargeData_whenCreated_thenHandlesCorrectly() {
        val largeData = ByteArray(1024 * 1024) { it.toByte() } // 1MB

        val part =
            MMSPart(
                name = "large_file.bin",
                mimeType = "application/octet-stream",
                data = largeData,
            )

        assertEquals(1024 * 1024, part.data?.size)
    }

    @Test
    fun givenGifImage_whenCreated_thenHasCorrectMimeType() {
        val part =
            MMSPart(name = "animation.gif", mimeType = ContentType.IMAGE_GIF, data = ByteArray(50))

        assertEquals(ContentType.IMAGE_GIF, part.mimeType)
    }

    @Test
    fun givenPngImage_whenCreated_thenHasCorrectMimeType() {
        val part =
            MMSPart(name = "screenshot.png", mimeType = ContentType.IMAGE_PNG, data = ByteArray(75))

        assertEquals(ContentType.IMAGE_PNG, part.mimeType)
    }

    @Test
    fun givenAmrAudio_whenCreated_thenHasCorrectMimeType() {
        val part =
            MMSPart(name = "recording.amr", mimeType = ContentType.AUDIO_AMR, data = ByteArray(200))

        assertEquals(ContentType.AUDIO_AMR, part.mimeType)
    }

    @Test
    fun given3gppVideo_whenCreated_thenHasCorrectMimeType() {
        val part =
            MMSPart(name = "video.3gp", mimeType = ContentType.VIDEO_3GPP, data = ByteArray(300))

        assertEquals(ContentType.VIDEO_3GPP, part.mimeType)
    }
}
