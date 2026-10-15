/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.util

import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import org.prauga.messages.compat.SubscriptionInfoCompat
import org.prauga.messages.compat.SubscriptionManagerCompat

class ActiveSubscriptionObservable(private val subscriptionManager: SubscriptionManagerCompat) :
    Observable<List<SubscriptionInfoCompat>>() {

    override fun subscribeActual(observer: Observer<in List<SubscriptionInfoCompat>>) {
        observer.onNext(subscriptionManager.activeSubscriptionInfoList)

        val listener = Listener(subscriptionManager, observer)
        observer.onSubscribe(listener)
        subscriptionManager.addOnSubscriptionsChangedListener(listener)
    }

    internal class Listener(
        private val subscriptionManager: SubscriptionManagerCompat,
        private val observer: Observer<in List<SubscriptionInfoCompat>>,
    ) : Disposable, SubscriptionManagerCompat.OnSubscriptionsChangedListener() {

        private var disposed: Boolean = false

        override fun onSubscriptionsChanged() {
            if (!isDisposed) {
                observer.onNext(subscriptionManager.activeSubscriptionInfoList)
            }
        }

        override fun isDisposed(): Boolean = disposed

        override fun dispose() {
            disposed = true
            subscriptionManager.removeOnSubscriptionsChangedListener(this)
        }
    }
}
