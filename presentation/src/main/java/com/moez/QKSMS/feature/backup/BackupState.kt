/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.backup

import org.prauga.messages.repository.BackupRepository

data class BackupState(
    val backupProgress: BackupRepository.Progress = BackupRepository.Progress.Idle(),
    val restoreProgress: BackupRepository.Progress = BackupRepository.Progress.Idle(),
    val showLocationRationale: Boolean = false,
    val showSelectedBackupError: Boolean = false,
    val selectedBackupDetails: String? = null,
    val showStopRestoreDialog: Boolean = false,
    val upgraded: Boolean = true,
)
