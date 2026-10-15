/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.plus

data class PlusState(
    val upgraded: Boolean = true,
    val upgradePrice: String = "",
    val upgradeDonatePrice: String = "",
    val currency: String = "",
)
