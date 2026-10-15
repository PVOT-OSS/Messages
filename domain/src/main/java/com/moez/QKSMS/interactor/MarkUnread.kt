/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.repository.MessageRepository

class MarkUnread
@Inject
constructor(private val messageRepo: MessageRepository, private val updateBadge: UpdateBadge) :
    Interactor<List<Long>>() {

    override fun buildObservable(params: List<Long>): Flowable<*> {
        return Flowable.just(params)
            .doOnNext { threadIds -> messageRepo.markUnread(threadIds) }
            .flatMap { updateBadge.buildObservable(Unit) } // Update the badge
    }
}
