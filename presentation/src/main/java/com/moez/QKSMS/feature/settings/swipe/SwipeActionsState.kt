/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.settings.swipe

import androidx.annotation.DrawableRes
import org.prauga.messages.R

data class SwipeActionsState(
    @DrawableRes val rightIcon: Int = R.drawable.ic_archive_white_24dp,
    val rightLabel: String = "",
    @DrawableRes val leftIcon: Int = R.drawable.ic_archive_white_24dp,
    val leftLabel: String = "",
)
