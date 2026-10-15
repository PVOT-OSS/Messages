/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import java.util.Locale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.messages.util.PhoneNumberUtils

@RunWith(AndroidJUnit4::class)
class PhoneNumberUtilsTest {

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val phoneNumberUtils = PhoneNumberUtils(context)

    @Before
    fun setup() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun compare_identicalNumbers_returnsTrue() {
        assertTrue(phoneNumberUtils.compare("+1 123 456 7890", "+1 123 456 7890"))
    }

    @Test
    fun compare_IdenticalNsnsWithOneMissingCountryCode_returnsTrue() {
        assertTrue(phoneNumberUtils.compare("+1 123 456 7890", "123 456 7890"))
    }

    @Test
    fun compare_IdenticalNsnsWithOnePoorlyFormattedCountryCode_returnsTrue() {
        assertTrue(phoneNumberUtils.compare("+1 123 456 7890", "1 123 456 7890"))
    }

    @Test
    fun compare_IdenticalFullNationalAustralianNsnsWithOneMissingCountryCode_returnsTrue() {
        assertTrue(phoneNumberUtils.compare("+61 4 1234 5678", "04 1234 5678"))
    }

    @Test
    fun compare_InvalidShortNsnMatch_returnsFalse() {
        assertFalse(phoneNumberUtils.compare("+1 123 456 7890", "67890"))
    }

    @Test
    fun compare_unequalNumbers_returnsFalse() {
        assertFalse(phoneNumberUtils.compare("123 456 7890", "234 567 8901"))
    }
}
