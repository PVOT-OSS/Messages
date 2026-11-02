/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.klinker.android.send_message

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.klinker.android.send_message.MmsHandoffState.Outcome
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MmsHandoffStateTest {

    private lateinit var state: MmsHandoffState

    @Before
    fun setUp() {
        val prefs =
            ApplicationProvider.getApplicationContext<Context>()
                .getSharedPreferences("mms_handoffs_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        state = MmsHandoffState(prefs)
    }

    @Test
    fun answerToTheAttemptInFlightIsCurrentAndStopsTheWait() {
        state.start(KEY, 1, SUB)

        assertEquals(Outcome.CURRENT, state.result(KEY, 1, timedOut = false, succeeded = true))
        assertFalse(state.isWaiting(KEY, 1))
        assertFalse(state.timeOut(KEY, 1, noNetwork = true))
    }

    @Test
    fun answerToAnEarlierAttemptIsStaleAndKeepsWaiting() {
        state.start(KEY, 1, SUB)
        state.start(KEY, 2, SUB)

        assertEquals(Outcome.STALE, state.result(KEY, 1, timedOut = false, succeeded = false))
        assertTrue(state.isWaiting(KEY, 2))
    }

    @Test
    fun answerWithoutAnAttemptIsCurrent() {
        state.start(KEY, 1, SUB)

        assertEquals(Outcome.CURRENT, state.result(KEY, null, timedOut = false, succeeded = true))
        assertFalse(state.isWaiting(KEY, 1))
    }

    @Test
    fun answerWithNothingInFlightIsCurrent() {
        assertEquals(Outcome.CURRENT, state.result(KEY, 7, timedOut = false, succeeded = false))
    }

    @Test
    fun timeoutIsCurrentAndTheAnswerAfterItIsLate() {
        state.start(KEY, 1, SUB)

        assertTrue(state.timeOut(KEY, 1, noNetwork = false))
        assertEquals(Outcome.CURRENT, state.result(KEY, 1, timedOut = true, succeeded = false))
        assertEquals(Outcome.LATE, state.result(KEY, 1, timedOut = false, succeeded = false))
    }

    @Test
    fun attemptTimesOutOnlyOnce() {
        state.start(KEY, 1, SUB)

        assertTrue(state.timeOut(KEY, 1, noNetwork = true))
        assertFalse(state.timeOut(KEY, 1, noNetwork = false))
    }

    @Test
    fun timeoutHandledAfterTheLateAnswerIsStale() {
        state.start(KEY, 1, SUB)
        state.timeOut(KEY, 1, noNetwork = true)

        assertEquals(Outcome.LATE, state.result(KEY, 1, timedOut = false, succeeded = true))
        assertEquals(Outcome.STALE, state.result(KEY, 1, timedOut = true, succeeded = false))
    }

    @Test
    fun timeoutForAReplacedAttemptIsStale() {
        state.start(KEY, 1, SUB)
        state.timeOut(KEY, 1, noNetwork = false)
        state.start(KEY, 2, SUB)

        assertEquals(Outcome.STALE, state.result(KEY, 1, timedOut = true, succeeded = false))
    }

    @Test
    fun lateSuccessAfterNoNetworkStopsTrustingTheProbeForThatSim() {
        state.start(KEY, 1, SUB)
        state.timeOut(KEY, 1, noNetwork = true)
        state.result(KEY, 1, timedOut = false, succeeded = true)

        assertFalse(state.probeTrusted(SUB))
        assertTrue(state.probeTrusted(SUB + 1))
    }

    @Test
    fun lateSuccessAfterTheDeadlineKeepsTrustingTheProbe() {
        state.start(KEY, 1, SUB)
        state.timeOut(KEY, 1, noNetwork = false)
        state.result(KEY, 1, timedOut = false, succeeded = true)

        assertTrue(state.probeTrusted(SUB))
    }

    @Test
    fun lateFailureAfterNoNetworkKeepsTrustingTheProbe() {
        state.start(KEY, 1, SUB)
        state.timeOut(KEY, 1, noNetwork = true)
        state.result(KEY, 1, timedOut = false, succeeded = false)

        assertTrue(state.probeTrusted(SUB))
    }

    @Test
    fun startingForgetsAttemptsOlderThanTwoDays() {
        val now = TimeUnit.DAYS.toMillis(10)
        state.start("other", now - TimeUnit.DAYS.toMillis(3), SUB)
        state.start("recent", now - TimeUnit.HOURS.toMillis(1), SUB)
        state.start(KEY, now, SUB)

        assertFalse(state.isWaiting("other", now - TimeUnit.DAYS.toMillis(3)))
        assertTrue(state.isWaiting("recent", now - TimeUnit.HOURS.toMillis(1)))
    }

    private companion object {
        const val KEY = "send:content://mms/1"
        const val SUB = 1
    }
}
