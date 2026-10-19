/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.blocking.filters

import io.reactivex.Observable
import org.prauga.messages.common.base.QkViewContract
import org.prauga.messages.model.MessageContentFilterData

interface MessageContentFiltersView : QkViewContract<MessageContentFiltersState> {

    fun removeFilter(): Observable<Long>

    fun addFilter(): Observable<*>

    fun saveFilter(): Observable<MessageContentFilterData>

    fun showAddDialog()
}
