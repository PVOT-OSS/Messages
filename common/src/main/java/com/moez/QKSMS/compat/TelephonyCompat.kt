/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.compat

import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import android.provider.Telephony
import android.text.TextUtils
import android.util.Patterns
import java.util.regex.Pattern

class TelephonyCompat {
    companion object {
        val THREADS_CONTENT_URI = Telephony.Threads.CONTENT_URI

        private val ID_PROJECTION = arrayOf(BaseColumns._ID)

        private val THREAD_ID_CONTENT_URI = Uri.parse("content://mms-sms/threadID")

        val NAME_ADDR_EMAIL_PATTERN = Pattern.compile("\\s*(\"[^\"]*\"|[^<>\"]+)\\s*<([^<>]+)>\\s*")

        fun getOrCreateThreadId(context: Context, recipient: String): Long {
            return getOrCreateThreadId(context, listOf(recipient))
        }

        fun getOrCreateThreadId(context: Context, recipients: Collection<String>): Long {
            return Telephony.Threads.getOrCreateThreadId(context, recipients.toSet())
        }

        fun extractAddrSpec(address: String): String {
            val match = NAME_ADDR_EMAIL_PATTERN.matcher(address)
            return if (match.matches()) {
                match.group(2)
            } else address
        }

        fun isEmailAddress(address: String): Boolean {
            if (TextUtils.isEmpty(address)) {
                return false
            }
            val s = extractAddrSpec(address)
            val match = Patterns.EMAIL_ADDRESS.matcher(s)
            return match.matches()
        }
    }
}
