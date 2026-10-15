/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.receiver

data class ReceivedSms(val address: String, val body: String, val sentTime: Long) {

    data class Part(val address: String?, val body: String?, val sentTime: Long)

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
            )
        }
    }
}
