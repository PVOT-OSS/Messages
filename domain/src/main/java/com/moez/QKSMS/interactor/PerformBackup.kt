/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.repository.BackupRepository

class PerformBackup @Inject constructor(private val backupRepo: BackupRepository) :
    Interactor<Unit>() {

    override fun buildObservable(params: Unit): Flowable<*> {
        return Flowable.just(params).doOnNext { backupRepo.performBackup() }
    }
}
