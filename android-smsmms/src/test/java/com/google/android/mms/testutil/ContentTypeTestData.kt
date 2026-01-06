/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 *
 * Content type test data for MMS testing.
 */
package com.google.android.mms.testutil

import com.google.android.mms.ContentType

/**
 * Provides test data for all supported MMS content types.
 * Includes MIME types, sample data, and test parameters.
 */
object ContentTypeTestData {

    // ==================== Image Content Types ====================

    data class ContentTypeInfo(
        val mimeType: String,
        val extension: String,
        val sampleData: () -> ByteArray,
        val category: ContentCategory
    )

    enum class ContentCategory {
        IMAGE, AUDIO, VIDEO, TEXT, APPLICATION
    }

    val IMAGE_TYPES = listOf(
        ContentTypeInfo(
            mimeType = ContentType.IMAGE_JPEG,
            extension = "jpg",
            sampleData = { MmsPduFactory.createFakeImageData() },
            category = ContentCategory.IMAGE
        ),
        ContentTypeInfo(
            mimeType = ContentType.IMAGE_PNG,
            extension = "png",
            sampleData = { MmsPduFactory.createFakePngData() },
            category = ContentCategory.IMAGE
        ),
        ContentTypeInfo(
            mimeType = ContentType.IMAGE_GIF,
            extension = "gif",
            sampleData = { createFakeGifData() },
            category = ContentCategory.IMAGE
        ),
        ContentTypeInfo(
            mimeType = ContentType.IMAGE_WBMP,
            extension = "wbmp",
            sampleData = { createFakeWbmpData() },
            category = ContentCategory.IMAGE
        ),
        ContentTypeInfo(
            mimeType = "image/bmp",
            extension = "bmp",
            sampleData = { createFakeBmpData() },
            category = ContentCategory.IMAGE
        )
    )

    // ==================== Audio Content Types ====================

    val AUDIO_TYPES = listOf(
        ContentTypeInfo(
            mimeType = ContentType.AUDIO_MP3,
            extension = "mp3",
            sampleData = { createFakeMp3Data() },
            category = ContentCategory.AUDIO
        ),
        ContentTypeInfo(
            mimeType = ContentType.AUDIO_AAC,
            extension = "aac",
            sampleData = { createFakeAacData() },
            category = ContentCategory.AUDIO
        ),
        ContentTypeInfo(
            mimeType = ContentType.AUDIO_AMR,
            extension = "amr",
            sampleData = { createFakeAmrData() },
            category = ContentCategory.AUDIO
        ),
        ContentTypeInfo(
            mimeType = "audio/wav",
            extension = "wav",
            sampleData = { createFakeWavData() },
            category = ContentCategory.AUDIO
        ),
        ContentTypeInfo(
            mimeType = ContentType.AUDIO_OGG,
            extension = "ogg",
            sampleData = { createFakeOggData() },
            category = ContentCategory.AUDIO
        ),
        ContentTypeInfo(
            mimeType = ContentType.AUDIO_MIDI,
            extension = "mid",
            sampleData = { createFakeMidiData() },
            category = ContentCategory.AUDIO
        )
    )

    // ==================== Video Content Types ====================

    val VIDEO_TYPES = listOf(
        ContentTypeInfo(
            mimeType = ContentType.VIDEO_MP4,
            extension = "mp4",
            sampleData = { MmsPduFactory.createFakeVideoData() },
            category = ContentCategory.VIDEO
        ),
        ContentTypeInfo(
            mimeType = ContentType.VIDEO_3GPP,
            extension = "3gp",
            sampleData = { createFake3gppData() },
            category = ContentCategory.VIDEO
        ),
        ContentTypeInfo(
            mimeType = ContentType.VIDEO_H263,
            extension = "h263",
            sampleData = { createFakeH263Data() },
            category = ContentCategory.VIDEO
        )
    )

    // ==================== Text Content Types ====================

    val TEXT_TYPES = listOf(
        ContentTypeInfo(
            mimeType = ContentType.TEXT_PLAIN,
            extension = "txt",
            sampleData = { "Hello, this is a test message!".toByteArray() },
            category = ContentCategory.TEXT
        ),
        ContentTypeInfo(
            mimeType = ContentType.TEXT_HTML,
            extension = "html",
            sampleData = { "<html><body><p>Hello World</p></body></html>".toByteArray() },
            category = ContentCategory.TEXT
        ),
        ContentTypeInfo(
            mimeType = ContentType.TEXT_VCARD,
            extension = "vcf",
            sampleData = { MmsPduFactory.createVCardData() },
            category = ContentCategory.TEXT
        ),
        ContentTypeInfo(
            mimeType = ContentType.TEXT_VCALENDAR,
            extension = "vcs",
            sampleData = { createVCalendarData() },
            category = ContentCategory.TEXT
        )
    )

    // ==================== Application Content Types ====================

    val APPLICATION_TYPES = listOf(
        ContentTypeInfo(
            mimeType = ContentType.APP_SMIL,
            extension = "smil",
            sampleData = { createSmilData() },
            category = ContentCategory.APPLICATION
        ),
        ContentTypeInfo(
            mimeType = ContentType.MULTIPART_MIXED,
            extension = "",
            sampleData = { byteArrayOf() },
            category = ContentCategory.APPLICATION
        ),
        ContentTypeInfo(
            mimeType = ContentType.MULTIPART_RELATED,
            extension = "",
            sampleData = { byteArrayOf() },
            category = ContentCategory.APPLICATION
        ),
        ContentTypeInfo(
            mimeType = ContentType.MULTIPART_ALTERNATIVE,
            extension = "",
            sampleData = { byteArrayOf() },
            category = ContentCategory.APPLICATION
        )
    )

    // ==================== All Types Combined ====================

    val ALL_CONTENT_TYPES: List<ContentTypeInfo> by lazy {
        IMAGE_TYPES + AUDIO_TYPES + VIDEO_TYPES + TEXT_TYPES + APPLICATION_TYPES
    }

    val ALL_MEDIA_TYPES: List<ContentTypeInfo> by lazy {
        IMAGE_TYPES + AUDIO_TYPES + VIDEO_TYPES
    }

    // ==================== Test Data Generators ====================

    private fun createFakeGifData(): ByteArray {
        // GIF89a header + minimal image data
        return byteArrayOf(
            // GIF signature
            0x47, 0x49, 0x46, 0x38, 0x39, 0x61, // "GIF89a"
            // Logical screen descriptor
            0x01, 0x00, // Width = 1
            0x01, 0x00, // Height = 1
            0x00, // Packed byte (no global color table)
            0x00, // Background color index
            0x00, // Pixel aspect ratio
            // Image descriptor
            0x2C, // Image separator
            0x00, 0x00, // Left position
            0x00, 0x00, // Top position
            0x01, 0x00, // Width = 1
            0x01, 0x00, // Height = 1
            0x00, // Packed byte
            // Image data
            0x02, // LZW minimum code size
            0x02, // Block size
            0x44, 0x01, // Compressed data
            0x00, // Block terminator
            // Trailer
            0x3B
        )
    }

    private fun createFakeWbmpData(): ByteArray {
        return byteArrayOf(
            0x00, // Type = 0 (WBMP)
            0x00, // Fixed header
            0x01, // Width = 1
            0x01, // Height = 1
            0x00  // Pixel data (1 black pixel)
        )
    }

    private fun createFakeBmpData(): ByteArray {
        return byteArrayOf(
            // BMP header
            0x42, 0x4D, // "BM"
            0x3E, 0x00, 0x00, 0x00, // File size = 62
            0x00, 0x00, // Reserved
            0x00, 0x00, // Reserved
            0x36, 0x00, 0x00, 0x00, // Offset to pixel data = 54
            // DIB header
            0x28, 0x00, 0x00, 0x00, // Header size = 40
            0x01, 0x00, 0x00, 0x00, // Width = 1
            0x01, 0x00, 0x00, 0x00, // Height = 1
            0x01, 0x00, // Color planes = 1
            0x18, 0x00, // Bits per pixel = 24
            0x00, 0x00, 0x00, 0x00, // Compression = none
            0x04, 0x00, 0x00, 0x00, // Image size = 4
            0x00, 0x00, 0x00, 0x00, // X pixels per meter
            0x00, 0x00, 0x00, 0x00, // Y pixels per meter
            0x00, 0x00, 0x00, 0x00, // Colors in color table
            0x00, 0x00, 0x00, 0x00, // Important colors
            // Pixel data (BGR + padding)
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x00
        )
    }

    private fun createFakeMp3Data(): ByteArray {
        return byteArrayOf(
            // ID3v2 header
            0x49, 0x44, 0x33, // "ID3"
            0x04, 0x00, // Version 2.4.0
            0x00, // Flags
            0x00, 0x00, 0x00, 0x00, // Size = 0
            // MP3 frame sync
            0xFF.toByte(), 0xFB.toByte(), // Frame sync + MPEG Audio Layer 3
            0x90.toByte(), // Bitrate + sampling rate
            0x00 // Padding, private, channel, mode extension, copyright, original, emphasis
        )
    }

    private fun createFakeAacData(): ByteArray {
        return byteArrayOf(
            // ADTS header
            0xFF.toByte(), 0xF1.toByte(), // Sync word + ID + layer + protection
            0x50, // Profile + sampling frequency index + private bit + channel config (part)
            0x80.toByte(), // Channel config (part) + original + home + copyright id + copyright start
            0x00, 0x1F, // Frame length (part)
            0xFC.toByte() // Frame length (part) + buffer fullness (part)
        )
    }

    private fun createFakeAmrData(): ByteArray {
        return byteArrayOf(
            // AMR header
            0x23, 0x21, 0x41, 0x4D, 0x52, 0x0A, // "#!AMR\n"
            // AMR-NB frame (silence)
            0x3C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        )
    }

    private fun createFakeWavData(): ByteArray {
        return byteArrayOf(
            // RIFF header
            0x52, 0x49, 0x46, 0x46, // "RIFF"
            0x24, 0x00, 0x00, 0x00, // Chunk size
            0x57, 0x41, 0x56, 0x45, // "WAVE"
            // fmt chunk
            0x66, 0x6D, 0x74, 0x20, // "fmt "
            0x10, 0x00, 0x00, 0x00, // Chunk size = 16
            0x01, 0x00, // Audio format = PCM
            0x01, 0x00, // Channels = 1
            0x44, 0xAC.toByte(), 0x00, 0x00, // Sample rate = 44100
            0x88.toByte(), 0x58, 0x01, 0x00, // Byte rate
            0x02, 0x00, // Block align
            0x10, 0x00, // Bits per sample = 16
            // data chunk
            0x64, 0x61, 0x74, 0x61, // "data"
            0x00, 0x00, 0x00, 0x00  // Chunk size = 0
        )
    }

    private fun createFakeOggData(): ByteArray {
        return byteArrayOf(
            // Ogg page header
            0x4F, 0x67, 0x67, 0x53, // "OggS"
            0x00, // Version = 0
            0x02, // Header type flags (BOS)
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // Granule position
            0x00, 0x00, 0x00, 0x00, // Serial number
            0x00, 0x00, 0x00, 0x00, // Page sequence number
            0x00, 0x00, 0x00, 0x00, // Checksum
            0x01, // Page segments = 1
            0x00  // Segment table
        )
    }

    private fun createFakeMidiData(): ByteArray {
        return byteArrayOf(
            // MIDI header
            0x4D, 0x54, 0x68, 0x64, // "MThd"
            0x00, 0x00, 0x00, 0x06, // Header length = 6
            0x00, 0x00, // Format = 0
            0x00, 0x01, // Tracks = 1
            0x00, 0x60, // Division = 96 PPQN
            // Track header
            0x4D, 0x54, 0x72, 0x6B, // "MTrk"
            0x00, 0x00, 0x00, 0x04, // Track length = 4
            // End of track
            0x00, 0xFF.toByte(), 0x2F, 0x00
        )
    }

    private fun createFake3gppData(): ByteArray {
        // Same as createFakeVideoData but with 3gp brand
        return byteArrayOf(
            // ftyp box
            0x00, 0x00, 0x00, 0x14, // Size = 20
            0x66, 0x74, 0x79, 0x70, // "ftyp"
            0x33, 0x67, 0x70, 0x34, // "3gp4"
            0x00, 0x00, 0x00, 0x00, // Minor version
            0x33, 0x67, 0x70, 0x34  // Compatible brand "3gp4"
        )
    }

    private fun createFakeH263Data(): ByteArray {
        return byteArrayOf(
            // H.263 picture start code
            0x00, 0x00, 0x80.toByte(), // Picture start code
            0x02, // Temporal reference + split screen + document camera + freeze picture release
            0x00, // Source format + optional picture format
            0x00, 0x00 // Padding
        )
    }

    private fun createVCalendarData(): ByteArray {
        return """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            DTSTART:20250101T120000Z
            DTEND:20250101T130000Z
            SUMMARY:Test Event
            END:VEVENT
            END:VCALENDAR
        """.trimIndent().toByteArray()
    }

    private fun createSmilData(): ByteArray {
        return """
            <smil>
                <head>
                    <layout>
                        <root-layout width="100%" height="100%"/>
                        <region id="Image" top="0" left="0" height="80%" width="100%" fit="meet"/>
                        <region id="Text" top="80%" left="0" height="20%" width="100%"/>
                    </layout>
                </head>
                <body>
                    <par dur="5000ms">
                        <text src="text.txt" region="Text"/>
                    </par>
                </body>
            </smil>
        """.trimIndent().toByteArray()
    }

    // ==================== Content Type Validation ====================

    /**
     * Returns true if the given MIME type is a supported image type.
     */
    fun isImageType(mimeType: String): Boolean {
        return IMAGE_TYPES.any { it.mimeType.equals(mimeType, ignoreCase = true) }
    }

    /**
     * Returns true if the given MIME type is a supported audio type.
     */
    fun isAudioType(mimeType: String): Boolean {
        return AUDIO_TYPES.any { it.mimeType.equals(mimeType, ignoreCase = true) }
    }

    /**
     * Returns true if the given MIME type is a supported video type.
     */
    fun isVideoType(mimeType: String): Boolean {
        return VIDEO_TYPES.any { it.mimeType.equals(mimeType, ignoreCase = true) }
    }

    /**
     * Returns true if the given MIME type is a supported text type.
     */
    fun isTextType(mimeType: String): Boolean {
        return TEXT_TYPES.any { it.mimeType.equals(mimeType, ignoreCase = true) }
    }

    /**
     * Returns the appropriate file extension for a given MIME type.
     */
    fun getExtension(mimeType: String): String? {
        return ALL_CONTENT_TYPES.find { it.mimeType.equals(mimeType, ignoreCase = true) }?.extension
    }

    /**
     * Returns sample data for a given MIME type.
     */
    fun getSampleData(mimeType: String): ByteArray? {
        return ALL_CONTENT_TYPES.find { it.mimeType.equals(mimeType, ignoreCase = true) }?.sampleData?.invoke()
    }
}
