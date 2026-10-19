/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.manager.NotificationManager
import org.prauga.messages.repository.MessageRepository

class MarkRead
@Inject
constructor(
    private val messageRepo: MessageRepository,
    private val notificationManager: NotificationManager,
    private val updateBadge: UpdateBadge,
) : Interactor<List<Long>>() {

    override fun buildObservable(params: List<Long>): Flowable<*> {
        return Flowable.just(params)
            .doOnNext { threadIds -> messageRepo.markRead(threadIds) }
            .doOnNext { threadIds -> threadIds.forEach(notificationManager::update) }
            .flatMap { updateBadge.buildObservable(Unit) } // Update the badge
    }
}
