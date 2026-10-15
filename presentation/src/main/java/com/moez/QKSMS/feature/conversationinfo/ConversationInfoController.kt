/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.conversationinfo

import android.view.View
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bluelinelabs.conductor.RouterTransaction
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import org.prauga.messages.R
import org.prauga.messages.common.Navigator
import org.prauga.messages.common.QkChangeHandler
import org.prauga.messages.common.base.QkController
import org.prauga.messages.common.util.extensions.scrapViews
import org.prauga.messages.common.widget.TextInputDialog
import org.prauga.messages.feature.blocking.BlockingDialog
import org.prauga.messages.feature.conversationinfo.injection.ConversationInfoModule
import org.prauga.messages.feature.themepicker.ThemePickerController
import org.prauga.messages.injection.appComponent

class ConversationInfoController(val threadId: Long = 0) :
    QkController<ConversationInfoView, ConversationInfoState, ConversationInfoPresenter>(),
    ConversationInfoView {

    @Inject override lateinit var presenter: ConversationInfoPresenter
    @Inject lateinit var blockingDialog: BlockingDialog
    @Inject lateinit var navigator: Navigator
    @Inject lateinit var adapter: ConversationInfoAdapter

    private lateinit var recyclerView: RecyclerView

    private val nameDialog: TextInputDialog by lazy {
        TextInputDialog(
            activity!!,
            activity!!.getString(R.string.info_name),
            nameChangeSubject::onNext,
        )
    }

    private val nameChangeSubject: Subject<String> = PublishSubject.create()
    private val confirmDeleteSubject: Subject<Unit> = PublishSubject.create()

    init {
        appComponent
            .conversationInfoBuilder()
            .conversationInfoModule(ConversationInfoModule(this))
            .build()
            .inject(this)

        layoutRes = R.layout.conversation_info_controller
    }

    override fun onViewCreated() {
        super.onViewCreated()

        val view = containerView ?: return

        recyclerView = view.findViewById(R.id.recyclerView)

        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(GridSpacingItemDecoration(adapter, activity!!))
        recyclerView.layoutManager =
            GridLayoutManager(activity, 3).apply {
                spanSizeLookup =
                    object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int =
                            if (adapter.getItemViewType(position) == 2) 1 else 3
                    }
            }

        themedActivity?.theme?.autoDispose(scope())?.subscribe { recyclerView.scrapViews() }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.info_title)
        showBackButton(true)
    }

    override fun render(state: ConversationInfoState) {
        if (state.hasError) {
            activity?.finish()
            return
        }

        adapter.data = state.data
    }

    override fun recipientClicks(): Observable<Long> = adapter.recipientClicks

    override fun recipientLongClicks(): Observable<Long> = adapter.recipientLongClicks

    override fun themeClicks(): Observable<Long> = adapter.themeClicks

    override fun nameClicks(): Observable<*> = adapter.nameClicks

    override fun nameChanges(): Observable<String> = nameChangeSubject

    override fun notificationClicks(): Observable<*> = adapter.notificationClicks

    override fun markUnreadClicks(): Observable<*> = adapter.markUnreadClicks

    override fun archiveClicks(): Observable<*> = adapter.archiveClicks

    override fun blockClicks(): Observable<*> = adapter.blockClicks

    override fun deleteClicks(): Observable<*> = adapter.deleteClicks

    override fun confirmDelete(): Observable<*> = confirmDeleteSubject

    override fun mediaClicks(): Observable<Long> = adapter.mediaClicks

    override fun showNameDialog(name: String) = nameDialog.setText(name).show()

    override fun showThemePicker(recipientId: Long) {
        router.pushController(
            RouterTransaction.with(ThemePickerController(recipientId))
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler())
        )
    }

    override fun showBlockingDialog(conversations: List<Long>, block: Boolean) {
        blockingDialog.show(activity!! as ComponentActivity, conversations, block)
    }

    override fun requestDefaultSms() {
        navigator.showDefaultSmsDialog(activity!!)
    }

    override fun showDeleteDialog() {
        val dialog =
            AlertDialog.Builder(activity!!, R.style.AppThemeDialog)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(resources?.getQuantityString(R.plurals.dialog_delete_message, 1))
                .setPositiveButton(R.string.button_delete) { _, _ ->
                    confirmDeleteSubject.onNext(Unit)
                }
                .setNegativeButton(R.string.button_cancel, null)
                .create()

        dialog.show()

        themedActivity?.theme?.take(1)?.autoDispose(scope())?.subscribe { theme ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(theme.theme)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(theme.theme)
        }
    }
}
