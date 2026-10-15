/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.telephony.SmsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.messages.receiver.SmsSentParts.Category
import org.prauga.messages.receiver.SmsSentParts.Result

class SmsSentPartsTest {

    private class MapStore : SmsSentParts.Store {
        val values = HashMap<String, String>()

        override fun get(key: String) = values[key]

        override fun put(key: String, value: String) {
            values[key] = value
        }

        override fun remove(key: String) {
            values.remove(key)
        }
    }

    private val store = MapStore()
    private val parts = SmsSentParts(store)

    private val sent = Result(Category.SENT, -1, -1)
    private val noService = Result(Category.AUTO_RETRY, SmsManager.RESULT_ERROR_NO_SERVICE, -1)
    private val generic = Result(Category.MANUAL_RETRY, SmsManager.RESULT_ERROR_GENERIC_FAILURE, 7)

    @Test
    fun givenSinglePart_whenItReports_thenThatIsTheResult() {
        assertEquals(sent, parts.onResult(1, 0, parts = 1, result = sent))
        assertEquals(generic, parts.onResult(1, 0, parts = 1, result = generic))
        assertTrue(store.values.isEmpty())
    }

    @Test
    fun givenMultipart_whenEveryPartIsSent_thenItIsSentOnceTheLastReports() {
        assertNull(parts.onResult(1, 7, parts = 3, result = sent))
        assertNull(parts.onResult(1, 7, parts = 3, result = sent))
        assertEquals(sent, parts.onResult(1, 7, parts = 3, result = sent))
        assertTrue(store.values.isEmpty())
    }

    @Test
    fun givenMultipart_whenAMiddlePartFails_thenTheWholeMessageFailsWithItsCodes() {
        assertNull(parts.onResult(1, 7, parts = 3, result = sent))
        assertNull(parts.onResult(1, 7, parts = 3, result = generic))
        assertEquals(generic, parts.onResult(1, 7, parts = 3, result = sent))
    }

    @Test
    fun givenMultipart_whenPartsFailDifferently_thenTheWorstDecides() {
        assertNull(parts.onResult(1, 7, parts = 3, result = generic))
        assertNull(parts.onResult(1, 7, parts = 3, result = noService))
        assertEquals(generic, parts.onResult(1, 7, parts = 3, result = sent))
    }

    @Test
    fun givenMultipart_whenOnlyATemporaryFailure_thenItIsRetried() {
        assertNull(parts.onResult(1, 7, parts = 2, result = noService))
        assertEquals(noService, parts.onResult(1, 7, parts = 2, result = sent))
    }

    @Test
    fun givenTheAppWasKilledBetweenParts_whenTheRestReport_thenTheEarlierFailureIsKept() {
        assertNull(parts.onResult(1, 7, parts = 2, result = generic))

        val restarted = SmsSentParts(store)

        assertEquals(generic, restarted.onResult(1, 7, parts = 2, result = sent))
    }

    @Test
    fun givenTwoAttempts_whenBothReport_thenTheyAreCombinedSeparately() {
        assertNull(parts.onResult(1, 7, parts = 2, result = generic))
        assertNull(parts.onResult(1, 8, parts = 2, result = sent))
        assertEquals(sent, parts.onResult(1, 8, parts = 2, result = sent))
        assertEquals(generic, parts.onResult(1, 7, parts = 2, result = sent))
    }

    @Test
    fun givenACorruptStoredValue_whenAPartReports_thenItStartsOver() {
        store.values["1:7"] = "garbage"

        assertNull(parts.onResult(1, 7, parts = 2, result = sent))
        assertEquals(generic, parts.onResult(1, 7, parts = 2, result = generic))
    }
}
