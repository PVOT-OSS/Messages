/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.util

import android.content.Context
import javax.inject.Inject
import org.prauga.messages.carrier.CarrierMmsConfig

class SmsSettings
@Inject
constructor(private val context: Context, private val prefs: Preferences) {

    fun deliveryReports(subId: Int): Boolean {
        val config = CarrierMmsConfig.get(context, subId)
        if (!config.smsDeliveryReportsEnabled) return false
        return if (prefs.delivery.isSet) prefs.delivery.get()
        else config.smsDeliveryReportsByDefault
    }

    fun groupMessaging(subId: Int): Boolean {
        val config = CarrierMmsConfig.get(context, subId)
        if (!config.groupMmsEnabled) return false
        return if (prefs.sendAsGroup.isSet) prefs.sendAsGroup.get()
        else config.groupChatDefaultsToMms
    }

    fun simpleCharacters(subId: Int): Boolean =
        if (prefs.unicode.isSet) prefs.unicode.get()
        else CarrierMmsConfig.get(context, subId).smsSimpleCharactersOnly
}
