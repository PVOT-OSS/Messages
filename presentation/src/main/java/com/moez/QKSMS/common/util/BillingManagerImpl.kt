package org.prauga.messages.common.util

import android.app.Activity
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import org.prauga.messages.manager.BillingManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManagerImpl @Inject constructor(
) : BillingManager {

    override val products: Observable<List<BillingManager.Product>> =
        BehaviorSubject.createDefault(listOf())
    override val upgradeStatus: Observable<Boolean> = BehaviorSubject.createDefault(true)

    override suspend fun checkForPurchases() = Unit
    override suspend fun queryProducts() = Unit
    override suspend fun initiatePurchaseFlow(activity: Activity, sku: String) = Unit

}