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

    data class Params(val id: Long, val resultCode: Int, val notify: Boolean = true)

    override fun buildObservable(params: Params): Flowable<Unit> {
        return Flowable.just(Unit)
            .doOnNext { messageRepo.markFailed(params.id, params.resultCode) }
            .doOnNext { if (params.notify) notificationManager.notifyFailed(params.id) }
    }
}
