/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.settings

import io.reactivex.Observable
import org.prauga.messages.common.base.QkViewContract
import org.prauga.messages.common.widget.PreferenceView

interface SettingsView : QkViewContract<SettingsState> {
    fun preferenceClicks(): Observable<PreferenceView>

    fun aboutLongClicks(): Observable<*>

    fun viewQksmsPlusClicks(): Observable<*>

    fun nightModeSelected(): Observable<Int>

    fun nightStartSelected(): Observable<Pair<Int, Int>>

    fun nightEndSelected(): Observable<Pair<Int, Int>>

    fun sendDelaySelected(): Observable<Int>

    fun signatureChanged(): Observable<String>

    fun autoDeleteChanged(): Observable<Int>

    fun mmsSizeSelected(): Observable<Int>

    fun messageLinkHandlingSelected(): Observable<Int>

    fun showQksmsPlusSnackbar()

    fun showNightModeDialog()

    fun showStartTimePicker(hour: Int, minute: Int)

    fun showEndTimePicker(hour: Int, minute: Int)

    fun showDelayDurationDialog()

    fun showSignatureDialog(signature: String)

    fun showAutoDeleteDialog(days: Int)

    suspend fun showAutoDeleteWarningDialog(messages: Int): Boolean

    fun showMmsSizePicker()

    fun showMessageLinkHandlingDialogPicker()

    fun showSwipeActions()

    fun showThemePicker()

    fun showAbout()
}
