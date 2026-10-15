/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.util.extensions

import android.content.Context
import org.prauga.messages.R
import org.prauga.messages.repository.BackupRepository

fun BackupRepository.Progress.getLabel(context: Context): String? {
    return when (this) {
        is BackupRepository.Progress.Parsing -> context.getString(R.string.backup_progress_parsing)
        is BackupRepository.Progress.Running ->
            context.getString(R.string.backup_progress_running, count, max)

        is BackupRepository.Progress.Saving -> context.getString(R.string.backup_progress_saving)
        is BackupRepository.Progress.Syncing -> context.getString(R.string.backup_progress_syncing)
        is BackupRepository.Progress.Finished ->
            context.getString(R.string.backup_progress_finished)
        else -> null
    }
}
