/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.compat

import android.telephony.SubscriptionInfo

data class SubscriptionInfoCompat(private val subscriptionInfo: SubscriptionInfo) {

    val subscriptionId
        get() = subscriptionInfo.subscriptionId

    val simSlotIndex
        get() = subscriptionInfo.simSlotIndex

    val displayName
        get() = subscriptionInfo.displayName
}
