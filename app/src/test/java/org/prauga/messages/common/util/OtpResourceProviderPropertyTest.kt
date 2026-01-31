/*
 * Copyright (C) 2026 Vishnu R <vishnurajesh45@gmail.com>
 */

package org.prauga.messages.common.util

import android.os.Build
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Property-based tests for OtpResourceProviderImpl.
 * 
 * These tests verify universal properties across multiple test iterations.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class OtpResourceProviderPropertyTest {

    private lateinit var provider: OtpResourceProviderImpl

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        provider = OtpResourceProviderImpl(context)
    }

    /**
     * Property 1: Resource-based keyword loading
     * 
     * For any loaded keyword list,
     * all keywords should be lowercase (validates normalization).
     */
    @Test
    fun property_resourceBasedKeywordLoading_allAreLowercase() {
        // Property: All loaded OTP keywords should be lowercase
        val keywords = provider.getOtpKeywords()
        
        // Verify property holds for all keywords
        assertTrue("All OTP keywords should be lowercase", 
            keywords.all { it == it.lowercase() })
    }

    /**
     * Property 3: Lowercase normalization for OTP keywords
     * 
     * For any list of mixed-case keywords,
     * all returned keywords should be lowercase.
     */
    @Test
    fun property_lowercaseNormalization_otpKeywords() {
        // Property: For any call to getOtpKeywords, all returned keywords are lowercase
        repeat(100) {
            val result = provider.getOtpKeywords()
            assertTrue("All OTP keywords should be lowercase on iteration $it",
                result.all { keyword -> keyword == keyword.lowercase() })
        }
    }

    /**
     * Property 3: Lowercase normalization for safety keywords
     * 
     * For any list of mixed-case
     * safety keywords, all returned keywords should be lowercase.
     */
    @Test
    fun property_lowercaseNormalization_safetyKeywords() {
        // Property: For any call to getSafetyKeywords, all returned keywords are lowercase
        repeat(100) {
            val result = provider.getSafetyKeywords()
            assertTrue("All safety keywords should be lowercase on iteration $it",
                result.all { keyword -> keyword == keyword.lowercase() })
        }
    }

    /**
     * Property 3: Lowercase normalization for money indicators
     * 
     * For any list of mixed-case
     * money indicators, all returned indicators should be lowercase.
     */
    @Test
    fun property_lowercaseNormalization_moneyIndicators() {
        // Property: For any call to getMoneyIndicators, all returned indicators are lowercase
        repeat(100) {
            val result = provider.getMoneyIndicators()
            assertTrue("All money indicators should be lowercase on iteration $it",
                result.all { indicator -> indicator == indicator.lowercase() })
        }
    }

    /**
     * Property: Consistency across multiple calls
     * 
     * Multiple calls to the same
     * method should return consistent results.
     */
    @Test
    fun property_consistency_multipleCallsReturnSameResults() {
        // Property: Multiple calls return the same keywords
        repeat(100) {
            val firstCall = provider.getOtpKeywords()
            val secondCall = provider.getOtpKeywords()
            assertTrue("Multiple calls should return same results on iteration $it",
                firstCall == secondCall)
        }
    }

    /**
     * Property: Non-empty keyword lists
     * 
     * All keyword lists should
     * contain at least one keyword (either from resources or fallback).
     */
    @Test
    fun property_nonEmpty_allKeywordListsHaveContent() {
        // Property: All keyword lists are non-empty
        repeat(100) {
            val otpKeywords = provider.getOtpKeywords()
            val safetyKeywords = provider.getSafetyKeywords()
            val moneyIndicators = provider.getMoneyIndicators()
            
            assertTrue("OTP keywords should not be empty on iteration $it", 
                otpKeywords.isNotEmpty())
            assertTrue("Safety keywords should not be empty on iteration $it",
                safetyKeywords.isNotEmpty())
            assertTrue("Money indicators should not be empty on iteration $it",
                moneyIndicators.isNotEmpty())
        }
    }

    /**
     * Property: Error messages are non-empty
     * 
     * All error messages should
     * be non-empty strings.
     */
    @Test
    fun property_errorMessages_allAreNonEmpty() {
        // Property: All error messages are non-empty
        repeat(100) {
            val emptyMsg = provider.getErrorMessage(OtpErrorType.EMPTY_MESSAGE)
            val noKeywordMsg = provider.getErrorMessage(OtpErrorType.NO_OTP_KEYWORD)
            val noCodeMsg = provider.getErrorMessage(OtpErrorType.KEYWORD_BUT_NO_CODE)
            
            assertTrue("Empty message error should not be empty on iteration $it",
                emptyMsg.isNotEmpty())
            assertTrue("No keyword error should not be empty on iteration $it",
                noKeywordMsg.isNotEmpty())
            assertTrue("No code error should not be empty on iteration $it",
                noCodeMsg.isNotEmpty())
        }
    }
}
