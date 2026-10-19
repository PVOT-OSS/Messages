/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class BlockedNumber(@PrimaryKey var id: Long = 0, var address: String = "") : RealmObject()
