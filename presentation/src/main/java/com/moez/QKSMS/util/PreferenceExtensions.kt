package com.moez.QKSMS.util

import com.f2prateek.rx.preferences2.Preference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.rx2.asFlow

fun <T : Any> Preference<T>.asFlow(): Flow<T> =
    this.asObservable().asFlow()