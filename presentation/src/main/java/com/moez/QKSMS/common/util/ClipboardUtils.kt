/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import org.prauga.messages.R

class ClipboardUtils {
    companion object {
        fun copy(context: Context, string: String) {
            try {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("SMS", string))
            } catch (e: Exception) {
                Toast.makeText(
                        context,
                        if (
                            (e is RuntimeException) &&
                                e.message?.startsWith("android.os.TransactionTooLargeException") ==
                                    true
                        )
                            R.string.clipboard_too_large_to_copy
                        else R.string.clipboard_unable_to_copy_to,
                        Toast.LENGTH_LONG,
                    )
                    .show()
            }
        }
    }
}
