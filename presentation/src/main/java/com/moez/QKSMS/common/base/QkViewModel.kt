/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.base

import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModel
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

abstract class QkViewModel<View, State : Any>(initialState: State) : ViewModel() where
View : QkView<State> {

    protected val disposables = CompositeDisposable()
    protected val state: Subject<State> = BehaviorSubject.createDefault(initialState)

    private val stateReducer: Subject<State.() -> State> = PublishSubject.create()

    init {
        // If we accidentally push a realm object into the state on the wrong thread, switching
        // to mainThread right here should immediately alert us of the issue
        disposables +=
            stateReducer
                .observeOn(AndroidSchedulers.mainThread())
                .scan(initialState) { state, reducer -> reducer(state) }
                .subscribe { newState -> state.onNext(newState) }
    }

    @CallSuper
    open fun bindView(view: View) {
        state
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(view.scope())
            .subscribe(view::render)
    }

    protected fun newState(reducer: State.() -> State) = stateReducer.onNext(reducer)

    override fun onCleared() = disposables.dispose()
}
