/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.blocking.messages

import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import javax.inject.Inject
import org.prauga.messages.R
import org.prauga.messages.blocking.BlockingClient
import org.prauga.messages.common.Navigator
import org.prauga.messages.common.base.QkPresenter
import org.prauga.messages.interactor.DeleteConversations
import org.prauga.messages.repository.ConversationRepository

class BlockedMessagesPresenter
@Inject
constructor(
    conversationRepo: ConversationRepository,
    private val blockingClient: BlockingClient,
    private val deleteConversations: DeleteConversations,
    private val navigator: Navigator,
) :
    QkPresenter<BlockedMessagesView, BlockedMessagesState>(
        BlockedMessagesState(data = conversationRepo.getBlockedConversationsAsync())
    ) {

    override fun bindIntents(view: BlockedMessagesView) {
        super.bindIntents(view)

        view.menuReadyIntent.autoDispose(view.scope()).subscribe { newState { copy() } }

        view.optionsItemIntent
            .withLatestFrom(view.selectionChanges) { itemId, conversations ->
                when (itemId) {
                    R.id.block -> {
                        view.showBlockingDialog(conversations, false)
                        view.clearSelection()
                    }

                    R.id.delete -> {
                        view.showDeleteDialog(conversations)
                    }
                }
            }
            .autoDispose(view.scope())
            .subscribe()

        view.confirmDeleteIntent.autoDispose(view.scope()).subscribe { conversations ->
            deleteConversations.execute(conversations)
            view.clearSelection()
        }

        view.conversationClicks.autoDispose(view.scope()).subscribe { threadId ->
            navigator.showConversation(threadId)
        }

        view.selectionChanges.autoDispose(view.scope()).subscribe { selection ->
            newState { copy(selected = selection.size) }
        }

        view.backClicked
            .withLatestFrom(state) { _, state ->
                when (state.selected) {
                    0 -> view.goBack()
                    else -> view.clearSelection()
                }
            }
            .autoDispose(view.scope())
            .subscribe()
    }
}
