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

package org.prauga.messages.extensions

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable

data class Optional<out T : Any>(val value: T?) {
    fun notNull() = value != null
}

fun <T : Any, R : Any> Flowable<T>.mapNotNull(mapper: (T) -> R?): Flowable<R> =
    map { input -> Optional(mapper(input)) }
        .filter { optional -> optional.notNull() }
        .map { optional -> optional.value!! }

fun <T : Any, R : Any> Observable<T>.mapNotNull(mapper: (T) -> R?): Observable<R> =
    map { input -> Optional(mapper(input)) }
        .filter { optional -> optional.notNull() }
        .map { optional -> optional.value!! }

fun <T : Any> Observable<T>.toFlowable(): Flowable<T> = this.toFlowable(io.reactivex.rxjava3.core.BackpressureStrategy.BUFFER)
