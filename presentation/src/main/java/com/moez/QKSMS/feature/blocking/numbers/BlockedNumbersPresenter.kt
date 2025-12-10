/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.prauga.messages.feature.blocking.numbers

import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import org.prauga.messages.common.base.QkPresenter
import org.prauga.messages.interactor.MarkUnblocked
import org.prauga.messages.repository.BlockingRepository
import org.prauga.messages.repository.ConversationRepository
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class BlockedNumbersPresenter @Inject constructor(
    private val blockingRepo: BlockingRepository,
    private val conversationRepo: ConversationRepository,
    private val markUnblocked: MarkUnblocked
) : QkPresenter<BlockedNumbersView, BlockedNumbersState>(
        BlockedNumbersState(numbers = blockingRepo.getBlockedNumbers())
) {

    override fun bindIntents(view: BlockedNumbersView) {
        super.bindIntents(view)

        view.unblockAddress()
            .observeOn(Schedulers.io())
            .doOnNext { id ->
                blockingRepo.getBlockedNumber(id)?.address
                    ?.let(conversationRepo::getConversation)
                    ?.let { conversation -> markUnblocked.execute(listOf(conversation.id)) }
            }
            .doOnNext(blockingRepo::unblockNumber)
            .subscribeOn(Schedulers.io())
            .autoDispose(view.scope())
            .subscribe()

        view.addAddress()
            .autoDispose(view.scope())
            .subscribe { view.showAddDialog() }

        view.saveAddress()
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .autoDispose(view.scope())
            .subscribe { address -> blockingRepo.blockNumber(address) }
    }

}
