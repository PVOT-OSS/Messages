/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.manager.NotificationManager
import org.prauga.messages.repository.MessageRepository

class MarkFailed
@Inject
constructor(
    private val messageRepo: MessageRepository,
    private val notificationManager: NotificationManager,
) : Interactor<MarkFailed.Params>() {

    data class Params(val id: Long, val resultCode: Int)

    override fun buildObservable(params: Params): Flowable<Unit> {
        return Flowable.just(Unit)
            .doOnNext { messageRepo.markFailed(params.id, params.resultCode) }
            .doOnNext { notificationManager.notifyFailed(params.id) }
    }
}
