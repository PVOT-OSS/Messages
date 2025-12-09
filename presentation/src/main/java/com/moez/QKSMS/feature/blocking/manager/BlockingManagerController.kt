package org.prauga.messages.feature.blocking.manager

import android.app.Activity
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.view.View
import androidx.core.view.isInvisible
import com.jakewharton.rxbinding4.view.clicks
import org.prauga.messages.R
import org.prauga.messages.common.base.QkController
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.extensions.resolveThemeColor
import org.prauga.messages.injection.appComponent
import org.prauga.messages.util.Preferences
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.PublishSubject
import android.widget.ImageView
import javax.inject.Inject

class BlockingManagerController : QkController<BlockingManagerView, BlockingManagerState, BlockingManagerPresenter>(),
    BlockingManagerView {

    @Inject lateinit var colors: Colors
    @Inject override lateinit var presenter: BlockingManagerPresenter

    private lateinit var qksms: BlockingManagerPreferenceView
    private lateinit var callBlocker: BlockingManagerPreferenceView
    private lateinit var callControl: BlockingManagerPreferenceView
    private lateinit var shouldIAnswer: BlockingManagerPreferenceView

    private val activityResumedSubject: PublishSubject<Unit> = PublishSubject.create()

    init {
        appComponent.inject(this)
        retainViewMode = RetainViewMode.RETAIN_DETACH
        layoutRes = R.layout.blocking_manager_controller
    }

    override fun onViewCreated() {
        super.onViewCreated()

        val view = containerView ?: return

        qksms = view.findViewById(R.id.qksms)
        callBlocker = view.findViewById(R.id.callBlocker)
        callControl = view.findViewById(R.id.callControl)
        shouldIAnswer = view.findViewById(R.id.shouldIAnswer)

        val states = arrayOf(
                intArrayOf(android.R.attr.state_activated),
                intArrayOf(-android.R.attr.state_activated))

        val textTertiary = view.context.resolveThemeColor(android.R.attr.textColorTertiary)
        val imageTintList = ColorStateList(states, intArrayOf(colors.theme().theme, textTertiary))

        qksms.findViewById<ImageView>(R.id.action).imageTintList = imageTintList
        callBlocker.findViewById<ImageView>(R.id.action).imageTintList = imageTintList
        callControl.findViewById<ImageView>(R.id.action).imageTintList = imageTintList
        shouldIAnswer.findViewById<ImageView>(R.id.action).imageTintList = imageTintList
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.blocking_manager_title)
        showBackButton(true)
    }

    override fun onActivityResumed(activity: Activity) {
        activityResumedSubject.onNext(Unit)
    }

    override fun render(state: BlockingManagerState) {
        qksms.findViewById<ImageView>(R.id.action).setImageResource(getActionIcon(true))
        qksms.findViewById<ImageView>(R.id.action).isActivated = true
        qksms.findViewById<ImageView>(R.id.action).isInvisible = state.blockingManager != Preferences.BLOCKING_MANAGER_QKSMS

        callBlocker.findViewById<ImageView>(R.id.action).setImageResource(getActionIcon(state.callBlockerInstalled))
        callBlocker.findViewById<ImageView>(R.id.action).isActivated = state.callBlockerInstalled
        callBlocker.findViewById<ImageView>(R.id.action).isInvisible = state.blockingManager != Preferences.BLOCKING_MANAGER_CB
                && state.callBlockerInstalled

        callControl.findViewById<ImageView>(R.id.action).setImageResource(getActionIcon(state.callControlInstalled))
        callControl.findViewById<ImageView>(R.id.action).isActivated = state.callControlInstalled
        callControl.findViewById<ImageView>(R.id.action).isInvisible = state.blockingManager != Preferences.BLOCKING_MANAGER_CC
                && state.callControlInstalled

        shouldIAnswer.findViewById<ImageView>(R.id.action).setImageResource(getActionIcon(state.siaInstalled))
        shouldIAnswer.findViewById<ImageView>(R.id.action).isActivated = state.siaInstalled
        shouldIAnswer.findViewById<ImageView>(R.id.action).isInvisible = state.blockingManager != Preferences.BLOCKING_MANAGER_SIA
                && state.siaInstalled
    }

    private fun getActionIcon(installed: Boolean): Int = when {
        !installed -> R.drawable.ic_chevron_right_black_24dp
        else -> R.drawable.ic_check_white_24dp
    }

    override fun activityResumed(): Observable<*> = activityResumedSubject
    override fun qksmsClicked(): Observable<*> = qksms.clicks()
    override fun callBlockerClicked(): Observable<*> = callBlocker.clicks()
    override fun callControlClicked(): Observable<*> = callControl.clicks()
    override fun siaClicked(): Observable<*> = shouldIAnswer.clicks()

    override fun showCopyDialog(manager: String): Single<Boolean> = Single.create { emitter ->
        AlertDialog.Builder(activity!!, R.style.AppThemeDialog)
                .setTitle(R.string.blocking_manager_copy_title)
                .setMessage(resources?.getString(R.string.blocking_manager_copy_summary, manager))
                .setPositiveButton(R.string.button_continue) { _, _ -> emitter.onSuccess(true) }
                .setNegativeButton(R.string.button_cancel) { _, _ -> emitter.onSuccess(false) }
                .setCancelable(false)
                .show()
    }

}
