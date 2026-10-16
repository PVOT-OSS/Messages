/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.compat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import javax.inject.Inject

class SubscriptionManagerCompat
@Inject
constructor(private val context: Context) {

    private val subscriptionManager: SubscriptionManager?
        get() = field?.takeIf { hasPhone() }

    val activeSubscriptionInfoList: List<SubscriptionInfoCompat>
        get() {
            return if (hasPhone()) {
                try {
                    subscriptionManager?.activeSubscriptionInfoList?.map {
                        SubscriptionInfoCompat(it)
                    } ?: listOf()
                } catch (e: SecurityException) {
                    // Handle exception if permission is not granted
                    listOf()
                }
            } else {
                listOf()
            }
        }

    val defaultSmsSubscriptionId: Int
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                SubscriptionManager.getDefaultSmsSubscriptionId()
            else SubscriptionManager.INVALID_SUBSCRIPTION_ID

    init {
        subscriptionManager = SubscriptionManager.from(context)
    }

    private fun hasPhone(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    fun addOnSubscriptionsChangedListener(listener: OnSubscriptionsChangedListener) {
        subscriptionManager?.addOnSubscriptionsChangedListener(listener.listener)
    }

    fun removeOnSubscriptionsChangedListener(listener: OnSubscriptionsChangedListener) {
        subscriptionManager?.removeOnSubscriptionsChangedListener(listener.listener)
    }

    abstract class OnSubscriptionsChangedListener {

        val listener: SubscriptionManager.OnSubscriptionsChangedListener? =
            object : SubscriptionManager.OnSubscriptionsChangedListener() {
                override fun onSubscriptionsChanged() {
                    this@OnSubscriptionsChangedListener.onSubscriptionsChanged()
                }
            }

        abstract fun onSubscriptionsChanged()
    }

    companion object {
        fun preferredSubscription(
            subs: List<SubscriptionInfoCompat>,
            lastUsedSubId: Int,
            defaultSmsSubId: Int,
        ): SubscriptionInfoCompat? =
            subs.firstOrNull { it.subscriptionId == lastUsedSubId }
                ?: subs.firstOrNull { it.subscriptionId == defaultSmsSubId }
                ?: subs.firstOrNull()
    }
}
