/*
 * SPDX-FileCopyrightText: 2025 Ben Iofel <ben@iofel.me>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.model

import io.realm.RealmObject

open class EmojiSyncNeeded : RealmObject() {
    var createdAt: Long = System.currentTimeMillis()
}
