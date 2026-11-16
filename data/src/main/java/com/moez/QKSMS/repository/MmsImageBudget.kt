/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import kotlin.math.max
import kotlin.math.sqrt

object MmsImageBudget {

    private const val MIN_IMAGE_SIZE = 4096

    /** Like [budgets], but images that all fit share the whole of [remaining] by their size */
    fun shares(sizes: List<Int>, remaining: Double): List<Double> {
        val budgets = budgets(sizes, remaining)
        val total = budgets.sum()
        return if (total < remaining) budgets.map { it / total * remaining } else budgets
    }

    fun budgets(sizes: List<Int>, remaining: Double): List<Double> {
        val counted = sizes.map { max(it, MIN_IMAGE_SIZE).toDouble() }
        if (counted.sum() <= remaining) return counted

        val fairShare = remaining / counted.size
        val kept = counted.filter { it <= fairShare }
        val left = remaining - kept.sum()
        val roots = counted.filter { it > fairShare }.sumOf { sqrt(it) }

        return counted.map { size -> if (size <= fairShare) size else sqrt(size) / roots * left }
    }
}
