/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.manager.ShortcutManager
import org.prauga.messages.repository.ConversationRepository

class MarkPinned
@Inject
constructor(
    private val conversationRepo: ConversationRepository,
    private val updateBadge: UpdateBadge,
    private val shortcutManager: ShortcutManager,
) : Interactor<List<Long>>() {

    override fun buildObservable(params: List<Long>): Flowable<*> {
        return Flowable.just(params.toLongArray())
            .doOnNext { threadIds -> conversationRepo.markPinned(*threadIds) }
            .doOnNext { shortcutManager.updateShortcuts() } // Update shortcuts
            .flatMap { updateBadge.buildObservable(Unit) } // Update the badge and widget
    }
}
