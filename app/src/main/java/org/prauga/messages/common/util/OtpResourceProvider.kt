/*
 * Copyright (C) 2026 Vishnu R <vishnurajesh45@gmail.com>
 */

package org.prauga.messages.common.util

/**
 * Provides OTP-related resources with locale support.
 * 
 * This interface abstracts the loading of OTP keywords, safety keywords,
 * money indicators, and error messages from Android resources, enabling
 * automatic locale-based translations.
 */
interface OtpResourceProvider {
    /**
     * Returns the list of OTP keywords for the current locale.
     * Keywords are normalized to lowercase for case-insensitive matching.
     * 
     * @return List of OTP-related keywords (e.g., "otp", "verification code")
     */
    fun getOtpKeywords(): List<String>
    
    /**
     * Returns the list of safety keywords for the current locale.
     * These are phrases that indicate security warnings in OTP messages.
     * Keywords are normalized to lowercase for case-insensitive matching.
     * 
     * @return List of safety-related keywords (e.g., "do not share", "expires in")
     */
    fun getSafetyKeywords(): List<String>
    
    /**
     * Returns the list of money indicators for the current locale.
     * These terms help identify financial transaction messages.
     * Keywords are normalized to lowercase for case-insensitive matching.
     * 
     * @return List of money-related terms (e.g., "rs", "balance", "debited")
     */
    fun getMoneyIndicators(): List<String>
    
    /**
     * Returns a localized error message for the specified error type.
     * 
     * @param errorType The type of OTP detection error
     * @return Localized error message string
     */
    fun getErrorMessage(errorType: OtpErrorType): String
}

/**
 * Types of errors that can occur during OTP detection.
 */
enum class OtpErrorType {
    /** The message text is empty */
    EMPTY_MESSAGE,
    
    /** No OTP-like keywords and no candidate code found */
    NO_OTP_KEYWORD,
    
    /** Contains OTP-like keywords but no numeric/alphanumeric candidate code found */
    KEYWORD_BUT_NO_CODE
}
