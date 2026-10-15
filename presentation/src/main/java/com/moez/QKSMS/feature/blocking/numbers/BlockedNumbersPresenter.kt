/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.blocking.numbers

import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import org.prauga.messages.common.base.QkPresenter
import org.prauga.messages.interactor.MarkUnblocked
import org.prauga.messages.repository.BlockingRepository
import org.prauga.messages.repository.ConversationRepository

class BlockedNumbersPresenter
@Inject
constructor(
    private val blockingRepo: BlockingRepository,
    private val conversationRepo: ConversationRepository,
    private val markUnblocked: MarkUnblocked,
) :
    QkPresenter<BlockedNumbersView, BlockedNumbersState>(
        BlockedNumbersState(numbers = blockingRepo.getBlockedNumbers())
    ) {

    override fun bindIntents(view: BlockedNumbersView) {
        super.bindIntents(view)

        view
            .unblockAddress()
            .observeOn(Schedulers.io())
            .doOnNext { id ->
                blockingRepo
                    .getBlockedNumber(id)
                    ?.address
                    ?.let(conversationRepo::getConversation)
                    ?.let { conversation -> markUnblocked.execute(listOf(conversation.id)) }
            }
            .doOnNext(blockingRepo::unblockNumber)
            .subscribeOn(Schedulers.io())
            .autoDispose(view.scope())
            .subscribe()

        view.addAddress().autoDispose(view.scope()).subscribe { view.showAddDialog() }

        view
            .saveAddress()
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .autoDispose(view.scope())
            .subscribe { address -> blockingRepo.blockNumber(address) }
    }
}
