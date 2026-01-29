/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */

package org.prauga.messages.common.util

internal object OtpConstants {
    // OTP Keywords
    val OTP_KEYWORDS = listOf(
        "otp",
        "one time password",
        "one-time password",
        "verification code",
        "verification number",
        "login code",
        "login otp",
        "security code",
        "2-step code",
        "2 factor code",
        "2fa code",
        "mfa code",
        "auth code",
        "passcode",
        "access code",
        "reset code",
        "transaction code",
        "confirm code",
        "confirmation code",
        "code"
    ).map { it.lowercase() }

    // Safety Keywords
    val SAFETY_KEYWORDS = listOf(
        "do not share",
        "don't share",
        "never share",
        "do not disclose",
        "do not forward",
        "keep this code secret",
        "valid for",
        "expires in",
        "expires within",
        "expires after"
    ).map { it.lowercase() }

    // Money Indicators
    val MONEY_INDICATORS = listOf(
        "rs", "inr", "usd", "eur", "gbp", "₹", "$", "€", "£", "balance",
        "amount", "debited", "credited", "txn", "transaction id", "order id"
    ).map { it.lowercase() }

    // Error Messages
    const val EMPTY_MESSAGE = "Empty message"
    const val NO_OTP_KEYWORD_MSG = "No OTP-like keywords and no candidate code found"
    const val KEYWORD_BUT_NO_CODE_MSG = "Contains OTP-like keywords but no numeric/alphanumeric candidate code found"

    // Regex Patterns
    const val WHITESPACE_REGEX = "\\s+"
    const val NUMERIC_REGEX = "\\b\\d{3,10}\\b"
    const val SPACED_NUMERIC_REGEX = "\\b\\d{2,4}([\\s-]\\d{2,4})+\\b"
    const val ALPHANUMERIC_REGEX = "\\b[0-9A-Za-z]{4,10}\\b"
    const val OTP_LINE_PATTERN = "(otp|code|password|passcode)"
}
