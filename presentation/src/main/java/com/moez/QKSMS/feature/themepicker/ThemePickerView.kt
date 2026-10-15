/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.themepicker

import io.reactivex.Observable
import org.prauga.messages.common.base.QkViewContract

interface ThemePickerView : QkViewContract<ThemePickerState> {

    fun themeSelected(): Observable<Int>

    fun hsvThemeSelected(): Observable<Int>

    fun clearHsvThemeClicks(): Observable<*>

    fun applyHsvThemeClicks(): Observable<*>

    fun viewQksmsPlusClicks(): Observable<*>

    fun setCurrentTheme(color: Int)

    fun showQksmsPlusSnackbar()
}
