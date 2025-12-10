/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.prauga.messages.feature.notificationprefs

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import org.prauga.messages.common.base.QkView
import org.prauga.messages.common.widget.PreferenceView

interface NotificationPrefsView : QkView<NotificationPrefsState> {

    val preferenceClickIntent: Flow<PreferenceView>
    val previewModeSelectedIntent: Flow<Int>
    val ringtoneSelectedIntent: Flow<String>
    val actionsSelectedIntent: Flow<Int>

    fun showPreviewModeDialog()
    fun showRingtonePicker(default: Uri?)
    fun showActionDialog(selected: Int)
}
