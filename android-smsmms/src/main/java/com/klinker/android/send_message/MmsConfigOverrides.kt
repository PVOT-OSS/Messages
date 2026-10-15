/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package com.klinker.android.send_message

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import timber.log.Timber

object MmsConfigOverrides {

    const val DEFAULT_UA_PROF_URL = "http://www.gstatic.com/android/sms/mms_ua_profile.xml"

    @JvmStatic
    fun create(context: Context, subscriptionId: Int): Bundle {
        val subId = CarrierMmsConfig.resolveSubscriptionId(subscriptionId)
        val overrides =
            try {
                CarrierMmsConfig.overrides(context, subId)
            } catch (e: Exception) {
                Timber.w(e, "Unable to load the MMS carrier config")
                Bundle()
            }
        val useCustomUserAgent =
            try {
                CarrierMmsConfig.get(context, subId).useCustomUserAgent
            } catch (e: Exception) {
                false
            }

        val telephony = telephonyManager(context, subId)
        val version = appVersion(context)
        overrides.putString(
            SmsManager.MMS_CONFIG_USER_AGENT,
            when {
                useCustomUserAgent ->
                    customUserAgent(Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE, version)
                else -> userAgent(telephony?.mmsUserAgent, version)
            },
        )
        overrides.putString(
            SmsManager.MMS_CONFIG_UA_PROF_URL,
            telephony?.mmsUAProfUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_UA_PROF_URL,
        )
        return overrides
    }

    fun userAgent(carrierUserAgent: String?, appVersion: String?): String =
        carrierUserAgent?.takeIf { it.isNotBlank() }
            ?: listOfNotNull("Messages", appVersion?.takeIf { it.isNotBlank() }).joinToString(" ")

    fun customUserAgent(
        manufacturer: String,
        model: String,
        release: String,
        appVersion: String?,
    ): String =
        listOf(manufacturer, model, "Android-$release", "Messages_${appVersion.orEmpty()}")
            .joinToString("/") { it.replace(' ', '_') }

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
