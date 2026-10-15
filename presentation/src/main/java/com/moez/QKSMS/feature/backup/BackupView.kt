/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.backup

import android.net.Uri
import io.reactivex.Observable
import org.prauga.messages.common.base.QkViewContract

interface BackupView : QkViewContract<BackupState> {
    fun setBackupLocationClicks(): Observable<*>

    fun restoreClicks(): Observable<*>

    fun backupClicks(): Observable<*>

    fun locationRationaleConfirmClicks(): Observable<*>

    fun locationRationaleCancelClicks(): Observable<*>

    fun selectedBackupErrorClicks(): Observable<*>

    fun confirmRestoreBackupConfirmClicks(): Observable<*>

    fun confirmRestoreBackupCancelClicks(): Observable<*>

    fun stopRestoreClicks(): Observable<*>

    fun stopRestoreConfirmed(): Observable<*>

    fun stopRestoreCancel(): Observable<*>

    fun documentTreeSelected(): Observable<Uri>

    fun documentSelected(): Observable<Uri>

    fun selectFolder(initialUri: Uri)

    fun selectFile(initialUri: Uri)
}
