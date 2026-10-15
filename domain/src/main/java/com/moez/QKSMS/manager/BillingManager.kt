/*
 * SPDX-FileCopyrightText: 2021 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.manager

import android.app.Activity
import io.reactivex.Observable

interface BillingManager {

    companion object {
        const val SKU_PLUS = "remove_ads"
        const val SKU_PLUS_DONATE = "qksms_plus_donate"
    }

    data class Product(val sku: String, val price: String, val priceCurrencyCode: String)

    val products: Observable<List<Product>>
    val upgradeStatus: Observable<Boolean>

    suspend fun checkForPurchases()

    suspend fun queryProducts()

    suspend fun initiatePurchaseFlow(activity: Activity, sku: String)
}
