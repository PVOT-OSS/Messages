package com.moez.QKSMS.common.base

import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import org.prauga.messages.common.base.QkView

abstract class PvotViewModel<State : Any>(
    initialState: State
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state

    private val reducers = MutableSharedFlow<State.() -> State>()

    init {
        viewModelScope.launch {
            reducers
                .scan(initialState) { current, reducer -> reducer(current) }
                .collect { newState -> _state.value = newState }
        }
    }

    protected fun newState(reducer: State.() -> State) {
        viewModelScope.launch {
            reducers.emit(reducer)
        }
    }

    @CallSuper
    open fun bindView(view: QkView<State>) {
        viewModelScope.launch {
            state.collect { view.render(it) }
        }
    }
}