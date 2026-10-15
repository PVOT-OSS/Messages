/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class MmsImageBudgetTest {

    private fun assertBudgets(expected: List<Double>, actual: List<Double>) {
        assertEquals(expected.size, actual.size)
        expected.zip(actual).forEach { (e, a) -> assertEquals(e, a, 0.5) }
    }

    @Test
    fun givenImagesThatFit_whenBudgeted_thenTheyKeepTheirSize() {
        assertBudgets(
            listOf(10_000.0, 20_000.0),
            MmsImageBudget.budgets(listOf(10_000, 20_000), 100_000.0),
        )
    }

    @Test
    fun givenATinyImage_whenBudgeted_thenItCountsAsFourKilobytes() {
        assertBudgets(listOf(4096.0), MmsImageBudget.budgets(listOf(100), 100_000.0))
    }

    @Test
    fun givenImagesThatDontFit_whenBudgeted_thenSplitBySquareRoot() {
        assertBudgets(
            listOf(100_000.0, 50_000.0),
            MmsImageBudget.budgets(listOf(400_000, 100_000), 150_000.0),
        )
    }

    @Test
    fun givenASmallImageAmongBigOnes_whenBudgeted_thenTheSmallOneKeepsItsSize() {
        val budgets = MmsImageBudget.budgets(listOf(20_000, 900_000, 400_000), 300_000.0)

        assertEquals(20_000.0, budgets[0], 0.5)
        assertEquals(280_000.0, budgets[1] + budgets[2], 0.5)
        assertEquals(1.5, budgets[1] / budgets[2], 0.001)
    }
}
