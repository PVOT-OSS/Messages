/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.util

import android.content.Context
import com.google.android.mms.pdu_alt.EncodedStringValue
import com.google.android.mms.pdu_alt.MultimediaMessagePdu
import com.google.android.mms.pdu_alt.PduPersister
import java.util.Locale.getDefault
import javax.inject.Inject
import org.prauga.messages.R
import org.prauga.messages.model.Message
import org.prauga.messages.util.tryOrNull

class MessageDetailsFormatter
@Inject
constructor(private val context: Context, private val dateFormatter: DateFormatter) {

    fun format(message: Message): String {
        val builder = StringBuilder()

        message.type
            .takeIf { it.isNotBlank() }
            ?.uppercase(getDefault())
            ?.let { context.getString(R.string.compose_details_type, it) }
            ?.let(builder::appendLine)

        if (message.isSms()) {
            message.address
                .takeIf { it.isNotBlank() && !message.isMe() }
                ?.let { context.getString(R.string.compose_details_from, it) }
                ?.let(builder::appendLine)

            message.address
                .takeIf { it.isNotBlank() && message.isMe() }
                ?.let { context.getString(R.string.compose_details_to, it) }
                ?.let(builder::appendLine)
        } else {
            val pdu = tryOrNull {
                PduPersister.getPduPersister(context).load(message.getUri()) as MultimediaMessagePdu
            }

            pdu?.from
                ?.string
                ?.takeIf { it.isNotBlank() }
                ?.let { context.getString(R.string.compose_details_from, it) }
                ?.let(builder::appendLine)

            pdu?.to
                ?.let(EncodedStringValue::concat)
                ?.takeIf { it.isNotBlank() }
                ?.let { context.getString(R.string.compose_details_to, it) }
                ?.let(builder::appendLine)
        }

        message.date
            .takeIf { it > 0 && message.isMe() }
            ?.let(dateFormatter::getDetailedTimestamp)
            ?.let { context.getString(R.string.compose_details_sent, it) }
            ?.let(builder::appendLine)

        message.dateSent
            .takeIf { it > 0 && !message.isMe() }
            ?.let(dateFormatter::getDetailedTimestamp)
            ?.let { context.getString(R.string.compose_details_sent, it) }
            ?.let(builder::appendLine)

        message.date
            .takeIf { it > 0 && !message.isMe() }
            ?.let(dateFormatter::getDetailedTimestamp)
            ?.let { context.getString(R.string.compose_details_received, it) }
            ?.let(builder::appendLine)

        message.dateSent
            .takeIf { it > 0 && message.isMe() }
            ?.let(dateFormatter::getDetailedTimestamp)
            ?.let { context.getString(R.string.compose_details_delivered, it) }
            ?.let(builder::appendLine)

        message.errorCode
            .takeIf { it != 0 && message.isSms() }
            ?.let { context.getString(R.string.compose_details_error_code, it) }
            ?.let(builder::appendLine)

        return builder.toString().trim()
    }
}
