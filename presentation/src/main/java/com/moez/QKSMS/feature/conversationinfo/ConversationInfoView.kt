/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.conversationinfo

import io.reactivex.Observable
import org.prauga.messages.common.base.QkViewContract

interface ConversationInfoView : QkViewContract<ConversationInfoState> {

    fun recipientClicks(): Observable<Long>

    fun recipientLongClicks(): Observable<Long>

    fun themeClicks(): Observable<Long>

    fun nameClicks(): Observable<*>

    fun nameChanges(): Observable<String>

    fun notificationClicks(): Observable<*>

    fun markUnreadClicks(): Observable<*>

    fun archiveClicks(): Observable<*>

    fun blockClicks(): Observable<*>

    fun deleteClicks(): Observable<*>

    fun confirmDelete(): Observable<*>

    fun mediaClicks(): Observable<Long>

    fun showNameDialog(name: String)

    fun showThemePicker(recipientId: Long)

    fun showBlockingDialog(conversations: List<Long>, block: Boolean)

    fun requestDefaultSms()

    fun showDeleteDialog()
}
