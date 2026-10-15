/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.repository.MessageRepository

class MarkAllSeen @Inject constructor(private val messageRepo: MessageRepository) :
    Interactor<Unit>() {

    override fun buildObservable(params: Unit): Flowable<Unit> {
        return Flowable.just(Unit).doOnNext { messageRepo.markAllSeen() }
    }
}
