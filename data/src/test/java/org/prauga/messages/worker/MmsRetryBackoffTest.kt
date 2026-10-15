/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.worker

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MmsRetryBackoffTest {

    private val seconds = 1000L

    @Test
    fun givenFirstAttempt_whenRetryDelay_thenWaitsFiveSeconds() {
        repeat(20) { assertEquals(5 * seconds, MmsRetryBackoff.retryDelay(0, Random(it))) }
    }

    @Test
    fun givenLaterAttempts_whenRetryDelay_thenDoublesWithinTheUpperHalf() {
        repeat(20) {
            val delay = MmsRetryBackoff.retryDelay(3, Random(it))
            assertTrue(delay in 20 * seconds..40 * seconds)
        }
    }

    @Test
    fun givenManyAttempts_whenRetryDelay_thenCapsAtTwentyMinutes() {
        repeat(20) {
            val delay = MmsRetryBackoff.retryDelay(50, Random(it))
            assertTrue(delay in 10 * 60 * seconds..20 * 60 * seconds)
        }
    }
}
