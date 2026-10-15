/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package com.klinker.android.send_message

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import com.android.mms.MmsConfig
import timber.log.Timber

object MmsConfigOverrides {

    @JvmStatic
    fun create(context: Context, subscriptionId: Int): Bundle =
        Bundle().apply {
            MmsConfig.getHttpParams()
                ?.takeIf { it.isNotEmpty() }
                ?.let { putString(SmsManager.MMS_CONFIG_HTTP_PARAMS, it) }

            val telephony = telephonyManager(context, subscriptionId)
            putString(
                SmsManager.MMS_CONFIG_USER_AGENT,
                userAgent(telephony?.mmsUserAgent, appVersion(context)),
            )
            telephony
                ?.mmsUAProfUrl
                ?.takeIf { it.isNotBlank() }
                ?.let { putString(SmsManager.MMS_CONFIG_UA_PROF_URL, it) }
        }

    fun userAgent(carrierUserAgent: String?, appVersion: String?): String =
        carrierUserAgent?.takeIf { it.isNotBlank() }
            ?: listOfNotNull("Messages", appVersion?.takeIf { it.isNotBlank() }).joinToString(" ")

    private fun telephonyManager(context: Context, subscriptionId: Int): TelephonyManager? {
        val telephony = context.getSystemService(TelephonyManager::class.java) ?: return null
        return if (subscriptionId != -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            telephony.createForSubscriptionId(subscriptionId)
        } else {
            telephony
        }
    }

    private fun appVersion(context: Context): String? =
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            Timber.w(e)
            null
        }
}
