/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.model

data class ReceivedSmsDetails(
    val protocol: Int = 0,
    val replyPathPresent: Boolean = false,
    val serviceCenter: String? = null,
    val subject: String? = null,
    val replace: Boolean = false,
)
