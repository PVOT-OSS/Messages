/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.blocking.manager

data class BlockingManagerState(
    val blockingManager: Int = 0,
    val callBlockerInstalled: Boolean = false,
    val callControlInstalled: Boolean = false,
    val siaInstalled: Boolean = false,
)
