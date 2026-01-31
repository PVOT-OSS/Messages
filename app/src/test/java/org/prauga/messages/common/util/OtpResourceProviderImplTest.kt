/*
 * Copyright (C) 2026 Vishnu R <vishnurajesh45@gmail.com>
 */

package org.prauga.messages.common.util

import android.content.Context
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Unit tests for OtpResourceProviderImpl.
 * 
 * These tests verify resource loading, lowercase normalization,
 * fallback behavior, and error message retrieval.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class OtpResourceProviderImplTest {

    private lateinit var context: Context
    private lateinit var provider: OtpResourceProviderImpl

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        provider = OtpResourceProviderImpl(context)
    }

    @Test
    fun givenRealResources_whenGetOtpKeywords_thenReturnsLowercaseKeywords() {
        // When
        val result = provider.getOtpKeywords()

        // Then
        assertNotNull("OTP keywords should not be null", result)
        assertFalse("OTP keywords should not be empty", result.isEmpty())
        assertTrue("All keywords should be lowercase", result.all { it == it.lowercase() })
        assertTrue("Should contain 'otp'", result.contains("otp"))
        assertTrue("Should contain 'verification code'", result.contains("verification code"))
    }

    @Test
    fun givenRealResources_whenGetSafetyKeywords_thenReturnsLowercaseKeywords() {
        // When
        val result = provider.getSafetyKeywords()

        // Then
        assertNotNull("Safety keywords should not be null", result)
        assertFalse("Safety keywords should not be empty", result.isEmpty())
        assertTrue("All keywords should be lowercase", result.all { it == it.lowercase() })
        assertTrue("Should contain 'do not share'", result.contains("do not share"))
    }

    @Test
    fun givenRealResources_whenGetMoneyIndicators_thenReturnsLowercaseIndicators() {
        // When
        val result = provider.getMoneyIndicators()

        // Then
        assertNotNull("Money indicators should not be null", result)
        assertFalse("Money indicators should not be empty", result.isEmpty())
        assertTrue("All indicators should be lowercase", result.all { it == it.lowercase() })
        assertTrue("Should contain 'rs'", result.contains("rs"))
        assertTrue("Should contain 'balance'", result.contains("balance"))
    }

    @Test
    fun givenEmptyMessageError_whenGetErrorMessage_thenReturnsLocalizedMessage() {
        // When
        val result = provider.getErrorMessage(OtpErrorType.EMPTY_MESSAGE)

        // Then
        assertNotNull("Error message should not be null", result)
        assertFalse("Error message should not be empty", result.isEmpty())
        assertEquals("Empty message", result)
    }

    @Test
    fun givenNoKeywordError_whenGetErrorMessage_thenReturnsLocalizedMessage() {
        // When
        val result = provider.getErrorMessage(OtpErrorType.NO_OTP_KEYWORD)

        // Then
        assertNotNull("Error message should not be null", result)
        assertFalse("Error message should not be empty", result.isEmpty())
        assertTrue("Should contain expected message", 
            result.contains("No OTP") || result.contains("no candidate code"))
    }

    @Test
    fun givenKeywordButNoCodeError_whenGetErrorMessage_thenReturnsLocalizedMessage() {
        // When
        val result = provider.getErrorMessage(OtpErrorType.KEYWORD_BUT_NO_CODE)

        // Then
        assertNotNull("Error message should not be null", result)
        assertFalse("Error message should not be empty", result.isEmpty())
        assertTrue("Should contain expected message",
            result.contains("OTP") || result.contains("no numeric") || result.contains("no code"))
    }

    @Test
    fun givenMultipleCalls_whenGetOtpKeywords_thenReturnsConsistentResults() {
        // When
        val firstCall = provider.getOtpKeywords()
        val secondCall = provider.getOtpKeywords()

        // Then
        assertEquals("Multiple calls should return same size", firstCall.size, secondCall.size)
        assertTrue("Multiple calls should return same content", firstCall.containsAll(secondCall))
    }

    @Test
    fun givenKeywordsWithMixedCase_whenLoaded_thenAllAreLowercase() {
        // When
        val otpKeywords = provider.getOtpKeywords()
        val safetyKeywords = provider.getSafetyKeywords()
        val moneyIndicators = provider.getMoneyIndicators()

        // Then
        assertTrue("All OTP keywords should be lowercase", 
            otpKeywords.all { it == it.lowercase() })
        assertTrue("All safety keywords should be lowercase",
            safetyKeywords.all { it == it.lowercase() })
        assertTrue("All money indicators should be lowercase",
            moneyIndicators.all { it == it.lowercase() })
    }

    @Test
    fun givenProvider_whenGetAllResources_thenNoDuplicatesInOtpKeywords() {
        // When
        val keywords = provider.getOtpKeywords()

        // Then
        val uniqueKeywords = keywords.toSet()
        assertEquals("Should not have duplicate keywords", keywords.size, uniqueKeywords.size)
    }

    @Test
    fun givenProvider_whenGetErrorMessages_thenAllErrorTypesHaveMessages() {
        // When & Then
        for (errorType in OtpErrorType.values()) {
            val message = provider.getErrorMessage(errorType)
            assertNotNull("Error message for $errorType should not be null", message)
            assertFalse("Error message for $errorType should not be empty", message.isEmpty())
        }
    }
}
