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
package dev.danascape.messages.feature.blocking

import android.view.View
import com.bluelinelabs.conductor.RouterTransaction
import com.jakewharton.rxbinding2.view.clicks
import dev.danascape.messages.R
import dev.danascape.messages.common.QkChangeHandler
import dev.danascape.messages.common.base.QkController
import dev.danascape.messages.common.util.Colors
import dev.danascape.messages.common.util.extensions.animateLayoutChanges
import dev.danascape.messages.feature.blocking.manager.BlockingManagerController
import dev.danascape.messages.feature.blocking.messages.BlockedMessagesController
import dev.danascape.messages.feature.blocking.numbers.BlockedNumbersController
import dev.danascape.messages.feature.blocking.filters.MessageContentFiltersController
import dev.danascape.messages.injection.appComponent
import android.widget.CompoundButton
import android.widget.LinearLayout
import dev.danascape.messages.common.widget.PreferenceView
import javax.inject.Inject

class BlockingController : QkController<BlockingView, BlockingState, BlockingPresenter>(), BlockingView {

    @Inject lateinit var colors: Colors
    @Inject override lateinit var presenter: BlockingPresenter

    private lateinit var parent: LinearLayout
    private lateinit var blockingManager: PreferenceView
    private lateinit var blockedNumbers: PreferenceView
    private lateinit var messageContentFilters: PreferenceView
    private lateinit var blockedMessages: PreferenceView
    private lateinit var drop: PreferenceView

    override val blockingManagerIntent by lazy { blockingManager.clicks() }
    override val blockedNumbersIntent by lazy { blockedNumbers.clicks() }
    override val messageContentFiltersIntent by lazy { messageContentFilters.clicks() }
    override val blockedMessagesIntent by lazy { blockedMessages.clicks() }
    override val dropClickedIntent by lazy { drop.clicks() }

    init {
        appComponent.inject(this)
        retainViewMode = RetainViewMode.RETAIN_DETACH
        layoutRes = R.layout.blocking_controller
    }

    override fun onViewCreated() {
        super.onViewCreated()

        val view = containerView ?: return

        parent = view.findViewById(R.id.parent)
        blockingManager = view.findViewById(R.id.blockingManager)
        blockedNumbers = view.findViewById(R.id.blockedNumbers)
        messageContentFilters = view.findViewById(R.id.messageContentFilters)
        blockedMessages = view.findViewById(R.id.blockedMessages)
        drop = view.findViewById(R.id.drop)

        parent.postDelayed({ parent.animateLayoutChanges = true }, 100)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.blocking_title)
        showBackButton(true)
    }

    override fun render(state: BlockingState) {
        blockingManager.summary = state.blockingManager
        drop.findViewById<CompoundButton>(R.id.checkbox)?.isChecked = state.dropEnabled
        blockedMessages.isEnabled = !state.dropEnabled
    }

    override fun openBlockedNumbers() {
        router.pushController(RouterTransaction.with(BlockedNumbersController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler()))
    }

    override fun openMessageContentFilters() {
        router.pushController(RouterTransaction.with(MessageContentFiltersController())
            .pushChangeHandler(QkChangeHandler())
            .popChangeHandler(QkChangeHandler()))
    }

    override fun openBlockedMessages() {
        router.pushController(RouterTransaction.with(BlockedMessagesController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler()))
    }

    override fun openBlockingManager() {
        router.pushController(RouterTransaction.with(BlockingManagerController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler()))
    }

}
