/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
@file:Suppress("NOTHING_TO_INLINE")

package org.prauga.messages.common.androidxcompat

import androidx.annotation.CheckResult
import androidx.drawerlayout.widget.DrawerLayout
import com.jakewharton.rxbinding4.InitialValueObservable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.functions.Consumer

/**
 * Create an observable of the open state of the drawer of `view`.
 *
 * *Warning:* The created observable keeps a strong reference to `view`. Unsubscribe
 * to free this reference.
 *
 * *Note:* A value will be emitted immediately on subscribe.
 */
@CheckResult
fun DrawerLayout.drawerOpen(gravity: Int): InitialValueObservable<Boolean> {
    return DrawerOpenObservable(this, gravity)
}

/**
 * An action which sets whether the drawer with `gravity` of `view` is open.
 *
 * *Warning:* The created observable keeps a strong reference to `view`. Unsubscribe
 * to free this reference.
 */
@CheckResult
fun DrawerLayout.open(gravity: Int): Consumer<in Boolean> {
    return Consumer { open ->
        if (open) {
            openDrawer(gravity)
        } else {
            closeDrawer(gravity)
        }
    }
}

private class DrawerOpenObservable(
    private val view: DrawerLayout,
    private val gravity: Int
) : InitialValueObservable<Boolean>() {
    override fun subscribeListener(observer: Observer<in Boolean>) {
        val listener = Listener(view, gravity, observer)
        observer.onSubscribe(listener)
        view.addDrawerListener(listener)
    }

    override val initialValue: Boolean
        get() = view.isDrawerOpen(gravity)

    private class Listener(
        private val view: DrawerLayout,
        private val gravity: Int,
        private val observer: Observer<in Boolean>
    ) : DrawerLayout.DrawerListener, io.reactivex.rxjava3.disposables.Disposable {
        private var disposed = false

        override fun onDrawerSlide(drawerView: android.view.View, slideOffset: Float) {}

        override fun onDrawerOpened(drawerView: android.view.View) {
            if (!disposed && view.isDrawerOpen(gravity)) {
                observer.onNext(true)
            }
        }

        override fun onDrawerClosed(drawerView: android.view.View) {
            if (!disposed && !view.isDrawerOpen(gravity)) {
                observer.onNext(false)
            }
        }

        override fun onDrawerStateChanged(newState: Int) {}

        override fun dispose() {
            if (!disposed) {
                disposed = true
                view.removeDrawerListener(this)
            }
        }

        override fun isDisposed(): Boolean = disposed
    }
}
