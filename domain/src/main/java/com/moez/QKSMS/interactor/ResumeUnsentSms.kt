/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.interactor

import android.provider.Telephony
import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.manager.NotificationManager
import org.prauga.messages.repository.MessageRepository
import timber.log.Timber

class ResumeUnsentSms
@Inject
constructor(
    private val messageRepo: MessageRepository,
    private val notificationManager: NotificationManager,
) : Interactor<ResumeUnsentSms.Params>() {

    data class Params(val now: Long, val retrying: Set<Long> = emptySet())

    override fun buildObservable(params: Params): Flowable<Unit> {
        return Flowable.fromCallable {
                messageRepo.getUnsentSms().forEach { message ->
                    when {
                        message.id in params.retrying -> Unit
                        message.date > params.now ->
                            messageRepo.scheduleDelayedSms(message.id, message.date)
                        messageRepo.isDelayedSmsWaiting(message.id) ->
                            messageRepo.scheduleDelayedSms(message.id, params.now)
                        else -> {
                            messageRepo.markFailed(message.id, Telephony.MmsSms.ERR_TYPE_GENERIC)
                            notificationManager.notifyFailed(message.id)
                        }
                    }
                }
            }
            .doOnError(Timber::w)
            .onErrorReturnItem(Unit)
    }
}
