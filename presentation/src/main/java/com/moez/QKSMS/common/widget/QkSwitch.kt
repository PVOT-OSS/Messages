/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
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
package org.prauga.messages.common.widget

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import org.prauga.messages.R
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.extensions.resolveThemeColor
import org.prauga.messages.common.util.extensions.withAlpha
import org.prauga.messages.injection.appComponent
import org.prauga.messages.util.Preferences
import javax.inject.Inject

class QkSwitch @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    SwitchCompat(context, attrs) {

    @Inject
    lateinit var colors: Colors
    @Inject
    lateinit var prefs: Preferences

    init {
        if (!isInEditMode) {
            appComponent.inject(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (!isInEditMode) {
            val states = arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            )

            thumbTintList = ColorStateList(
                states, intArrayOf(
                    context.resolveThemeColor(R.attr.switchThumbDisabled),
                    colors.theme().theme,
                    context.resolveThemeColor(R.attr.switchThumbEnabled)
                )
            )

            trackTintList = ColorStateList(
                states, intArrayOf(
                    context.resolveThemeColor(R.attr.switchTrackDisabled),
                    colors.theme().theme.withAlpha(0x4D),
                    context.resolveThemeColor(R.attr.switchTrackEnabled)
                )
            )
        }
    }
}