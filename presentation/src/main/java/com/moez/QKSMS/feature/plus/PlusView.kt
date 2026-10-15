/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.plus

import io.reactivex.Observable
import org.prauga.messages.common.base.QkView
import org.prauga.messages.manager.BillingManager

interface PlusView : QkView<PlusState> {

    val upgradeIntent: Observable<Unit>
    val upgradeDonateIntent: Observable<Unit>
    val donateIntent: Observable<*>
    val themeClicks: Observable<*>
    val scheduleClicks: Observable<*>
    val backupClicks: Observable<*>
    val delayedClicks: Observable<*>
    val nightClicks: Observable<*>

    fun initiatePurchaseFlow(billingManager: BillingManager, sku: String)
}
