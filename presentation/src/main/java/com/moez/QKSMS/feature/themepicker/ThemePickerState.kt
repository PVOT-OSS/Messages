/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.themepicker

data class ThemePickerState(
    val recipientId: Long = 0,
    val applyThemeVisible: Boolean = false,
    val newColor: Int = -1,
    val newTextColor: Int = -1,
)
