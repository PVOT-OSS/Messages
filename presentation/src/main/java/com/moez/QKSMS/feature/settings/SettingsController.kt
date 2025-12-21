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
package org.prauga.messages.feature.settings

import android.animation.ObjectAnimator
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.text.format.DateFormat
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.RouterTransaction
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.longClicks
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import org.prauga.messages.BuildConfig
import org.prauga.messages.R
import org.prauga.messages.common.MenuItem
import org.prauga.messages.common.QkChangeHandler
import org.prauga.messages.common.QkDialog
import org.prauga.messages.common.base.QkController
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.extensions.animateLayoutChanges
import org.prauga.messages.common.util.extensions.resolveThemeColor
import org.prauga.messages.common.util.extensions.setBackgroundTint
import org.prauga.messages.common.util.extensions.setVisible
import org.prauga.messages.common.widget.PreferenceView
import org.prauga.messages.common.widget.TextInputDialog
import org.prauga.messages.feature.settings.about.AboutController
import org.prauga.messages.feature.settings.autodelete.AutoDeleteDialog
import org.prauga.messages.feature.settings.swipe.SwipeActionsController
import org.prauga.messages.feature.themepicker.ThemePickerController
import org.prauga.messages.injection.appComponent
import org.prauga.messages.repository.SyncRepository
import org.prauga.messages.util.Preferences
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume

class SettingsController : QkController<SettingsView, SettingsState, SettingsPresenter>(), SettingsView {

    @Inject lateinit var context: Context
    @Inject lateinit var colors: Colors
    @Inject lateinit var nightModeDialog: QkDialog
    @Inject lateinit var sendDelayDialog: QkDialog
    @Inject lateinit var mmsSizeDialog: QkDialog
    @Inject lateinit var messageLinkHandlingDialog: QkDialog

    @Inject override lateinit var presenter: SettingsPresenter

    private lateinit var preferences: LinearLayout
    private lateinit var contentView: ScrollView
    private lateinit var themePreview: View
    private lateinit var theme: PreferenceView
    private lateinit var night: PreferenceView
    private lateinit var nightStart: PreferenceView
    private lateinit var nightEnd: PreferenceView
    private lateinit var autoEmoji: PreferenceView
    private lateinit var delayed: PreferenceView
    private lateinit var delivery: PreferenceView
    private lateinit var unreadAtTop: PreferenceView
    private lateinit var signature: PreferenceView
    private lateinit var systemFont: PreferenceView
    private lateinit var unicode: PreferenceView
    private lateinit var mobileOnly: PreferenceView
    private lateinit var autoDelete: PreferenceView
    private lateinit var longAsMms: PreferenceView
    private lateinit var mmsSize: PreferenceView
    private lateinit var messsageLinkHandling: PreferenceView
    private lateinit var disableScreenshots: PreferenceView
    private lateinit var swipeActions: PreferenceView
    private lateinit var about: PreferenceView
    private lateinit var syncingProgress: ProgressBar

    private val signatureDialog: TextInputDialog by lazy {
        TextInputDialog(activity!!, context.getString(R.string.settings_signature_title), signatureSubject::onNext)
    }
    private val autoDeleteDialog: AutoDeleteDialog by lazy {
        AutoDeleteDialog(activity!!, autoDeleteSubject::onNext)
    }

    private val viewQksmsPlusSubject: Subject<Unit> = PublishSubject.create()
    private val startTimeSelectedSubject: Subject<Pair<Int, Int>> = PublishSubject.create()
    private val endTimeSelectedSubject: Subject<Pair<Int, Int>> = PublishSubject.create()
    private val signatureSubject: Subject<String> = PublishSubject.create()
    private val autoDeleteSubject: Subject<Int> = PublishSubject.create()

    private val progressAnimator by lazy {
        if (::syncingProgress.isInitialized) {
            ObjectAnimator.ofInt(syncingProgress, "progress", 0, 0)
        } else {
            null
        }
    }

    init {
        appComponent.inject(this)
        retainViewMode = RetainViewMode.RETAIN_DETACH
        layoutRes = R.layout.settings_controller

        colors.themeObservable()
                .autoDisposable(scope())
                .subscribe { activity?.recreate() }
    }

    override fun onViewCreated() {
        val view = containerView ?: return

        preferences = view.findViewById(R.id.preferences)
        contentView = view.findViewById(R.id.contentView)
        themePreview = view.findViewById(R.id.themePreview)
        theme = view.findViewById(R.id.theme)
        night = view.findViewById(R.id.night)
        nightStart = view.findViewById(R.id.nightStart)
        nightEnd = view.findViewById(R.id.nightEnd)
        autoEmoji = view.findViewById(R.id.autoEmoji)
        delayed = view.findViewById(R.id.delayed)
        delivery = view.findViewById(R.id.delivery)
        unreadAtTop = view.findViewById(R.id.unreadAtTop)
        signature = view.findViewById(R.id.signature)
        systemFont = view.findViewById(R.id.systemFont)
        unicode = view.findViewById(R.id.unicode)
        mobileOnly = view.findViewById(R.id.mobileOnly)
        autoDelete = view.findViewById(R.id.autoDelete)
        longAsMms = view.findViewById(R.id.longAsMms)
        mmsSize = view.findViewById(R.id.mmsSize)
        messsageLinkHandling = view.findViewById(R.id.messsageLinkHandling)
        disableScreenshots = view.findViewById(R.id.disableScreenshots)
        swipeActions = view.findViewById(R.id.swipeActions)
        about = view.findViewById(R.id.about)
        syncingProgress = view.findViewById(R.id.syncingProgress)

        preferences.postDelayed({ preferences.animateLayoutChanges = true }, 100)

        when (Build.VERSION.SDK_INT >= 29) {
            true -> nightModeDialog.adapter.setData(R.array.night_modes)
            false -> nightModeDialog.adapter.data = context.resources.getStringArray(R.array.night_modes)
                    .mapIndexed { index, title -> MenuItem(title, index) }
                    .drop(1)
        }
        sendDelayDialog.adapter.setData(R.array.delayed_sending_labels)
        mmsSizeDialog.adapter.setData(R.array.mms_sizes, R.array.mms_sizes_ids)
        messageLinkHandlingDialog.adapter.setData(R.array.messageLinkHandlings, R.array.messageLinkHandling_ids)

        about.summary = context.getString(R.string.settings_version, BuildConfig.VERSION_NAME)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.title_settings)
        showBackButton(true)
    }

    override fun preferenceClicks(): Observable<PreferenceView> = (0 until preferences.childCount)
            .map { index -> preferences.getChildAt(index) }
            .mapNotNull { view -> view as? PreferenceView }
            .map { preference -> preference.clicks().map { preference } }
            .let { preferences -> Observable.merge(preferences) }

    override fun aboutLongClicks(): Observable<*> = about.longClicks()

    override fun viewQksmsPlusClicks(): Observable<*> = viewQksmsPlusSubject

    override fun nightModeSelected(): Observable<Int> = nightModeDialog.adapter.menuItemClicks

    override fun nightStartSelected(): Observable<Pair<Int, Int>> = startTimeSelectedSubject

    override fun nightEndSelected(): Observable<Pair<Int, Int>> = endTimeSelectedSubject

    override fun sendDelaySelected(): Observable<Int> = sendDelayDialog.adapter.menuItemClicks

    override fun signatureChanged(): Observable<String> = signatureSubject

    override fun autoDeleteChanged(): Observable<Int> = autoDeleteSubject

    override fun mmsSizeSelected(): Observable<Int> = mmsSizeDialog.adapter.menuItemClicks

    override fun messageLinkHandlingSelected(): Observable<Int> = messageLinkHandlingDialog.adapter.menuItemClicks

    private fun PreferenceView.checkbox(): CompoundButton? = findViewById(R.id.checkbox)

    override fun render(state: SettingsState) {
        themePreview.setBackgroundTint(state.theme)
        night.summary = state.nightModeSummary
        nightModeDialog.adapter.selectedItem = state.nightModeId
        nightStart.setVisible(state.nightModeId == Preferences.NIGHT_MODE_AUTO)
        nightStart.summary = state.nightStart
        nightEnd.setVisible(state.nightModeId == Preferences.NIGHT_MODE_AUTO)
        nightEnd.summary = state.nightEnd

        autoEmoji.checkbox()?.isChecked = state.autoEmojiEnabled

        delayed.summary = state.sendDelaySummary
        sendDelayDialog.adapter.selectedItem = state.sendDelayId

        delivery.checkbox()?.isChecked = state.deliveryEnabled

        unreadAtTop.checkbox()?.isChecked = state.unreadAtTopEnabled

        signature.summary = state.signature.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.settings_signature_summary)

        systemFont.checkbox()?.isChecked = state.systemFontEnabled

        unicode.checkbox()?.isChecked = state.stripUnicodeEnabled
        mobileOnly.checkbox()?.isChecked = state.mobileOnly

        autoDelete.summary = when (state.autoDelete) {
            0 -> context.getString(R.string.settings_auto_delete_never)
            else -> context.resources.getQuantityString(
                    R.plurals.settings_auto_delete_summary, state.autoDelete, state.autoDelete)
        }

        longAsMms.checkbox()?.isChecked = state.longAsMms

        mmsSize.summary = state.maxMmsSizeSummary
        mmsSizeDialog.adapter.selectedItem = state.maxMmsSizeId

        messsageLinkHandling.summary = state.messageLinkHandlingSummary
        messageLinkHandlingDialog.adapter.selectedItem = state.messageLinkHandlingId

        disableScreenshots.checkbox()?.isChecked = state.disableScreenshotsEnabled

        when (state.syncProgress) {
            is SyncRepository.SyncProgress.Idle -> syncingProgress.isVisible = false

            is SyncRepository.SyncProgress.Running -> {
                syncingProgress.isVisible = true
                syncingProgress.max = state.syncProgress.max
                progressAnimator?.apply { setIntValues(syncingProgress.progress, state.syncProgress.progress) }?.start()
                syncingProgress.isIndeterminate = state.syncProgress.indeterminate
            }
        }
    }

    override fun showQksmsPlusSnackbar() {
        view?.run {
            Snackbar.make(contentView, R.string.toast_qksms_plus, Snackbar.LENGTH_LONG).run {
                setAction(R.string.button_more) { viewQksmsPlusSubject.onNext(Unit) }
                setActionTextColor(colors.theme().theme)
                show()
            }
        }
    }

    // TODO change this to a PopupWindow
    override fun showNightModeDialog() = nightModeDialog.show(activity!!)

    override fun showStartTimePicker(hour: Int, minute: Int) {
        TimePickerDialog(activity, { _, newHour, newMinute ->
            startTimeSelectedSubject.onNext(Pair(newHour, newMinute))
        }, hour, minute, DateFormat.is24HourFormat(activity)).apply {
            tintDialogButtons(this)
            show()
        }
    }

    override fun showEndTimePicker(hour: Int, minute: Int) {
        TimePickerDialog(activity, { _, newHour, newMinute ->
            endTimeSelectedSubject.onNext(Pair(newHour, newMinute))
        }, hour, minute, DateFormat.is24HourFormat(activity)).apply {
            tintDialogButtons(this)
            show()
        }
    }

    override fun showDelayDurationDialog() = sendDelayDialog.show(activity!!)

    override fun showSignatureDialog(signature: String) = signatureDialog.setText(signature).show()

    override fun showAutoDeleteDialog(days: Int) = autoDeleteDialog.setExpiry(days).show()

    override suspend fun showAutoDeleteWarningDialog(messages: Int): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Boolean> { cont ->
            val dialog = AlertDialog.Builder(activity!!, R.style.AppThemeDialog)
                    .setTitle(R.string.settings_auto_delete_warning)
                    .setMessage(context.resources.getString(R.string.settings_auto_delete_warning_message, messages))
                    .setOnCancelListener { cont.resume(false) }
                    .setNegativeButton(R.string.button_cancel) { _, _ -> cont.resume(false) }
                    .setPositiveButton(R.string.button_yes) { _, _ -> cont.resume(true) }
                    .create()

            dialog.show()

            themedActivity?.theme?.take(1)
                    ?.autoDisposable(scope())
                    ?.subscribe { theme ->
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(theme.theme)
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(theme.theme)
                    }
        }
    }

    override fun showMmsSizePicker() = mmsSizeDialog.show(activity!!)

    override fun showMessageLinkHandlingDialogPicker() = messageLinkHandlingDialog.show(activity!!)

    override fun showSwipeActions() {
        router.pushController(RouterTransaction.with(SwipeActionsController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler()))
    }

    override fun showThemePicker() {
        router.pushController(RouterTransaction.with(ThemePickerController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler()))
    }

    override fun showAbout() {
        router.pushController(RouterTransaction.with(AboutController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler()))
    }

    private fun tintDialogButtons(dialog: android.app.TimePickerDialog) {
        dialog.setOnShowListener {
            val color = activity?.resolveThemeColor(android.R.attr.textColorPrimary, colors.theme().textPrimary)
                ?: colors.theme().textPrimary
            listOf(
                DialogInterface.BUTTON_POSITIVE,
                DialogInterface.BUTTON_NEGATIVE,
                DialogInterface.BUTTON_NEUTRAL
            ).forEach { type ->
                dialog.getButton(type)?.setTextColor(color)
            }
        }
    }
}
