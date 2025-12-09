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

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.jakewharton.rxbinding4.view.clicks
import com.uber.autodispose2.androidx.lifecycle.scope
import com.uber.autodispose2.autoDispose
import org.prauga.messages.R
import org.prauga.messages.common.QkDialog
import org.prauga.messages.common.base.QkController
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.extensions.animateLayoutChanges
import org.prauga.messages.common.util.extensions.setBackgroundTint
import org.prauga.messages.common.util.extensions.setTint
import org.prauga.messages.injection.appComponent
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import android.widget.ImageView
import android.widget.TextView
import javax.inject.Inject

class SwipeActionsController : QkController<SwipeActionsView, SwipeActionsState, SwipeActionsPresenter>(), SwipeActionsView {

    @Inject override lateinit var presenter: SwipeActionsPresenter
    @Inject lateinit var actionsDialog: QkDialog
    @Inject lateinit var colors: Colors

    private lateinit var right: ConstraintLayout
    private lateinit var rightIcon: ImageView
    private lateinit var rightLabel: TextView
    private lateinit var left: ConstraintLayout
    private lateinit var leftIcon: ImageView
    private lateinit var leftLabel: TextView

    /**
     * Allows us to subscribe to [actionClicks] more than once
     */
    private val actionClicks: Subject<SwipeActionsView.Action> = PublishSubject.create()

    init {
        appComponent.inject(this)
        layoutRes = R.layout.swipe_actions_controller

        actionsDialog.adapter.setData(R.array.settings_swipe_actions)
    }

    override fun onViewCreated() {
        super.onViewCreated()

        val view = containerView ?: return

        right = view.findViewById(R.id.right)
        rightIcon = view.findViewById(R.id.rightIcon)
        rightLabel = view.findViewById(R.id.rightLabel)
        left = view.findViewById(R.id.left)
        leftIcon = view.findViewById(R.id.leftIcon)
        leftLabel = view.findViewById(R.id.leftLabel)

        colors.theme().let { theme ->
            rightIcon.setBackgroundTint(theme.theme)
            rightIcon.setTint(theme.textPrimary)
            leftIcon.setBackgroundTint(theme.theme)
            leftIcon.setTint(theme.textPrimary)
        }

        right.postDelayed({ right.animateLayoutChanges = true }, 100)
        left.postDelayed({ left.animateLayoutChanges = true }, 100)

        Observable.merge(
                right.clicks().map { SwipeActionsView.Action.RIGHT },
                left.clicks().map { SwipeActionsView.Action.LEFT })
                .autoDispose(scope())
                .subscribe(actionClicks)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.settings_swipe_actions)
        showBackButton(true)
    }

    override fun actionClicks(): Observable<SwipeActionsView.Action> = actionClicks

    override fun actionSelected(): Observable<Int> = actionsDialog.adapter.menuItemClicks

    override fun showSwipeActions(selected: Int) {
        actionsDialog.adapter.selectedItem = selected
        activity?.let(actionsDialog::show)
    }

    override fun render(state: SwipeActionsState) {
        rightIcon.isVisible = state.rightIcon != 0
        rightIcon.setImageResource(state.rightIcon)
        rightLabel.text = state.rightLabel

        leftIcon.isVisible = state.leftIcon != 0
        leftIcon.setImageResource(state.leftIcon)
        leftLabel.text = state.leftLabel
    }

}
