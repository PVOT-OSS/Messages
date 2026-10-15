/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.blocking.numbers

import io.realm.RealmResults
import org.prauga.messages.model.BlockedNumber

data class BlockedNumbersState(val numbers: RealmResults<BlockedNumber>? = null)
