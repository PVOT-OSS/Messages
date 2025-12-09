package org.prauga.messages.common.util

import android.app.Activity
import org.prauga.messages.manager.BillingManager
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManagerImpl @Inject constructor(
): BillingManager {

    override val products: Observable<List<BillingManager.Product>> = BehaviorSubject.createDefault(listOf())
    override val upgradeStatus: Observable<Boolean> = BehaviorSubject.createDefault(true)

    override suspend fun checkForPurchases() = Unit
    override suspend fun queryProducts() = Unit
    override suspend fun initiatePurchaseFlow(activity: Activity, sku: String) = Unit

}