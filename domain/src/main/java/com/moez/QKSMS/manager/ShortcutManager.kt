/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.manager

import androidx.core.content.pm.ShortcutInfoCompat

interface ShortcutManager {

    fun updateBadge()

    fun updateShortcuts()

    fun getShortcut(threadId: Long): ShortcutInfoCompat?

    fun reportShortcutUsed(threadId: Long)
}
