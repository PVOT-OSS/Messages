/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.manager

interface WidgetManager {

    companion object {
        const val ACTION_NOTIFY_DATASET_CHANGED =
            "org.prauga.messages.intent.action.ACTION_NOTIFY_DATASET_CHANGED"
    }

    fun sendDatasetChanged()

    fun updateTheme()
}
