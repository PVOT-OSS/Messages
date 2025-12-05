package org.prauga.messages.app.receiver

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import org.prauga.messages.app.R

class CopyOtpReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val otpCode = intent.getStringExtra("otpCode") ?: return

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("OTP", otpCode)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, context.getString(R.string.otp_copied, otpCode), Toast.LENGTH_SHORT)
            .show()
    }
}