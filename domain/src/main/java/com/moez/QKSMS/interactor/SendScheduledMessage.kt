/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import android.content.Context
import android.net.Uri
import io.reactivex.Flowable
import io.reactivex.rxkotlin.toFlowable
import io.realm.RealmList
import javax.inject.Inject
import org.prauga.messages.compat.TelephonyCompat
import org.prauga.messages.extensions.mapNotNull
import org.prauga.messages.model.Attachment
import org.prauga.messages.repository.ScheduledMessageRepository

class SendScheduledMessage
@Inject
constructor(
    private val context: Context,
    private val scheduledMessageRepo: ScheduledMessageRepository,
    private val deleteScheduledMessagesInteractor: DeleteScheduledMessages,
    private val sendMessage: SendMessage,
) : Interactor<Long>() {

    override fun buildObservable(params: Long): Flowable<*> {
        return Flowable.just(params)
            .mapNotNull(scheduledMessageRepo::getScheduledMessage)
            .flatMap { message ->
                if (message.sendAsGroup) {
                        listOf(message)
                    } else {
                        message.recipients.map { recipient ->
                            message.copy(recipients = RealmList(recipient))
                        }
                    }
                    .toFlowable()
            }
            .map { message ->
                val threadId = TelephonyCompat.getOrCreateThreadId(context, message.recipients)
                val attachments =
                    message.attachments.mapNotNull(Uri::parse).map { Attachment(context, it) }
                SendMessage.Params(
                    message.subId,
                    threadId,
                    message.recipients,
                    message.body,
                    attachments,
                )
            }
            .flatMap(sendMessage::buildObservable)
            .doOnNext { deleteScheduledMessagesInteractor.execute(listOf(params)) }
    }
}
