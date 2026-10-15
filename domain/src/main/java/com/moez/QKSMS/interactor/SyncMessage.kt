/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import android.net.Uri
import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.extensions.mapNotNull
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.SyncRepository

class SyncMessage
@Inject
constructor(
    private val conversationRepo: ConversationRepository,
    private val syncManager: SyncRepository,
    private val updateBadge: UpdateBadge,
) : Interactor<Uri>() {

    override fun buildObservable(params: Uri): Flowable<*> {
        return Flowable.just(params)
            .mapNotNull { uri -> syncManager.syncMessage(uri) }
            .doOnNext { message -> conversationRepo.updateConversations(message.threadId) }
            .flatMap { updateBadge.buildObservable(Unit) } // Update the badge
    }
}
