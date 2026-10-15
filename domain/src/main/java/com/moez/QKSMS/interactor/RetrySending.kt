/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.extensions.mapNotNull
import org.prauga.messages.manager.NotificationManager
import org.prauga.messages.model.Message
import org.prauga.messages.repository.MessageRepository

class RetrySending
@Inject
constructor(
    private val messageRepo: MessageRepository,
    private val notificationManager: NotificationManager,
) : Interactor<Long>() {

    override fun buildObservable(params: Long): Flowable<Message> {
        return Flowable.just(params)
            .doOnNext(messageRepo::markSending)
            .mapNotNull(messageRepo::getMessage)
            .doOnNext { message ->
                when (message.isSms()) {
                    true -> messageRepo.sendSms(message)
                    false -> messageRepo.resendMms(message)
                }
                val threadId = message.threadId
                notificationManager.cancel(threadId.toInt() + 100000)
            }
    }
}
