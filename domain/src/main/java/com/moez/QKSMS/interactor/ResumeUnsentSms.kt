/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.interactor

import android.provider.Telephony
import io.reactivex.Flowable
import javax.inject.Inject
import timber.log.Timber
import org.prauga.messages.repository.MessageRepository

class ResumeUnsentSms @Inject constructor(private val messageRepo: MessageRepository) :
    Interactor<Long>() {

    override fun buildObservable(params: Long): Flowable<Unit> {
        return Flowable.fromCallable {
                messageRepo.getUnsentSms().forEach { message ->
                    when {
                        message.date > params ->
                            messageRepo.scheduleDelayedSms(message.id, message.date)
                        else -> messageRepo.markFailed(message.id, Telephony.MmsSms.ERR_TYPE_GENERIC)
                    }
                }
            }
            .doOnError(Timber::w)
            .onErrorReturnItem(Unit)
    }
}
