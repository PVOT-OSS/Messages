/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.repository.MessageRepository

class SaveImage @Inject constructor(private val messageRepo: MessageRepository) :
    Interactor<Long>() {

    override fun buildObservable(params: Long): Flowable<*> {
        return Flowable.just(params).doOnNext { partId -> messageRepo.savePart(partId) }
    }
}
