/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.app.receiver

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import org.prauga.messages.app.R

class CopyOtpReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val otpCode = intent.getStringExtra("otpCode") ?: return

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("OTP", otpCode)
        clipboard.setPrimaryClip(clip)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            Toast.makeText(
                    context,
                    context.getString(R.string.otp_copied, otpCode),
                    Toast.LENGTH_SHORT,
                )
                .show()
        }

        val threadId = intent.getLongExtra("threadId", -1)
        if (threadId != -1L) {
            context.sendBroadcast(
                Intent()
                    .setComponent(ComponentName(context, MARK_READ_RECEIVER))
                    .putExtra("threadId", threadId)
            )
        }
    }

    companion object {
        private const val MARK_READ_RECEIVER = "org.prauga.messages.receiver.MarkReadReceiver"
    }
}
