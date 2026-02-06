/*
 * Copyright (C) 2026 Vishnu R <vishnurajesh45@gmail.com>
 */

package org.prauga.messages.common.util

import android.content.Context
import android.content.res.Resources
import org.prauga.messages.app.R
import timber.log.Timber

/**
 * Implementation of [OtpResourceProvider] that loads resources from Android resource files.
 * 
 * This class handles:
 * - Loading string arrays from resources with automatic locale support
 * - Normalizing all keywords to lowercase for case-insensitive matching
 * - Fallback to default English keywords if resources are missing
 * - Error message retrieval with locale support
 * 
 * @param context Android context for accessing resources
 */
class OtpResourceProviderImpl(private val context: Context) : OtpResourceProvider {
    
    companion object {
        // Default fallback keywords in case resources are missing
        private val DEFAULT_OTP_KEYWORDS = listOf(
            "otp", "one time password", "one-time password", "verification code",
            "verification number", "login code", "login otp", "security code",
            "2-step code", "2 factor code", "2fa code", "mfa code", "auth code",
            "passcode", "access code", "reset code", "transaction code",
            "confirm code", "confirmation code", "code"
        )
        
        private val DEFAULT_SAFETY_KEYWORDS = listOf(
            "do not share", "don't share", "never share", "do not disclose",
            "do not forward", "keep this code secret", "valid for",
            "expires in", "expires within", "expires after"
        )
        
        private val DEFAULT_MONEY_INDICATORS = listOf(
            "rs", "inr", "usd", "eur", "gbp", "₹", "$", "€", "£", "balance",
            "amount", "debited", "credited", "txn", "transaction id", "order id"
        )
    }
    
    override fun getOtpKeywords(): List<String> {
        return try {
            context.resources.getStringArray(R.array.otp_keywords)
                .map { it.lowercase() }
        } catch (e: Resources.NotFoundException) {
            Timber.w(e, "OTP keywords resource not found, using defaults")
            DEFAULT_OTP_KEYWORDS
        }
    }
    
    override fun getSafetyKeywords(): List<String> {
        return try {
            context.resources.getStringArray(R.array.otp_safety_keywords)
                .map { it.lowercase() }
        } catch (e: Resources.NotFoundException) {
            Timber.w(e, "OTP safety keywords resource not found, using defaults")
            DEFAULT_SAFETY_KEYWORDS
        }
    }
    
    override fun getMoneyIndicators(): List<String> {
        return try {
            context.resources.getStringArray(R.array.otp_money_indicators)
                .map { it.lowercase() }
        } catch (e: Resources.NotFoundException) {
            Timber.w(e, "OTP money indicators resource not found, using defaults")
            DEFAULT_MONEY_INDICATORS
        }
    }
    
    override fun getErrorMessage(errorType: OtpErrorType): String {
        val resId = when (errorType) {
            OtpErrorType.EMPTY_MESSAGE -> R.string.otp_error_empty_message
            OtpErrorType.NO_OTP_KEYWORD -> R.string.otp_error_no_keyword
            OtpErrorType.KEYWORD_BUT_NO_CODE -> R.string.otp_error_keyword_no_code
        }
        
        return try {
            context.getString(resId)
        } catch (e: Resources.NotFoundException) {
            Timber.w(e, "OTP error message resource not found for type: $errorType")
            // Fallback to English error messages
            when (errorType) {
                OtpErrorType.EMPTY_MESSAGE -> "Empty message"
                OtpErrorType.NO_OTP_KEYWORD -> "No OTP-like keywords and no candidate code found"
                OtpErrorType.KEYWORD_BUT_NO_CODE -> "Contains OTP-like keywords but no numeric/alphanumeric candidate code found"
            }
        }
    }
}
