/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.widget

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import javax.inject.Inject
import org.prauga.messages.R
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.extensions.resolveThemeColor
import org.prauga.messages.common.util.extensions.withAlpha
import org.prauga.messages.injection.appComponent
import org.prauga.messages.util.Preferences

class QkSwitch @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    SwitchCompat(context, attrs) {

    @Inject lateinit var colors: Colors
    @Inject lateinit var prefs: Preferences

    init {
        if (!isInEditMode) {
            appComponent.inject(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (!isInEditMode) {
            val states =
                arrayOf(
                    intArrayOf(-android.R.attr.state_enabled),
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(),
                )

            thumbTintList =
                ColorStateList(
                    states,
                    intArrayOf(
                        context.resolveThemeColor(R.attr.switchThumbDisabled),
                        colors.theme().theme,
                        context.resolveThemeColor(R.attr.switchThumbEnabled),
                    ),
                )

            trackTintList =
                ColorStateList(
                    states,
                    intArrayOf(
                        context.resolveThemeColor(R.attr.switchTrackDisabled),
                        colors.theme().theme.withAlpha(0x4D),
                        context.resolveThemeColor(R.attr.switchTrackEnabled),
                    ),
                )
        }
    }
}
