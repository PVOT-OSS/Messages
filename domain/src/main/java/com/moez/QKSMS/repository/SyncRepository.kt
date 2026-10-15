/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import android.net.Uri
import io.reactivex.Observable
import org.prauga.messages.model.Message

interface SyncRepository {

    sealed class SyncProgress {
        object Idle : SyncProgress()

        data class Running(val max: Int, val progress: Int, val indeterminate: Boolean) :
            SyncProgress()
    }

    val syncProgress: Observable<SyncProgress>

    fun syncMessages()

    fun syncMessage(uri: Uri): Message?

    fun syncContacts()
}
