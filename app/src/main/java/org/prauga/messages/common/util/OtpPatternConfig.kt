/*
 * Copyright (C) 2026 Vishnu R <vishnurajesh45@gmail.com>
 */

package org.prauga.messages.common.util

/**
 * Configuration object containing regex patterns for OTP detection.
 * 
 * These patterns are used to identify and extract OTP codes from SMS messages.
 * All patterns are documented to explain their purpose and matching behavior.
 */
object OtpPatternConfig {
    /**
     * Matches sequences of whitespace characters.
     * 
     * Used to normalize message text before processing by collapsing
     * multiple spaces, tabs, and newlines into single spaces.
     * 
     * Example matches: " ", "  ", "\t", "\n", "  \n  "
     */
    const val WHITESPACE_REGEX = "\\s+"
    
    /**
     * Matches numeric codes between 3-10 digits.
     * 
     * This is the primary pattern for detecting standard OTP codes.
     * Word boundaries ensure we don't match parts of longer numbers.
     * 
     * Example matches: "123", "456789", "1234567890"
     * Does not match: "12" (too short), "12345678901" (too long)
     */
    const val NUMERIC_REGEX = "\\b\\d{3,10}\\b"
    
    /**
     * Matches spaced or hyphenated numeric codes.
     * 
     * Some OTP messages format codes with spaces or hyphens for readability.
     * This pattern captures codes split into 2-4 digit groups.
     * 
     * Example matches: "12-34-56", "12 34 56", "1234-5678", "12 34 56 78"
     * Does not match: "1-2" (groups too small)
     */
    const val SPACED_NUMERIC_REGEX = "\\b\\d{2,4}([\\s-]\\d{2,4})+\\b"
    
    /**
     * Matches alphanumeric codes between 4-10 characters.
     * 
     * Some services use alphanumeric OTP codes mixing letters and numbers.
     * This pattern captures codes containing both uppercase/lowercase letters and digits.
     * 
     * Example matches: "A1B2C3", "XYZ123", "abc123def", "1A2B3C4D5E"
     * Does not match: "ABC" (no numbers), "123" (no letters), "A1B" (too short)
     */
    const val ALPHANUMERIC_REGEX = "\\b[0-9A-Za-z]{4,10}\\b"
    
    /**
     * Pattern to identify lines containing OTP-related keywords.
     * 
     * Used for line-by-line message analysis to focus on relevant sections.
     * Matches common terms that indicate an OTP code is present.
     * 
     * Example matches: "Your OTP is", "Enter code:", "password: 123456", "passcode 456"
     */
    const val OTP_LINE_PATTERN = "(otp|code|password|passcode)"
}
