/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.DisposableSubscriber
import timber.log.Timber

abstract class Interactor<in Params> : Disposable {

    private val disposables: CompositeDisposable = CompositeDisposable()

    abstract fun buildObservable(params: Params): Flowable<*>

    internal val pendingRuns: Int
        get() = disposables.size()

    fun execute(params: Params, onComplete: () -> Unit = {}) {
        val run =
            buildObservable(params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(onComplete)
                .subscribeWith(Run())
        disposables += run
        if (run.isDisposed) disposables.delete(run)
    }

    private inner class Run : DisposableSubscriber<Any?>() {
        override fun onNext(t: Any?) = Unit

        override fun onError(t: Throwable) {
            Timber.w(t)
            finish()
        }

        override fun onComplete() = finish()

        private fun finish() {
            dispose()
            disposables.delete(this)
        }
    }

    override fun dispose() {
        return disposables.dispose()
    }

    override fun isDisposed(): Boolean {
        return disposables.isDisposed
    }
}
