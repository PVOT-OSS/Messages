/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.model

data class ReceivedSmsDetails(
    val protocol: Int = 0,
    val replyPathPresent: Boolean = false,
    val serviceCenter: String? = null,
    val subject: String? = null,
    val replace: Boolean = false,
)
