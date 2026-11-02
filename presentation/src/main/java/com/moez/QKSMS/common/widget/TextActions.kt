/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.widget

import android.content.ActivityNotFoundException
import timber.log.Timber

inline fun ignoringMissingActivity(action: () -> Boolean): Boolean =
    try {
        action()
    } catch (e: ActivityNotFoundException) {
        Timber.w(e)
        false
    }
