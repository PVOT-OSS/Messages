/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.blocking.numbers

import io.reactivex.Observable
import org.prauga.messages.common.base.QkViewContract

interface BlockedNumbersView : QkViewContract<BlockedNumbersState> {

    fun unblockAddress(): Observable<Long>

    fun addAddress(): Observable<*>

    fun saveAddress(): Observable<String>

    fun showAddDialog()
}
