/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import org.prauga.messages.repository.SyncRepository
import timber.log.Timber

class SyncMessages
@Inject
constructor(private val syncManager: SyncRepository, private val updateBadge: UpdateBadge) :
    Interactor<Unit>() {

    override fun buildObservable(params: Unit): Flowable<*> {
        return Flowable.just(System.currentTimeMillis())
            .doOnNext { syncManager.syncMessages() }
            .map { startTime -> System.currentTimeMillis() - startTime }
            .map { elapsed -> TimeUnit.MILLISECONDS.toSeconds(elapsed) }
            .doOnNext { seconds -> Timber.v("Completed sync in $seconds seconds") }
            .flatMap { updateBadge.buildObservable(Unit) } // Update the badge
    }
}
