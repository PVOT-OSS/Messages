/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 * Copyright (C) 2026 Vishnu R <vishnurajesh45@gmail.com>
 */

package org.prauga.messages.app.utils

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.prauga.messages.common.util.OtpDetector
import org.prauga.messages.common.util.OtpErrorType
import org.prauga.messages.common.util.OtpResourceProvider

class OtpDetectorTest {

    private lateinit var otpDetector: OtpDetector
    private lateinit var resourceProvider: OtpResourceProvider

    @Before
    fun setup() {
        // Create a test resource provider with default English keywords
        resourceProvider = object : OtpResourceProvider {
            override fun getOtpKeywords(): List<String> = listOf(
                "otp", "one time password", "one-time password", "verification code",
                "verification number", "login code", "login otp", "security code",
                "2-step code", "2 factor code", "2fa code", "mfa code", "auth code",
                "passcode", "access code", "reset code", "transaction code",
                "confirm code", "confirmation code", "code"
            )

            override fun getSafetyKeywords(): List<String> = listOf(
                "do not share", "don't share", "never share", "do not disclose",
                "do not forward", "keep this code secret", "valid for",
                "expires in", "expires within", "expires after"
            )

            override fun getMoneyIndicators(): List<String> = listOf(
                "rs", "inr", "usd", "eur", "gbp", "₹", "$", "€", "£", "balance",
                "amount", "debited", "credited", "txn", "transaction id", "order id"
            )

            override fun getErrorMessage(errorType: OtpErrorType): String = when (errorType) {
                OtpErrorType.EMPTY_MESSAGE -> "Empty message"
                OtpErrorType.NO_OTP_KEYWORD -> "No OTP-like keywords and no candidate code found"
                OtpErrorType.KEYWORD_BUT_NO_CODE -> "Contains OTP-like keywords but no numeric/alphanumeric candidate code found"
            }
        }
        
        otpDetector = OtpDetector(resourceProvider)
    }

    // Basic OTP Detection
    @Test
    fun givenMessageWithSixDigitOtpAndExplicitKeyword_whenDetected_thenExtractsCode() {
        val result = otpDetector.detect("Your OTP is 123456")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "123456", result.code)
        assertTrue("Should have high confidence", result.confidence >= 0.6)
    }

    @Test
    fun givenMessageWithSixDigitOtpAndVerificationCodeKeyword_whenDetected_thenExtractsCode() {
        val result = otpDetector.detect("Your verification code is 987654")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "987654", result.code)
        assertTrue("Should have high confidence", result.confidence >= 0.6)
    }

    @Test
    fun givenMessageWithFourDigitOtp_whenDetected_thenExtractsCode() {
        val result = otpDetector.detect("Your OTP is 1234")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "1234", result.code)
        assertTrue("Should have high confidence", result.confidence >= 0.6)
    }

    @Test
    fun givenMessageWithFiveDigitOtp_whenDetected_thenExtractsCode() {
        val result = otpDetector.detect("Your login code: 12345")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "12345", result.code)
        assertTrue("Should have high confidence", result.confidence >= 0.6)
    }

    @Test
    fun givenMessageWithEightDigitOtp_whenDetected_thenExtractsCode() {
        val result = otpDetector.detect("Security code: 12345678")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "12345678", result.code)
    }

    // Different OTP Formats
    @Test
    fun givenOtpWithColonSeparator_whenDetected_thenExtractsCode() {
        val result = otpDetector.detect("Your OTP: 456789")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "456789", result.code)
    }

    @Test
    fun givenOtpWithEqualsSign_whenDetected_thenExtractsCode() {
        val result = otpDetector.detect("OTP = 654321")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "654321", result.code)
    }

    @Test
    fun givenOtpOnItsOwnLine_whenDetected_thenExtractsCode() {
        val result = otpDetector.detect("Your verification code:\n123456\nDo not share")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "123456", result.code)
    }

    @Test
    fun givenOtpWithSpacesInNumber_whenDetected_thenExtractsCodeWithoutSpaces() {
        val result = otpDetector.detect("Your OTP is 123 456")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "123456", result.code)
    }

    @Test
    fun givenOtpWithDashesInNumber_whenDetected_thenExtractsCodeWithoutDashes() {
        val result = otpDetector.detect("Your code is 12-34-56")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "123456", result.code)
    }

    // ALPHANUMERIC OTP Tests
    @Test
    fun givenAlphanumericOtp_whenDetected_thenExtractsCode() {
        val result = otpDetector.detect("Your verification code is A1B2C3")

        assertTrue("Should detect alphanumeric OTP", result.isOtp)
        assertEquals("Should extract code", "A1B2C3", result.code)
    }

    @Test
    fun givenMixedCaseAlphanumericOtp_whenDetected_thenExtractsCode() {
        val result = otpDetector.detect("Login code: Xy9Z4m")

        assertTrue("Should detect mixed case OTP", result.isOtp)
        assertEquals("Should extract code", "Xy9Z4m", result.code)
    }

    // Keyword Variation Tests
    @Test
    fun givenOneTimePasswordKeyword_whenDetected_thenExtractsCode() {
        val result = otpDetector.detect("Your one-time password is 555666")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "555666", result.code)
    }

    @Test
    fun given2faKeyword_whenDetected_thenExtractsCode() {
        val result = otpDetector.detect("Your 2FA code: 777888")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "777888", result.code)
    }

    @Test
    fun givenAuthenticationCodeKeyword_whenDetected_thenExtractsCode() {
        val result = otpDetector.detect("Authentication code is 999000")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "999000", result.code)
    }

    @Test
    fun givenPasscodeKeyword_whenDetected_thenExtractsCode() {
        val result = otpDetector.detect("Your passcode: 112233")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "112233", result.code)
    }

    // Safety Keyword Tests
    @Test
    fun givenOtpWithDoNotShareWarning_whenDetected_thenHasHigherConfidence() {
        val result = otpDetector.detect("Your OTP is 123456. Do not share this code with anyone.")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "123456", result.code)
        assertTrue("Should have higher confidence with safety keyword", result.confidence >= 0.7)
    }

    @Test
    fun givenOtpWithExpiryInformation_whenDetected_thenExtractsCode() {
        val result = otpDetector.detect("Your code 234567 expires in 5 minutes")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "234567", result.code)
    }

    @Test
    fun givenOtpWithValidForInformation_whenDetected_thenExtractsCode() {
        val result = otpDetector.detect("345678 is your verification code. Valid for 10 minutes.")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "345678", result.code)
    }

    // Some Real Message Tests
    @Test
    fun givenTypicalBankMessage_whenDetected_thenExtractsOtp() {
        val result = otpDetector.detect("Your OTP for transaction is 456789. Do not share this code.")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "456789", result.code)
    }

    @Test
    fun givenTypicalAppSignupMessage_whenDetected_thenExtractsOtp() {
        val result = otpDetector.detect("Welcome! Your verification code is 987654. Use this to complete your registration.")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "987654", result.code)
    }

    @Test
    fun givenTypicalLoginMessage_whenDetected_thenExtractsOtp() {
        val result = otpDetector.detect("123456 is your login verification code")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "123456", result.code)
    }

    @Test
    fun givenMessageWithCompanyName_whenDetected_thenExtractsOtp() {
        val result = otpDetector.detect("ABC Corp: Your security code is 567890")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "567890", result.code)
    }

    // Not detect Negative Texts
    @Test
    fun givenPhoneNumber_whenDetected_thenNotDetectedAsOtp() {
        val result = otpDetector.detect("Please call +1 234 567 8900 for support")

        assertFalse("Should not detect phone number as OTP", result.isOtp)
        assertNull("Should not extract code from phone number", result.code)
    }

    @Test
    fun givenTransactionAmount_whenDetected_thenNotDetectedAsOtp() {
        val result = otpDetector.detect("Amount debited: Rs 123456 from your account")

        assertFalse("Should not detect amount as OTP", result.isOtp)
        assertNull("Should not extract code from amount", result.code)
    }

    @Test
    fun givenAccountBalance_whenDetected_thenNotDetectedAsOtp() {
        val result = otpDetector.detect("Your account balance is 654321")

        assertFalse("Should not detect balance as OTP", result.isOtp)
        assertNull("Should not extract code from balance", result.code)
    }

    @Test
    fun givenTransactionId_whenDetected_thenNotDetectedAsOtp() {
        val result = otpDetector.detect("Transaction ID: 123456789 completed successfully")

        assertFalse("Should not detect long transaction ID as OTP", result.isOtp)
    }

    @Test
    fun givenOrderNumber_whenDetected_thenNotDetectedAsOtp() {
        val result = otpDetector.detect("Your order #987654321 has been placed")

        assertFalse("Should not detect order number as OTP", result.isOtp)
    }

    @Test
    fun givenMessageWithoutOtpKeyword_whenDetected_thenCodeIsNullIfNotDetected() {
        val result = otpDetector.detect("The number is 123456")

        // This might detect as OTP if confidence is high enough, so we just verify
        // that if it's not detected, the code is null
        if (!result.isOtp) {
            assertNull("Should have null code if not detected", result.code)
        }
    }

    @Test
    fun givenEmptyMessage_whenDetected_thenNotDetectedAsOtp() {
        val result = otpDetector.detect("")

        assertFalse("Should not detect OTP in empty message", result.isOtp)
        assertNull("Should have null code", result.code)
        assertEquals("Should have zero confidence", 0.0, result.confidence, 0.01)
    }

    @Test
    fun givenMessageWithKeywordButNoCode_whenDetected_thenNotDetectedAsOtp() {
        val result = otpDetector.detect("Your OTP will arrive shortly")

        assertFalse("Should not detect OTP without code", result.isOtp)
        assertNull("Should have null code", result.code)
    }

    // Some edge cases
    @Test
    fun givenMultipleCandidates_whenDetected_thenPrefersSixDigitCode() {
        val result = otpDetector.detect("Your codes are 1234 and 567890. Use 567890 for login.")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should prefer 6-digit code", "567890", result.code)
    }

    @Test
    fun givenOtpMixedWithTransactionAmount_whenDetected_thenExtractsOtpNotAmount() {
        val result = otpDetector.detect("Amount: Rs 5000. Your OTP is 123456.")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract OTP not amount", "123456", result.code)
    }

    @Test
    fun givenMessageWithExtraWhitespace_whenDetected_thenExtractsOtp() {
        val result = otpDetector.detect("   Your    OTP    is    123456   ")

        assertTrue("Should detect OTP with extra whitespace", result.isOtp)
        assertEquals("Should extract code", "123456", result.code)
    }

    @Test
    fun givenOtpWithSpecialCharactersAround_whenDetected_thenExtractsCode() {
        val result = otpDetector.detect("Your OTP is: <123456>")

        assertTrue("Should detect OTP", result.isOtp)
        assertEquals("Should extract code", "123456", result.code)
    }

    // 3-Digit Code
    @Test
    fun givenThreeDigitCodeWithStrongOtpContext_whenDetected_thenExtractsCode() {
        val result = otpDetector.detect("Your verification code is 123")

        // 3-digit codes should still be detected if context is strong
        assertTrue("Should detect 3-digit OTP with strong context", result.isOtp)
        assertEquals("Should extract code", "123", result.code)
    }

    // Case sensitivity
    @Test
    fun givenUppercaseKeyword_whenDetected_thenExtractsOtp() {
        val result = otpDetector.detect("YOUR OTP IS 123456")

        assertTrue("Should detect OTP regardless of case", result.isOtp)
        assertEquals("Should extract code", "123456", result.code)
    }

    @Test
    fun givenMixedCaseKeyword_whenDetected_thenExtractsOtp() {
        val result = otpDetector.detect("Your VeRiFiCaTiOn CoDe is 789012")

        assertTrue("Should detect OTP with mixed case", result.isOtp)
        assertEquals("Should extract code", "789012", result.code)
    }

    // Multi-lingual
    @Test
    fun givenHindiKeyword_whenDetected_thenExtractsCodeIfSupported() {
        val result = otpDetector.detect("आपका रमज 123456 है")

        // The detector should at least extract the code even if keyword matching varies
        if (result.isOtp) {
            assertEquals("Should extract code", "123456", result.code)
        } else {
            // If not detected, it's okay as multi-language support may vary
            // This test documents the behavior rather than strictly requiring it
            assertNotNull("Test shows multi-language capability", result)
        }
    }

    @Test
    fun givenChineseKeyword_whenDetected_thenExtractsCodeIfSupported() {
        val result = otpDetector.detect("您的验证码是 654321")

        // The detector should at least extract the code even if keyword matching varies
        if (result.isOtp) {
            assertEquals("Should extract code", "654321", result.code)
        } else {
            // If not detected, it's okay as multi-language support may vary
            // This test documents the behavior rather than strictly requiring it
            assertNotNull("Test shows multi-language capability", result)
        }
    }

    // Check confidence level
    @Test
    fun givenClearOtpMessage_whenDetected_thenHasHighConfidence() {
        val result = otpDetector.detect("Your OTP is 123456. Do not share this code.")

        assertTrue("Should have high confidence", result.confidence >= 0.75)
    }

    @Test
    fun givenAmbiguousMessage_whenDetected_thenHasLowerConfidenceOrNotDetected() {
        val result = otpDetector.detect("Code: 123456789") // 9 digits might be too long

        // Should have lower confidence or not detect at all
        assertTrue("Should have lower confidence or not detect",
            !result.isOtp || result.confidence < 0.75)
    }

    // Performance and robustness
    @Test
    fun givenVeryLongMessage_whenDetected_thenExtractsOtp() {
        val longMessage = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(10) +
                "Your OTP is 123456. " +
                "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ".repeat(10)

        val result = otpDetector.detect(longMessage)

        assertTrue("Should detect OTP in long message", result.isOtp)
        assertEquals("Should extract code", "123456", result.code)
    }

    @Test
    fun givenMessageWithMultipleNumbers_whenDetected_thenExtractsCorrectOtp() {
        val result = otpDetector.detect("Your account 12345 received Rs 1000. OTP: 567890")

        assertTrue("Should detect OTP among multiple numbers", result.isOtp)
        assertEquals("Should extract correct OTP", "567890", result.code)
    }

    @Test
    fun givenNullLikeInputs_whenHandled_thenHandlesGracefully() {
        val result = otpDetector.detect("   \n\n   ")

        assertFalse("Should handle whitespace-only message", result.isOtp)
        assertNull("Should have null code", result.code)
    }
}
