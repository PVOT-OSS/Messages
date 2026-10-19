/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import org.prauga.messages.model.ReceivedSmsDetails

data class ReceivedSms(
    val address: String,
    val body: String,
    val sentTime: Long,
    val details: ReceivedSmsDetails = ReceivedSmsDetails(),
    val classZero: Boolean = false,
) {

    data class Part(
        val address: String?,
        val body: String?,
        val sentTime: Long,
        val details: ReceivedSmsDetails = ReceivedSmsDetails(),
        val classZero: Boolean = false,
    )

    companion object {
        const val UNKNOWN_SENDER = "Unknown sender"

        fun from(parts: List<Part>): ReceivedSms? {
            if (parts.isEmpty()) return null

            return ReceivedSms(
                address =
                    parts.firstNotNullOfOrNull { it.address?.takeIf(String::isNotBlank) }
                        ?: UNKNOWN_SENDER,
                body = parts.joinToString("") { it.body.orEmpty() },
                sentTime = parts.first().sentTime,
                details = parts.first().details,
                classZero = parts.first().classZero,
            )
        }
    }
}
