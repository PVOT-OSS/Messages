/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.blocking

import io.reactivex.Observable
import org.prauga.messages.common.base.QkViewContract

interface BlockingView : QkViewContract<BlockingState> {

    val blockingManagerIntent: Observable<*>
    val blockedNumbersIntent: Observable<*>
    val messageContentFiltersIntent: Observable<*>
    val blockedMessagesIntent: Observable<*>
    val dropClickedIntent: Observable<*>

    fun openBlockingManager()

    fun openBlockedNumbers()

    fun openMessageContentFilters()

    fun openBlockedMessages()
}
