/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.notificationprefs

import android.net.Uri
import io.reactivex.Observable
import io.reactivex.subjects.Subject
import org.prauga.messages.common.base.QkView
import org.prauga.messages.common.widget.PreferenceView

interface NotificationPrefsView : QkView<NotificationPrefsState> {

    val preferenceClickIntent: Subject<PreferenceView>
    val previewModeSelectedIntent: Subject<Int>
    val ringtoneSelectedIntent: Observable<String>
    val actionsSelectedIntent: Subject<Int>

    fun showPreviewModeDialog()

    fun showRingtonePicker(default: Uri?)

    fun showActionDialog(selected: Int)
}
