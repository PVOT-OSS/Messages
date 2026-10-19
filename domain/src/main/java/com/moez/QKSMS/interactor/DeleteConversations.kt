/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.manager.NotificationManager
import org.prauga.messages.repository.ConversationRepository

class DeleteConversations
@Inject
constructor(
    private val conversationRepo: ConversationRepository,
    private val notificationManager: NotificationManager,
    private val updateBadge: UpdateBadge,
) : Interactor<List<Long>>() {

    override fun buildObservable(params: List<Long>): Flowable<*> {
        return Flowable.just(params.toLongArray())
            .doOnNext { threadIds -> conversationRepo.deleteConversations(*threadIds) }
            .doOnNext { threadIds -> threadIds.forEach(notificationManager::update) }
            .flatMap { updateBadge.buildObservable(Unit) } // Update the badge
    }
}
