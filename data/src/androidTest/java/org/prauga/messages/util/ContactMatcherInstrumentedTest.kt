/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.util

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.realm.RealmList
import java.util.Locale
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.messages.model.Contact
import org.prauga.messages.model.PhoneNumber

@RunWith(AndroidJUnit4::class)
class ContactMatcherInstrumentedTest {

    private lateinit var phoneNumberUtils: PhoneNumberUtils

    @Before
    fun setUp() {
        Locale.setDefault(Locale("en", "IN"))
        phoneNumberUtils = PhoneNumberUtils(ApplicationProvider.getApplicationContext())
    }

    private fun contact(key: String, vararg numbers: String) =
        Contact(lookupKey = key).apply {
            this.numbers = RealmList(*numbers.map { PhoneNumber(address = it) }.toTypedArray())
        }

    private fun formats(i: Int): List<String> {
        val local = "98765${(10000 + i).toString().takeLast(5)}"
        return listOf("+91$local", "0$local", "${local.take(5)} ${local.drop(5)}", "+91-$local")
    }

    @Test
    fun givenManyContactsAndAddresses_whenMatching_thenSameContactAsComparingEveryPair() {
        val contacts =
            (0 until 300).map { i ->
                if (i % 3 == 0) contact("c$i", formats(i)[i % 4], "57575")
                else contact("c$i", formats(i)[i % 4])
            } + contact("dup", formats(7)[2])
        val addresses =
            (0 until 450).map { i -> formats(i)[(i + 1) % 4] } +
                listOf("57575", "AMAZON", "VK-HDFCBK", "someone@example.com", "", "5550000") +
                (0 until 144).map { i -> "+1 415 555 ${(1000 + i)}" }

        var compares = 0
        val matcher =
            ContactMatcher(contacts) { a, b ->
                compares++
                phoneNumberUtils.compare(a, b)
            }

        addresses.forEach { address ->
            val expected =
                contacts.firstOrNull { c ->
                    c.numbers.any { phoneNumberUtils.compare(address, it.address) }
                }
            assertThat(matcher.find(address)).isSameInstanceAs(expected)
        }
        assertThat(compares).isLessThan(addresses.size * 10)
    }
}
