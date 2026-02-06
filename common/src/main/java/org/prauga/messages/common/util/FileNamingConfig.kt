/*
 * Copyright (C) 2026 Vishnu R <vishnurajesh45@gmail.com>
 */

package org.prauga.messages.common.util

/**
 * Configuration object containing file naming conventions.
 * 
 * These constants define prefixes, suffixes, and default filenames used
 * throughout the app for consistent file naming.
 */
object FileNamingConfig {
    /**
     * Prefix for saved message text files.
     * 
     * Used when: Exporting message text to files
     * Format: QuikSmsText-{timestamp}.txt
     */
    const val SAVED_MESSAGE_TEXT_PREFIX = "QuikSmsText-"
    
    /**
     * Prefix for recorded audio files.
     * 
     * Used when: Recording voice messages
     * Format: recorded-{timestamp}.3ga
     */
    const val AUDIO_FILE_PREFIX = "recorded-"
    
    /**
     * File extension for audio recordings.
     * 
     * Used when: Saving recorded voice messages
     * Format: .3ga (3GPP audio format)
     */
    const val AUDIO_FILE_SUFFIX = ".3ga"
    
    /**
     * Default filename for shared gallery images.
     * 
     * Used when: Sharing images from the gallery view
     * Format: quik-media-attachment.jpg
     */
    const val DEFAULT_GALLERY_SHARE_FILENAME = "quik-media-attachment.jpg"
    
    /**
     * Default filename for shared audio files.
     * 
     * Used when: Sharing audio attachments
     * Format: quik-audio-attachment.mp3
     */
    const val DEFAULT_AUDIO_SHARE_FILENAME = "quik-audio-attachment.mp3"
}
