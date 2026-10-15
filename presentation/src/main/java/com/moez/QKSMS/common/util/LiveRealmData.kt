/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.util

import androidx.lifecycle.LiveData
import io.realm.RealmChangeListener
import io.realm.RealmModel
import io.realm.RealmResults

class LiveRealmData<T : RealmModel>(private val results: RealmResults<T>) :
    LiveData<RealmResults<T>>() {

    private val listener: RealmChangeListener<RealmResults<T>> = RealmChangeListener { results ->
        value = results
    }

    override fun onActive() {
        super.onActive()
        results.addChangeListener(listener)
    }

    override fun onInactive() {
        super.onInactive()
        results.removeChangeListener(listener)
    }
}
