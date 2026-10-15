/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.repository.ConversationRepository

class MarkBlocked
@Inject
constructor(private val conversationRepo: ConversationRepository, private val markRead: MarkRead) :
    Interactor<MarkBlocked.Params>() {

    data class Params(val threadIds: List<Long>, val blockingClient: Int, val blockReason: String?)

    override fun buildObservable(params: Params): Flowable<*> {
        return Flowable.just(params)
            .doOnNext { (threadIds, blockingClient, blockReason) ->
                conversationRepo.markBlocked(threadIds, blockingClient, blockReason)
            }
            .flatMap { (threadIds) -> markRead.buildObservable(threadIds) }
    }
}
