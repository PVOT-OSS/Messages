/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.manager.AlarmManager
import org.prauga.messages.repository.ScheduledMessageRepository

class UpdateScheduledMessageAlarms
@Inject
constructor(
    private val alarmManager: AlarmManager,
    private val scheduledMessageRepo: ScheduledMessageRepository,
    private val sendScheduledMessage: SendScheduledMessage,
) : Interactor<Unit>() {

    override fun buildObservable(params: Unit): Flowable<*> {
        return Flowable.just(params)
            .map { scheduledMessageRepo.getScheduledMessages() } // Get all the scheduled messages
            .map {
                it.map { message -> Pair(message.id, message.date) }
            } // Map the data we need out of Realm
            .flatMap { messages -> Flowable.fromIterable(messages) } // Turn the list into a stream
            .doOnNext { (id, date) ->
                alarmManager.setAlarm(
                    date,
                    alarmManager.getScheduledMessageIntent(id),
                ) // Create alarm
            }
            .filter { (_, date) ->
                date < System.currentTimeMillis()
            } // Filter messages that should have already been sent
            .flatMap { (id, _) -> sendScheduledMessage.buildObservable(id) } // Send them
    }
}
