/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.interactor

import io.reactivex.Flowable
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import org.prauga.messages.manager.PermissionManager
import org.prauga.messages.repository.MessageRepository

class DownloadPendingMms
@Inject
constructor(
    private val messageRepo: MessageRepository,
    private val permissionManager: PermissionManager,
) : Interactor<Unit>() {

    companion object {
        private val MAX_AGE = TimeUnit.DAYS.toMillis(7)
    }

    override fun buildObservable(params: Unit): Flowable<*> {
        return Flowable.just(params)
            .filter { permissionManager.isDefaultSms() }
            .map { messageRepo.getPendingMmsDownloads(System.currentTimeMillis() - MAX_AGE) }
            .doOnNext { messages ->
                messages.forEach { message -> messageRepo.downloadMms(message, automatic = true) }
            }
    }
}
