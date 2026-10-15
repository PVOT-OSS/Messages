/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.manager

interface MmsDownloadPolicy {

    fun shouldAutoDownload(subId: Int, from: String?): Boolean
}
