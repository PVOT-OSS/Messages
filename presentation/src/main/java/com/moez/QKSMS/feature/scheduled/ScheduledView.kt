/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.scheduled

import io.reactivex.Observable
import org.prauga.messages.common.base.QkView

interface ScheduledView : QkView<ScheduledState> {

    val composeIntent: Observable<*>
    val upgradeIntent: Observable<*>
    val messagesSelectedIntent: Observable<List<Long>>
    val optionsItemIntent: Observable<Int>
    val deleteScheduledMessages: Observable<List<Long>>
    val sendScheduledMessages: Observable<List<Long>>
    val editScheduledMessage: Observable<Long>
    val backPressedIntent: Observable<Unit>

    fun clearSelection()

    fun toggleSelectAll()

    fun showDeleteDialog(messages: List<Long>)

    fun showSendNowDialog(messages: List<Long>)

    fun showEditMessageDialog(message: Long)

    fun finishActivity()
}
