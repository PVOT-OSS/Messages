/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.blocking.messages

import io.reactivex.Observable
import org.prauga.messages.common.base.QkViewContract

interface BlockedMessagesView : QkViewContract<BlockedMessagesState> {

    val menuReadyIntent: Observable<Unit>
    val optionsItemIntent: Observable<Int>
    val conversationClicks: Observable<Long>
    val selectionChanges: Observable<List<Long>>
    val confirmDeleteIntent: Observable<List<Long>>
    val backClicked: Observable<*>

    fun clearSelection()

    fun showBlockingDialog(conversations: List<Long>, block: Boolean)

    fun showDeleteDialog(conversations: List<Long>)

    fun goBack()
}
