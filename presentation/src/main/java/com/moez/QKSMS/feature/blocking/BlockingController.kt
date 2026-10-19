/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.blocking

import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import com.bluelinelabs.conductor.RouterTransaction
import com.jakewharton.rxbinding2.view.clicks
import javax.inject.Inject
import org.prauga.messages.R
import org.prauga.messages.common.QkChangeHandler
import org.prauga.messages.common.base.QkController
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.extensions.animateLayoutChanges
import org.prauga.messages.common.widget.PreferenceView
import org.prauga.messages.feature.blocking.filters.MessageContentFiltersController
import org.prauga.messages.feature.blocking.manager.BlockingManagerController
import org.prauga.messages.feature.blocking.messages.BlockedMessagesController
import org.prauga.messages.feature.blocking.numbers.BlockedNumbersController
import org.prauga.messages.injection.appComponent

class BlockingController :
    QkController<BlockingView, BlockingState, BlockingPresenter>(), BlockingView {

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
        router.pushController(
            RouterTransaction.with(BlockedNumbersController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler())
        )
    }

    override fun openMessageContentFilters() {
        router.pushController(
            RouterTransaction.with(MessageContentFiltersController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler())
        )
    }

    override fun openBlockedMessages() {
        router.pushController(
            RouterTransaction.with(BlockedMessagesController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler())
        )
    }

    override fun openBlockingManager() {
        router.pushController(
            RouterTransaction.with(BlockingManagerController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler())
        )
    }
}
