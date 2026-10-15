/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import android.net.Uri
import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.repository.BackupRepository

class PerformRestore @Inject constructor(private val backupRepo: BackupRepository) :
    Interactor<Uri>() {

    override fun buildObservable(params: Uri): Flowable<*> {
        return Flowable.just(params).doOnNext(backupRepo::performRestore)
    }
}
