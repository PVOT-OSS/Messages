/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.reactivex.Observable
import org.prauga.messages.model.BackupFile

interface BackupRepository {

    sealed class Progress(val running: Boolean = false, val indeterminate: Boolean = true) {
        class Idle : Progress()

        class Parsing : Progress(true)

        class Running(val max: Int, val count: Int) : Progress(true, false)

        class Saving : Progress(true)

        class Syncing : Progress(true)

        class Finished : Progress(true, false)
    }

    fun getDefaultBackupPath(): String

    fun getBackupDocumentTree(): DocumentFile?

    fun getBackupPathUriForPicker(): Uri

    fun persistBackupDirectory(directory: Uri)

    fun performBackup()

    fun getBackupProgress(): Observable<Progress>

    fun parseBackup(uri: Uri): BackupFile

    fun performRestore(uri: Uri)

    fun getRestoreProgress(): Observable<Progress>

    fun stopRestore()
}
