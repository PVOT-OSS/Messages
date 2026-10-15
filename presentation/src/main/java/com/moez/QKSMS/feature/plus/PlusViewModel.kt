/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.plus

import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import javax.inject.Inject
import org.prauga.messages.common.Navigator
import org.prauga.messages.common.base.QkViewModel
import org.prauga.messages.manager.BillingManager

class PlusViewModel
@Inject
constructor(private val billingManager: BillingManager, private val navigator: Navigator) :
    QkViewModel<PlusView, PlusState>(PlusState()) {

    init {
        disposables +=
            billingManager.upgradeStatus.subscribe { upgraded ->
                newState { copy(upgraded = upgraded) }
            }

        disposables +=
            billingManager.products.subscribe { products ->
                newState {
                    val upgrade = products.firstOrNull { it.sku == BillingManager.SKU_PLUS }
                    val upgradeDonate =
                        products.firstOrNull { it.sku == BillingManager.SKU_PLUS_DONATE }
                    copy(
                        upgradePrice = upgrade?.price ?: "",
                        upgradeDonatePrice = upgradeDonate?.price ?: "",
                        currency =
                            upgrade?.priceCurrencyCode ?: upgradeDonate?.priceCurrencyCode ?: "",
                    )
                }
            }
    }

    override fun bindView(view: PlusView) {
        super.bindView(view)

        Observable.merge(
                view.upgradeIntent.map { BillingManager.SKU_PLUS },
                view.upgradeDonateIntent.map { BillingManager.SKU_PLUS_DONATE },
            )
            .autoDispose(view.scope())
            .subscribe { sku -> view.initiatePurchaseFlow(billingManager, sku) }

        //        view.donateIntent
        //                .autoDisposable(view.scope())
        //                .subscribe { navigator.showDonation() }

        view.themeClicks.autoDispose(view.scope()).subscribe { navigator.showSettings() }

        view.scheduleClicks.autoDispose(view.scope()).subscribe { navigator.showScheduled(null) }

        view.backupClicks.autoDispose(view.scope()).subscribe { navigator.showBackup() }

        view.delayedClicks.autoDispose(view.scope()).subscribe { navigator.showSettings() }

        view.nightClicks.autoDispose(view.scope()).subscribe { navigator.showSettings() }
    }
}
