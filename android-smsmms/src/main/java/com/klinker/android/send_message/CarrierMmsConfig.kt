/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package com.klinker.android.send_message

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import org.xmlpull.v1.XmlPullParser
import timber.log.Timber

class CarrierMmsConfig internal constructor(private val values: Bundle) {

    val mmsEnabled: Boolean
        get() = bool(KEY_MMS_ENABLED, true)

    val groupMmsEnabled: Boolean
        get() = mmsEnabled && bool(KEY_GROUP_MMS_ENABLED, true)

    val groupChatDefaultsToMms: Boolean
        get() = bool(KEY_GROUP_CHAT_DEFAULTS_TO_MMS, true)

    val recipientLimit: Int
        get() = int(KEY_RECIPIENT_LIMIT, -1).takeIf { it >= 0 } ?: Int.MAX_VALUE

    val maxMessageSize: Int
        get() = int(KEY_MAX_MESSAGE_SIZE, 300 * 1024)

    val maxImageWidth: Int
        get() = int(KEY_MAX_IMAGE_WIDTH, 640)

    val maxImageHeight: Int
        get() = int(KEY_MAX_IMAGE_HEIGHT, 480)

    val multipartSmsEnabled: Boolean
        get() = bool(KEY_MULTIPART_SMS_ENABLED, true)

    val sendMultipartSmsAsSeparateMessages: Boolean
        get() = bool(KEY_SEND_MULTIPART_SMS_AS_SEPARATE_MESSAGES, false)

    val smsToMmsTextLengthThreshold: Int
        get() = int(KEY_SMS_TO_MMS_TEXT_LENGTH_THRESHOLD, -1)

    val autoRetrieveEnabled: Boolean
        get() = bool(KEY_AUTO_RETRIEVE_ENABLED, true)

    val roamingAutoRetrieveEnabled: Boolean
        get() = bool(KEY_ROAMING_AUTO_RETRIEVE, false)

    val notifyWapMmsc: Boolean
        get() = bool(KEY_NOTIFY_WAP_MMSC, false)

    val transactionIdEnabled: Boolean
        get() = bool(KEY_TRANSACTION_ID_ENABLED, false)

    val supportMmsContentDisposition: Boolean
        get() = bool(KEY_SUPPORT_CONTENT_DISPOSITION, true)

    val emailGatewayNumber: String
        get() = string(KEY_EMAIL_GATEWAY_NUMBER, "")

    val emailGatewaySmsToMmsTextThreshold: Int
        get() = int(KEY_EMAIL_GATEWAY_SMS_TO_MMS_TEXT_THRESHOLD, -1)

    val aliasEnabled: Boolean
        get() = bool(KEY_ALIAS_ENABLED, false)

    val aliasMinChars: Int
        get() = int(KEY_ALIAS_MIN_CHARS, 2)

    val aliasMaxChars: Int
        get() = int(KEY_ALIAS_MAX_CHARS, 48)

    val smsDeliveryReportsEnabled: Boolean
        get() = bool(KEY_SMS_DELIVERY_REPORTS_ENABLED, true)

    val smsDeliveryReportsByDefault: Boolean
        get() = bool(KEY_SMS_DELIVERY_REPORTS_BY_DEFAULT, false)

    val smsSimpleCharactersOnly: Boolean
        get() = bool(KEY_SMS_SIMPLE_CHARACTERS_ONLY, false)

    val useCustomUserAgent: Boolean
        get() = bool(KEY_USE_CUSTOM_USER_AGENT, false)

    val smsToMmsTextThreshold: Int
        get() =
            when {
                !mmsEnabled -> -1
                multipartSmsEnabled || sendMultipartSmsAsSeparateMessages ->
                    int(KEY_SMS_TO_MMS_TEXT_THRESHOLD, -1)
                else -> 1
            }

    private fun bool(key: String, default: Boolean) = (values.get(key) as? Boolean) ?: default

    private fun int(key: String, default: Int) = (values.get(key) as? Int) ?: default

    private fun string(key: String, default: String) =
        (values.get(key) as? String)?.takeIf { it.isNotEmpty() } ?: default

    companion object {
        const val KEY_MMS_ENABLED = "enabledMMS"
        const val KEY_GROUP_MMS_ENABLED = "enableGroupMms"
        const val KEY_GROUP_CHAT_DEFAULTS_TO_MMS = "groupChatDefaultsToMMS"
        const val KEY_RECIPIENT_LIMIT = "recipientLimit"
        const val KEY_MAX_MESSAGE_SIZE = "maxMessageSize"
        const val KEY_MAX_IMAGE_WIDTH = "maxImageWidth"
        const val KEY_MAX_IMAGE_HEIGHT = "maxImageHeight"
        const val KEY_MULTIPART_SMS_ENABLED = "enableMultipartSMS"
        const val KEY_SEND_MULTIPART_SMS_AS_SEPARATE_MESSAGES = "sendMultipartSmsAsSeparateMessages"
        const val KEY_SMS_TO_MMS_TEXT_THRESHOLD = "smsToMmsTextThreshold"
        const val KEY_SMS_TO_MMS_TEXT_LENGTH_THRESHOLD = "smsToMmsTextLengthThreshold"
        const val KEY_AUTO_RETRIEVE_ENABLED = "mms_auto_retrieve_enabled_bool"
        const val KEY_ROAMING_AUTO_RETRIEVE = "mmsRoamingAutoRetrieveByDefault"
        const val KEY_NOTIFY_WAP_MMSC = "enabledNotifyWapMMSC"
        const val KEY_TRANSACTION_ID_ENABLED = "enabledTransID"
        const val KEY_SUPPORT_CONTENT_DISPOSITION = "supportMmsContentDisposition"
        const val KEY_EMAIL_GATEWAY_NUMBER = "emailGatewayNumber"
        const val KEY_EMAIL_GATEWAY_SMS_TO_MMS_TEXT_THRESHOLD = "emailGatewaySmsToMmsTextThreshold"
        const val KEY_ALIAS_ENABLED = "aliasEnabled"
        const val KEY_ALIAS_MIN_CHARS = "aliasMinChars"
        const val KEY_ALIAS_MAX_CHARS = "aliasMaxChars"
        const val KEY_USE_CUSTOM_USER_AGENT = "useCustomUserAgent"
        const val KEY_SMS_DELIVERY_REPORTS_ENABLED = "enableSMSDeliveryReports"
        const val KEY_SMS_DELIVERY_REPORTS_BY_DEFAULT = "smsDeliveryReportSettingOnByDefault"
        const val KEY_SMS_SIMPLE_CHARACTERS_ONLY = "smsUsesSimpleCharactersOnly"

        @JvmStatic
        fun get(context: Context, subscriptionId: Int): CarrierMmsConfig {
            val subId = resolveSubscriptionId(subscriptionId)
            val values = bundled(context, subId)
            platform(subId)?.let(values::putAll)
            return CarrierMmsConfig(values)
        }

        @JvmStatic
        fun overrides(context: Context, subscriptionId: Int): Bundle {
            val subId = resolveSubscriptionId(subscriptionId)
            val platform = platform(subId)
            val bundled = bundled(context, subId)
            return Bundle().apply {
                bundled
                    .keySet()
                    .filter { key -> platform?.containsKey(key) != true }
                    .forEach { key ->
                        when (val value = bundled.get(key)) {
                            is String -> putString(key, value)
                            is Int -> putInt(key, value)
                            is Boolean -> putBoolean(key, value)
                        }
                    }
            }
        }

        @JvmStatic
        fun resolveSubscriptionId(subscriptionId: Int): Int =
            when {
                subscriptionId == -1 -> SmsManager.getDefaultSmsSubscriptionId()
                subscriptionId < 0 -> -1
                else -> subscriptionId
            }

        private fun platform(subId: Int): Bundle? =
            try {
                SmsManagerFactory.createSmsManager(subId).carrierConfigValues
            } catch (e: Exception) {
                Timber.w(e, "Unable to read the carrier config for sub $subId")
                null
            }

        private fun bundled(context: Context, subId: Int): Bundle {
            val (mcc, mnc) = carrierCode(context, subId) ?: (0 to 0)
            return bundled(context, mcc, mnc)
        }

        @JvmStatic
        fun bundled(context: Context, mcc: Int, mnc: Int): Bundle {
            val values = Bundle()
            val resources = carrierResources(context, mcc, mnc)
            readXml(resources, R.xml.mms_config, values)
            oemOverride(resources)?.let { id -> readXml(resources, id, values) }
            return values
        }

        private fun carrierCode(context: Context, subId: Int): Pair<Int, Int>? {
            val info =
                try {
                    SubscriptionManager.from(context).getActiveSubscriptionInfo(subId)
                } catch (e: Exception) {
                    null
                } ?: return null

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val mcc = info.mccString?.toIntOrNull() ?: 0
                val mnc = when (val mncString = info.mncString) {
                    null -> 0
                    else -> mncString.toIntOrNull()?.takeIf { it != 0 } ?: Configuration.MNC_ZERO
                }
                mcc to mnc
            } else {
                @Suppress("DEPRECATION")
                info.mcc to (info.mnc.takeIf { it != 0 } ?: Configuration.MNC_ZERO)
            }
        }

        private fun carrierResources(context: Context, mcc: Int, mnc: Int): Resources {
            if (mcc == 0) return context.resources

            val configuration = Configuration(context.resources.configuration)
            configuration.mcc = mcc
            configuration.mnc = mnc
            return context.createConfigurationContext(configuration).resources
        }

        private fun oemOverride(resources: Resources): Int? {
            fun isMadeBy(vararg names: String) =
                names.any { Build.MANUFACTURER.equals(it, true) || Build.BRAND.equals(it, true) }
            val name =
                when {
                    isMadeBy("MOTOROLA") -> "motorola_config_override"
                    isMadeBy("HUAWEI", "HONOR") -> "huawei_config_override"
                    isMadeBy("LGE") -> "lge_config_override"
                    else -> return null
                }
            return when (name) {
                "motorola_config_override" -> R.xml.motorola_config_override
                "huawei_config_override" -> R.xml.huawei_config_override
                else -> R.xml.lge_config_override
            }.takeIf { resources.hasXml(it) }
        }

        private fun Resources.hasXml(id: Int): Boolean =
            try {
                getXml(id).close()
                true
            } catch (e: Resources.NotFoundException) {
                false
            }

        private fun readXml(resources: Resources, id: Int, into: Bundle) {
            try {
                resources.getXml(id).use { parser -> parse(parser, into) }
            } catch (e: Resources.NotFoundException) {
            } catch (e: Exception) {
                Timber.w(e, "Unable to read MMS config")
            }
        }

        @JvmStatic
        fun parse(parser: XmlPullParser, into: Bundle) {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.depth > 1) {
                    val type = parser.name
                    val key = parser.getAttributeValue(null, "name")
                    val text = parser.nextText()?.trim().orEmpty()
                    if (key != null && text.isNotEmpty()) {
                        when (type) {
                            "bool" -> into.putBoolean(key, text.toBoolean())
                            "int" ->
                                text.toIntOrNull()?.let { into.putInt(key, it) }
                                    ?: Timber.w("Invalid int for $key: $text")
                            "string" -> into.putString(key, text)
                        }
                    }
                }
                event = parser.next()
            }
        }
    }
}
