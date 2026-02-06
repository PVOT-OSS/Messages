/*
 * Copyright (C) 2026 Vishnu R <vishnurajesh45@gmail.com>
 */

package org.prauga.messages.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for FileNamingConfig.
 * Keeping this in here to avoid introducing testing libs to common utils - can be refactored at a later point.
 * 
 * These tests verify that all file naming constants are properly defined
 * and follow expected naming conventions.
 */
class FileNamingConfigTest {

    // Constant Definition Tests
    @Test
    fun savedMessageTextPrefix_shouldBeNonEmpty() {
        val prefix = FileNamingConfig.SAVED_MESSAGE_TEXT_PREFIX
        
        assertFalse("Saved message text prefix should not be empty", prefix.isEmpty())
    }

    @Test
    fun audioFilePrefix_shouldBeNonEmpty() {
        val prefix = FileNamingConfig.AUDIO_FILE_PREFIX
        
        assertFalse("Audio file prefix should not be empty", prefix.isEmpty())
    }

    @Test
    fun audioFileSuffix_shouldBeNonEmpty() {
        val suffix = FileNamingConfig.AUDIO_FILE_SUFFIX
        
        assertFalse("Audio file suffix should not be empty", suffix.isEmpty())
    }

    @Test
    fun defaultGalleryShareFilename_shouldBeNonEmpty() {
        val filename = FileNamingConfig.DEFAULT_GALLERY_SHARE_FILENAME
        
        assertFalse("Default gallery share filename should not be empty", filename.isEmpty())
    }

    @Test
    fun defaultAudioShareFilename_shouldBeNonEmpty() {
        val filename = FileNamingConfig.DEFAULT_AUDIO_SHARE_FILENAME
        
        assertFalse("Default audio share filename should not be empty", filename.isEmpty())
    }

    // Expected Value Tests
    @Test
    fun savedMessageTextPrefix_shouldHaveExpectedValue() {
        val prefix = FileNamingConfig.SAVED_MESSAGE_TEXT_PREFIX
        
        assertEquals("Saved message text prefix should be 'QuikSmsText-'", 
            "QuikSmsText-", prefix)
    }

    @Test
    fun audioFilePrefix_shouldHaveExpectedValue() {
        val prefix = FileNamingConfig.AUDIO_FILE_PREFIX
        
        assertEquals("Audio file prefix should be 'recorded-'", 
            "recorded-", prefix)
    }

    @Test
    fun audioFileSuffix_shouldHaveExpectedValue() {
        val suffix = FileNamingConfig.AUDIO_FILE_SUFFIX
        
        assertEquals("Audio file suffix should be '.3ga'", 
            ".3ga", suffix)
    }

    @Test
    fun defaultGalleryShareFilename_shouldHaveExpectedValue() {
        val filename = FileNamingConfig.DEFAULT_GALLERY_SHARE_FILENAME
        
        assertEquals("Default gallery share filename should be 'quik-media-attachment.jpg'",
            "quik-media-attachment.jpg", filename)
    }

    @Test
    fun defaultAudioShareFilename_shouldHaveExpectedValue() {
        val filename = FileNamingConfig.DEFAULT_AUDIO_SHARE_FILENAME
        
        assertEquals("Default audio share filename should be 'quik-audio-attachment.mp3'",
            "quik-audio-attachment.mp3", filename)
    }

    // Format Validation Tests
    @Test
    fun prefixes_shouldEndWithHyphen() {
        val savedMessagePrefix = FileNamingConfig.SAVED_MESSAGE_TEXT_PREFIX
        val audioPrefix = FileNamingConfig.AUDIO_FILE_PREFIX
        
        assertTrue("Saved message prefix should end with hyphen", 
            savedMessagePrefix.endsWith("-"))
        assertTrue("Audio prefix should end with hyphen", 
            audioPrefix.endsWith("-"))
    }

    @Test
    fun audioFileSuffix_shouldStartWithDot() {
        val suffix = FileNamingConfig.AUDIO_FILE_SUFFIX
        
        assertTrue("Audio file suffix should start with dot", suffix.startsWith("."))
    }

    @Test
    fun audioFileSuffix_shouldBe3gaFormat() {
        val suffix = FileNamingConfig.AUDIO_FILE_SUFFIX
        
        assertEquals("Audio file suffix should be .3ga format", ".3ga", suffix)
    }

    @Test
    fun defaultShareFilenames_shouldHaveFileExtensions() {
        val galleryFilename = FileNamingConfig.DEFAULT_GALLERY_SHARE_FILENAME
        val audioFilename = FileNamingConfig.DEFAULT_AUDIO_SHARE_FILENAME
        
        assertTrue("Gallery share filename should have extension", 
            galleryFilename.contains("."))
        assertTrue("Audio share filename should have extension", 
            audioFilename.contains("."))
    }

    @Test
    fun defaultGalleryShareFilename_shouldBeJpgFormat() {
        val filename = FileNamingConfig.DEFAULT_GALLERY_SHARE_FILENAME
        
        assertTrue("Gallery share filename should end with .jpg", 
            filename.endsWith(".jpg"))
    }

    @Test
    fun defaultAudioShareFilename_shouldBeMp3Format() {
        val filename = FileNamingConfig.DEFAULT_AUDIO_SHARE_FILENAME
        
        assertTrue("Audio share filename should end with .mp3", 
            filename.endsWith(".mp3"))
    }

    // Naming Convention Tests
    @Test
    fun allConstants_shouldUseLowercaseOrKebabCase() {
        val audioPrefix = FileNamingConfig.AUDIO_FILE_PREFIX
        val audioSuffix = FileNamingConfig.AUDIO_FILE_SUFFIX
        val galleryFilename = FileNamingConfig.DEFAULT_GALLERY_SHARE_FILENAME
        val audioFilename = FileNamingConfig.DEFAULT_AUDIO_SHARE_FILENAME
        
        // Check that filenames don't contain uppercase (except for QuikSmsText which is a brand name)
        assertTrue("Audio prefix should be lowercase", 
            audioPrefix == audioPrefix.lowercase())
        assertTrue("Audio suffix should be lowercase", 
            audioSuffix == audioSuffix.lowercase())
        assertTrue("Gallery filename should be lowercase", 
            galleryFilename == galleryFilename.lowercase())
        assertTrue("Audio filename should be lowercase", 
            audioFilename == audioFilename.lowercase())
    }

    @Test
    fun filenamePrefixes_shouldNotContainSpaces() {
        val savedMessagePrefix = FileNamingConfig.SAVED_MESSAGE_TEXT_PREFIX
        val audioPrefix = FileNamingConfig.AUDIO_FILE_PREFIX
        
        assertFalse("Saved message prefix should not contain spaces", 
            savedMessagePrefix.contains(" "))
        assertFalse("Audio prefix should not contain spaces", 
            audioPrefix.contains(" "))
    }

    @Test
    fun defaultFilenames_shouldNotContainSpaces() {
        val galleryFilename = FileNamingConfig.DEFAULT_GALLERY_SHARE_FILENAME
        val audioFilename = FileNamingConfig.DEFAULT_AUDIO_SHARE_FILENAME
        
        assertFalse("Gallery filename should not contain spaces", 
            galleryFilename.contains(" "))
        assertFalse("Audio filename should not contain spaces", 
            audioFilename.contains(" "))
    }

    // File Path Safety Tests
    @Test
    fun allConstants_shouldNotContainPathSeparators() {
        val constants = listOf(
            FileNamingConfig.SAVED_MESSAGE_TEXT_PREFIX,
            FileNamingConfig.AUDIO_FILE_PREFIX,
            FileNamingConfig.AUDIO_FILE_SUFFIX,
            FileNamingConfig.DEFAULT_GALLERY_SHARE_FILENAME,
            FileNamingConfig.DEFAULT_AUDIO_SHARE_FILENAME
        )
        
        constants.forEach { constant ->
            assertFalse("Constant should not contain forward slash", 
                constant.contains("/"))
            assertFalse("Constant should not contain backslash", 
                constant.contains("\\"))
        }
    }

    @Test
    fun allConstants_shouldNotContainInvalidFilenameCharacters() {
        val constants = listOf(
            FileNamingConfig.SAVED_MESSAGE_TEXT_PREFIX,
            FileNamingConfig.AUDIO_FILE_PREFIX,
            FileNamingConfig.AUDIO_FILE_SUFFIX,
            FileNamingConfig.DEFAULT_GALLERY_SHARE_FILENAME,
            FileNamingConfig.DEFAULT_AUDIO_SHARE_FILENAME
        )
        
        val invalidChars = listOf('<', '>', ':', '"', '|', '?', '*')
        
        constants.forEach { constant ->
            invalidChars.forEach { invalidChar ->
                assertFalse(
                    "Constant should not contain invalid character '$invalidChar'",
                    constant.contains(invalidChar)
                )
            }
        }
    }
}
