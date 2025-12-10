package com.moez.QKSMS.util

import com.f2prateek.rx.preferences2.Preference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.rx2.asFlow

class FlowPreference<T : Any>(
    private val pref: Preference<T>
) {
    val flow: Flow<T> = pref.asObservable().asFlow()

    fun get(): T = pref.get()
    fun set(value: T) = pref.set(value)
}
