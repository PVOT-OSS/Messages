/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.repository.SyncRepository
import timber.log.Timber

class SyncContacts @Inject constructor(private val syncManager: SyncRepository) :
    Interactor<Unit>() {

    override fun buildObservable(params: Unit): Flowable<Long> {
        return Flowable.just(System.currentTimeMillis())
            .doOnNext { syncManager.syncContacts() }
            .map { startTime -> System.currentTimeMillis() - startTime }
            .doOnNext { duration -> Timber.v("Completed sync in ${duration}ms") }
    }
}
