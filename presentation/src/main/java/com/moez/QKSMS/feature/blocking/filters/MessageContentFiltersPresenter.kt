/*
 * SPDX-FileCopyrightText: 2025 peanut250 <209579534+peanut250@users.noreply.github.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.blocking.filters

import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import org.prauga.messages.common.base.QkPresenter
import org.prauga.messages.repository.MessageContentFilterRepository

class MessageContentFiltersPresenter
@Inject
constructor(private val filterRepo: MessageContentFilterRepository) :
    QkPresenter<MessageContentFiltersView, MessageContentFiltersState>(
        MessageContentFiltersState(filters = filterRepo.getMessageContentFilters())
    ) {

    override fun bindIntents(view: MessageContentFiltersView) {
        super.bindIntents(view)

        view
            .removeFilter()
            .observeOn(Schedulers.io())
            .doOnNext(filterRepo::removeFilter)
            .subscribeOn(Schedulers.io())
            .autoDispose(view.scope())
            .subscribe()

        view.addFilter().autoDispose(view.scope()).subscribe { view.showAddDialog() }

        view
            .saveFilter()
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .autoDispose(view.scope())
            .subscribe { filterData -> filterRepo.createFilter(filterData) }
    }
}
