/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.manager

interface MmsDownloadPolicy {

    fun shouldAutoDownload(subId: Int, from: String?): Boolean
}
