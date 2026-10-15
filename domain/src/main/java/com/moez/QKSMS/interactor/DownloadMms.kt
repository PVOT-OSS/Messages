/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.extensions.mapNotNull
import org.prauga.messages.model.Message
import org.prauga.messages.repository.MessageRepository

class DownloadMms @Inject constructor(private val messageRepo: MessageRepository) :
    Interactor<Long>() {

    override fun buildObservable(params: Long): Flowable<Message> {
        return Flowable.just(params)
            .mapNotNull(messageRepo::getMessage)
            .filter { message -> message.isPendingMmsDownload() && !message.isExpiredMmsDownload() }
            .doOnNext(messageRepo::downloadMms)
    }
}
