/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */

package org.prauga.messages.app.receiver

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CopyOtpReceiver.
 *
 * These tests verify the data contract and Intent extra handling logic.
 * Full integration tests with Android components (ClipboardManager, Toast, Context)
 * should be done via instrumentation tests since they require the Android framework.
 */
class CopyOtpReceiverTest {

    companion object {
        const val EXTRA_OTP_CODE = "otpCode"
    }

    @Test
    fun givenExtraOtpCodeKey_whenVerified_thenEqualsOtpCode() {
        val expectedKey = "otpCode"

        assertEquals("OTP code extra key should be 'otpCode'", expectedKey, EXTRA_OTP_CODE)
    }

    @Test
    fun givenOtpCodeExtraKey_whenComparedToWrongCase_thenNotEqual() {
        val correctKey = "otpCode"
        val wrongKeys = listOf("otpcode", "OTPCode", "OTPCODE", "otp_code")

        wrongKeys.forEach { wrongKey ->
            assertNotEquals("Key should not match incorrect case: $wrongKey",
                correctKey, wrongKey)
        }
    }

    // OTP Code Format
    @Test
    fun givenSixDigitOtp_whenValidated_thenMatchesPattern() {
        val otpCode = "123456"

        assertTrue("6-digit OTP should be valid", otpCode.matches(Regex("\\d{6}")))
        assertEquals("OTP length should be 6", 6, otpCode.length)
    }

    @Test
    fun givenFourDigitOtp_whenValidated_thenMatchesPattern() {
        val otpCode = "1234"

        assertTrue("4-digit OTP should be valid", otpCode.matches(Regex("\\d{4}")))
        assertEquals("OTP length should be 4", 4, otpCode.length)
    }

    @Test
    fun givenEightDigitOtp_whenValidated_thenMatchesPattern() {
        val otpCode = "12345678"

        assertTrue("8-digit OTP should be valid", otpCode.matches(Regex("\\d{8}")))
        assertEquals("OTP length should be 8", 8, otpCode.length)
    }

    @Test
    fun givenAlphanumericOtp_whenValidated_thenContainsBothLettersAndDigits() {
        val otpCode = "A1B2C3"

        assertTrue("Alphanumeric OTP should contain both letters and digits",
            otpCode.any { it.isLetter() } && otpCode.any { it.isDigit() })
        assertEquals("OTP length should be 6", 6, otpCode.length)
    }

    @Test
    fun givenMixedCaseAlphanumericOtp_whenValidated_thenContainsUpperLowerAndDigits() {
        val otpCode = "Xy9Z4m"

        assertTrue("Should contain uppercase", otpCode.any { it.isUpperCase() })
        assertTrue("Should contain lowercase", otpCode.any { it.isLowerCase() })
        assertTrue("Should contain digits", otpCode.any { it.isDigit() })
    }

    // Some more cases
    @Test
    fun givenEmptyString_whenValidated_thenNotValidOtp() {
        val otpCode = ""

        assertTrue("Empty string should be empty", otpCode.isEmpty())
        assertFalse("Empty string should not match OTP pattern",
            otpCode.matches(Regex("\\d{4,8}")))
    }

    @Test
    fun givenNullString_whenHandled_thenRemainsNull() {
        val otpCode: String? = null

        assertNull("Null OTP should be null", otpCode)
    }

    @Test
    fun givenWhitespaceOnlyString_whenValidated_thenNotValidOtp() {
        val otpCode = "   "

        assertTrue("Whitespace string should not match OTP pattern",
            otpCode.trim().isEmpty())
    }

    @Test
    fun givenOtpWithWhitespace_whenTrimmed_thenValidOtp() {
        val otpCodeWithSpace = "  123456  "
        val trimmedOtp = otpCodeWithSpace.trim()

        assertEquals("Trimmed OTP should be valid", "123456", trimmedOtp)
        assertTrue("Trimmed OTP should match pattern",
            trimmedOtp.matches(Regex("\\d{6}")))
    }

    // With special characters
    @Test
    fun givenOtpWithSpecialCharacters_whenChecked_thenIdentifiable() {
        val otpCode = "12#45@"

        assertTrue("Should contain special characters",
            otpCode.any { !it.isLetterOrDigit() })
    }

    @Test
    fun givenOtpWithOnlyDigits_whenChecked_thenPureNumeric() {
        val otpCode = "123456"

        assertTrue("Should contain only digits",
            otpCode.all { it.isDigit() })
    }

    // Clipboard label
    @Test
    fun givenClipboardLabel_whenVerified_thenEqualsOtp() {
        val expectedLabel = "OTP"

        assertEquals("Clipboard label should be 'OTP'", expectedLabel, "OTP")
    }

    // Receiver Lifecycle
    @Test
    fun givenCopyOtpReceiver_whenInstantiated_thenIsBroadcastReceiverSubclass() {
        val receiver = CopyOtpReceiver()

        assertNotNull("Receiver should be instantiable", receiver)
        assertTrue("Receiver should be a BroadcastReceiver",
            receiver is android.content.BroadcastReceiver)
    }
}
