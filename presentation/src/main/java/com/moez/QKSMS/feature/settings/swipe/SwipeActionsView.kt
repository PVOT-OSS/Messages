/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.settings.swipe

import io.reactivex.Observable
import org.prauga.messages.common.base.QkViewContract

interface SwipeActionsView : QkViewContract<SwipeActionsState> {

    enum class Action {
        LEFT,
        RIGHT,
    }

    fun actionClicks(): Observable<Action>

    fun actionSelected(): Observable<Int>

    fun showSwipeActions(selected: Int)
}
