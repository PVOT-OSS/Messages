/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.settings

import org.prauga.messages.repository.SyncRepository
import org.prauga.messages.util.Preferences

data class SettingsState(
    val theme: Int = 0,
    val nightModeSummary: String = "",
    val nightModeId: Int = Preferences.NIGHT_MODE_OFF,
    val nightStart: String = "",
    val nightEnd: String = "",
    val autoEmojiEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val sendDelaySummary: String = "",
    val sendDelayId: Int = 0,
    val deliveryEnabled: Boolean = false,
    val unreadAtTopEnabled: Boolean = false,
    val signature: String = "",
    val systemFontEnabled: Boolean = false,
    val splitSmsEnabled: Boolean = false,
    val stripUnicodeEnabled: Boolean = false,
    val mobileOnly: Boolean = false,
    val autoDelete: Int = 0,
    val longAsMms: Boolean = false,
    val mmsAutoDownload: Boolean = true,
    val mmsRoamingAutoDownload: Boolean = false,
    val maxMmsSizeSummary: String = "100KB",
    val maxMmsSizeId: Int = 100,
    val messageLinkHandlingSummary: String = "Ask before opening",
    val messageLinkHandlingId: Int = 2,
    val disableScreenshotsEnabled: Boolean = false,
    val syncProgress: SyncRepository.SyncProgress = SyncRepository.SyncProgress.Idle,
)
