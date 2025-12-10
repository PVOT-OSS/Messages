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
package org.prauga.messages.feature.settings.swipe

import android.content.Context
import androidx.annotation.DrawableRes
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import org.prauga.messages.R
import org.prauga.messages.common.base.QkPresenter
import org.prauga.messages.util.Preferences
import io.reactivex.rxkotlin.plusAssign
import javax.inject.Inject

class SwipeActionsPresenter @Inject constructor(
    context: Context,
    private val prefs: Preferences
) : QkPresenter<SwipeActionsView, SwipeActionsState>(SwipeActionsState()) {

    init {
        val actionLabels = context.resources.getStringArray(R.array.settings_swipe_actions)

        disposables += prefs.swipeRight.asObservable()
                .subscribe { action -> newState { copy(rightLabel = actionLabels[action], rightIcon = iconForAction(action)) } }

        disposables += prefs.swipeLeft.asObservable()
                .subscribe { action -> newState { copy(leftLabel = actionLabels[action], leftIcon = iconForAction(action)) } }
    }

    override fun bindIntents(view: SwipeActionsView) {
        super.bindIntents(view)

        view.actionClicks()
            .map { action ->
                when (action) {
                    SwipeActionsView.Action.RIGHT -> prefs.swipeRight.get()
                    SwipeActionsView.Action.LEFT -> prefs.swipeLeft.get()
                }
            }
            .autoDispose(view.scope())
            .subscribe(view::showSwipeActions)

        view.actionSelected()
            .withLatestFrom(view.actionClicks()) { actionId, action ->
                when (action) {
                    SwipeActionsView.Action.RIGHT -> prefs.swipeRight.set(actionId)
                    SwipeActionsView.Action.LEFT -> prefs.swipeLeft.set(actionId)
                }
            }
            .autoDispose(view.scope())
            .subscribe()
    }

    @DrawableRes
    private fun iconForAction(action: Int) = when (action) {
        Preferences.SWIPE_ACTION_ARCHIVE -> R.drawable.ic_archive_white_24dp
        Preferences.SWIPE_ACTION_DELETE -> R.drawable.ic_delete_white_24dp
        Preferences.SWIPE_ACTION_BLOCK -> R.drawable.ic_block_white_24dp
        Preferences.SWIPE_ACTION_CALL -> R.drawable.ic_call_white_24dp
        Preferences.SWIPE_ACTION_READ -> R.drawable.ic_check_white_24dp
        Preferences.SWIPE_ACTION_UNREAD -> R.drawable.ic_markunread_black_24dp
        Preferences.SWIPE_ACTION_SPEAK -> R.drawable.ic_speaker_black_24dp
        else -> 0
    }

}