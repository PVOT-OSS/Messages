/*
 * SPDX-FileCopyrightText: 2017 Lucy Yu <lucyyu24@users.noreply.github.com>
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.repository.ConversationRepository

class MarkArchived
@Inject
constructor(private val conversationRepo: ConversationRepository, private val markRead: MarkRead) :
    Interactor<List<Long>>() {

    override fun buildObservable(params: List<Long>): Flowable<*> {
        return Flowable.just(params.toLongArray())
            .doOnNext { threadIds -> conversationRepo.markArchived(*threadIds) }
            .flatMap { markRead.buildObservable(params) }
    }
}
