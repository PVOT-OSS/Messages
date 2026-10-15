/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.util

import org.junit.Assert.assertEquals
import org.junit.Test

class RunAfterTest {

    @Test
    fun givenCountOfThree_whenInvokedThreeTimes_thenRunsActionOnceAtTheEnd() {
        var runs = 0
        val callback = runAfter(3) { runs++ }

        callback()
        callback()
        assertEquals(0, runs)

        callback()
        assertEquals(1, runs)
    }

    @Test
    fun givenCountReached_whenInvokedAgain_thenDoesNotRunActionAgain() {
        var runs = 0
        val callback = runAfter(1) { runs++ }

        callback()
        callback()

        assertEquals(1, runs)
    }

    @Test
    fun givenZeroCount_whenCreated_thenRunsActionImmediately() {
        var runs = 0
        runAfter(0) { runs++ }

        assertEquals(1, runs)
    }
}
