/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.settings

import android.content.Context
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.prauga.messages.R
import org.prauga.messages.carrier.CarrierMmsConfig
import org.prauga.messages.common.Navigator
import org.prauga.messages.common.base.QkPresenter
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.DateFormatter
import org.prauga.messages.common.util.extensions.makeToast
import org.prauga.messages.interactor.DeleteOldMessages
import org.prauga.messages.interactor.SyncMessages
import org.prauga.messages.manager.BillingManager
import org.prauga.messages.repository.MessageRepository
import org.prauga.messages.repository.SyncRepository
import org.prauga.messages.service.AutoDeleteService
import org.prauga.messages.util.NightModeManager
import org.prauga.messages.util.Preferences
import org.prauga.messages.util.SmsSettings
import timber.log.Timber

class SettingsPresenter
@Inject
constructor(
    colors: Colors,
    syncRepo: SyncRepository,
    private val context: Context,
    private val billingManager: BillingManager,
    private val dateFormatter: DateFormatter,
    private val deleteOldMessages: DeleteOldMessages,
    private val messageRepo: MessageRepository,
    private val navigator: Navigator,
    private val nightModeManager: NightModeManager,
    private val prefs: Preferences,
    private val smsSettings: SmsSettings,
    private val syncMessages: SyncMessages,
) : QkPresenter<SettingsView, SettingsState>(SettingsState(nightModeId = prefs.nightMode.get())) {

    init {
        disposables +=
            colors.themeObservable().subscribe { theme -> newState { copy(theme = theme.theme) } }

        val nightModeLabels = context.resources.getStringArray(R.array.night_modes)
        disposables +=
            prefs.nightMode.asObservable().subscribe { nightMode ->
                newState {
                    copy(nightModeSummary = nightModeLabels[nightMode], nightModeId = nightMode)
                }
            }

        disposables +=
            prefs.nightStart
                .asObservable()
                .map { time -> nightModeManager.parseTime(time) }
                .map { calendar -> calendar.timeInMillis }
                .map { millis -> dateFormatter.getTimestamp(millis) }
                .subscribe { nightStart -> newState { copy(nightStart = nightStart) } }

        disposables +=
            prefs.nightEnd
                .asObservable()
                .map { time -> nightModeManager.parseTime(time) }
                .map { calendar -> calendar.timeInMillis }
                .map { millis -> dateFormatter.getTimestamp(millis) }
                .subscribe { nightEnd -> newState { copy(nightEnd = nightEnd) } }

        disposables +=
            prefs.notifications().asObservable().subscribe { enabled ->
                newState { copy(notificationsEnabled = enabled) }
            }

        disposables +=
            prefs.autoEmoji.asObservable().subscribe { enabled ->
                newState { copy(autoEmojiEnabled = enabled) }
            }

        val delayedSendingLabels = context.resources.getStringArray(R.array.delayed_sending_labels)
        disposables +=
            prefs.sendDelay.asObservable().subscribe { id ->
                newState { copy(sendDelaySummary = delayedSendingLabels[id], sendDelayId = id) }
            }

        disposables +=
            prefs.delivery.asObservable().subscribe {
                newState { copy(deliveryEnabled = smsSettings.deliveryReports(-1)) }
            }

        disposables +=
            prefs.unreadAtTop.asObservable().subscribe { enabled ->
                newState { copy(unreadAtTopEnabled = enabled) }
            }

        disposables +=
            prefs.signature.asObservable().subscribe { signature ->
                newState { copy(signature = signature) }
            }

        disposables +=
            prefs.systemFont.asObservable().subscribe { enabled ->
                newState { copy(systemFontEnabled = enabled) }
            }

        disposables +=
            prefs.unicode.asObservable().subscribe {
                newState { copy(stripUnicodeEnabled = smsSettings.simpleCharacters(-1)) }
            }

        disposables +=
            prefs.mobileOnly.asObservable().subscribe { enabled ->
                newState { copy(mobileOnly = enabled) }
            }

        disposables +=
            prefs.autoDelete.asObservable().subscribe { autoDelete ->
                newState { copy(autoDelete = autoDelete) }
            }

        disposables +=
            prefs.longAsMms.asObservable().subscribe { enabled ->
                newState { copy(longAsMms = enabled) }
            }

        disposables +=
            Observable.merge(
                    prefs.mmsAutoDownload.asObservable(),
                    prefs.mmsRoamingAutoDownload.asObservable(),
                )
                .subscribe {
                    newState {
                        copy(
                            mmsAutoDownload = mmsAutoDownloadEnabled(),
                            mmsRoamingAutoDownload = mmsRoamingAutoDownloadEnabled(),
                        )
                    }
                }

        val mmsSizeLabels = context.resources.getStringArray(R.array.mms_sizes)
        val mmsSizeIds = context.resources.getIntArray(R.array.mms_sizes_ids)
        disposables +=
            prefs.mmsSize.asObservable().subscribe { maxMmsSize ->
                val index = mmsSizeIds.indexOf(maxMmsSize)
                newState {
                    copy(maxMmsSizeSummary = mmsSizeLabels[index], maxMmsSizeId = maxMmsSize)
                }
            }

        val messageLinkHandlingLabels =
            context.resources.getStringArray(R.array.messageLinkHandlings)
        val messageLinkHandlingIds = context.resources.getIntArray(R.array.messageLinkHandling_ids)
        disposables +=
            prefs.messageLinkHandling.asObservable().subscribe { messageLinkHandlingId ->
                val index = messageLinkHandlingIds.indexOf(messageLinkHandlingId)
                newState {
                    copy(
                        messageLinkHandlingSummary = messageLinkHandlingLabels[index],
                        messageLinkHandlingId = messageLinkHandlingId,
                    )
                }
            }
        disposables +=
            prefs.disableScreenshots.asObservable().subscribe { enabled ->
                newState { copy(disableScreenshotsEnabled = enabled) }
            }

        disposables +=
            syncRepo.syncProgress
                .sample(16, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .subscribe { syncProgress -> newState { copy(syncProgress = syncProgress) } }

        disposables += syncMessages
    }

    override fun bindIntents(view: SettingsView) {
        super.bindIntents(view)

        view.preferenceClicks().autoDispose(view.scope()).subscribe {
            Timber.v("Preference click: ${context.resources.getResourceName(it.id)}")

            when (it.id) {
                R.id.theme -> view.showThemePicker()

                R.id.night -> view.showNightModeDialog()

                R.id.nightStart -> {
                    val date = nightModeManager.parseTime(prefs.nightStart.get())
                    view.showStartTimePicker(
                        date.get(Calendar.HOUR_OF_DAY),
                        date.get(Calendar.MINUTE),
                    )
                }

                R.id.nightEnd -> {
                    val date = nightModeManager.parseTime(prefs.nightEnd.get())
                    view.showEndTimePicker(
                        date.get(Calendar.HOUR_OF_DAY),
                        date.get(Calendar.MINUTE),
                    )
                }

                R.id.autoEmoji -> prefs.autoEmoji.set(!prefs.autoEmoji.get())

                R.id.notifications -> navigator.showNotificationSettings()

                R.id.swipeActions -> view.showSwipeActions()

                R.id.delayed -> view.showDelayDurationDialog()

                R.id.delivery -> prefs.delivery.set(!smsSettings.deliveryReports(-1))

                R.id.unreadAtTop -> prefs.unreadAtTop.set(!prefs.unreadAtTop.get())

                R.id.signature -> view.showSignatureDialog(prefs.signature.get())

                R.id.systemFont -> prefs.systemFont.set(!prefs.systemFont.get())

                R.id.unicode -> prefs.unicode.set(!smsSettings.simpleCharacters(-1))

                R.id.mobileOnly -> prefs.mobileOnly.set(!prefs.mobileOnly.get())

                R.id.autoDelete -> view.showAutoDeleteDialog(prefs.autoDelete.get())

                R.id.longAsMms -> prefs.longAsMms.set(!prefs.longAsMms.get())
                R.id.mmsAutoDownload -> prefs.mmsAutoDownload.set(!mmsAutoDownloadEnabled())
                R.id.mmsRoamingAutoDownload ->
                    prefs.mmsRoamingAutoDownload.set(!mmsRoamingAutoDownloadEnabled())

                R.id.mmsSize -> view.showMmsSizePicker()

                R.id.messsageLinkHandling -> view.showMessageLinkHandlingDialogPicker()

                R.id.disableScreenshots ->
                    prefs.disableScreenshots.set(!prefs.disableScreenshots.get())

                R.id.sync -> syncMessages.execute(Unit)

                R.id.about -> view.showAbout()
            }
        }

        view
            .aboutLongClicks()
            .map { !prefs.logging.get() }
            .doOnNext { enabled -> prefs.logging.set(enabled) }
            .autoDispose(view.scope())
            .subscribe { enabled ->
                context.makeToast(
                    when (enabled) {
                        true -> R.string.settings_logging_enabled
                        false -> R.string.settings_logging_disabled
                    }
                )
            }

        view
            .nightModeSelected()
            .withLatestFrom(billingManager.upgradeStatus) { mode, upgraded ->
                //                    if (!upgraded && mode == Preferences.NIGHT_MODE_AUTO) {
                //                        view.showQksmsPlusSnackbar()
                //                    } else {
                nightModeManager.updateNightMode(mode)
                //                    }
            }
            .autoDispose(view.scope())
            .subscribe()

        view.viewQksmsPlusClicks().autoDispose(view.scope()).subscribe {
            navigator.showQksmsPlusActivity("settings_night")
        }

        view.nightStartSelected().autoDispose(view.scope()).subscribe {
            nightModeManager.setNightStart(it.first, it.second)
        }

        view.nightEndSelected().autoDispose(view.scope()).subscribe {
            nightModeManager.setNightEnd(it.first, it.second)
        }

        view
            .sendDelaySelected()
            .withLatestFrom(billingManager.upgradeStatus) { duration, upgraded ->
                //                    if (!upgraded && duration != 0) {
                //                        view.showQksmsPlusSnackbar()
                //                    } else {
                prefs.sendDelay.set(duration)
                //                    }
            }
            .autoDispose(view.scope())
            .subscribe()

        view.signatureChanged().doOnNext(prefs.signature::set).autoDispose(view.scope()).subscribe()

        view
            .autoDeleteChanged()
            .observeOn(Schedulers.io())
            .filter { maxAge ->
                if (maxAge == 0) {
                    return@filter true
                }

                val counts = messageRepo.getOldMessageCounts(maxAge)
                if (counts.values.sum() == 0) {
                    return@filter true
                }

                runBlocking { view.showAutoDeleteWarningDialog(counts.values.sum()) }
            }
            .doOnNext { maxAge ->
                when (maxAge == 0) {
                    true -> AutoDeleteService.cancelJob(context)
                    false -> {
                        AutoDeleteService.scheduleJob(context)
                        deleteOldMessages.execute(Unit)
                    }
                }
            }
            .doOnNext(prefs.autoDelete::set)
            .autoDispose(view.scope())
            .subscribe()

        view.mmsSizeSelected().autoDispose(view.scope()).subscribe(prefs.mmsSize::set)

        view
            .messageLinkHandlingSelected()
            .autoDispose(view.scope())
            .subscribe(prefs.messageLinkHandling::set)
    }

    private fun carrierMmsConfig() = CarrierMmsConfig.get(context, -1)

    private fun mmsAutoDownloadEnabled(): Boolean =
        if (prefs.mmsAutoDownload.isSet) prefs.mmsAutoDownload.get()
        else carrierMmsConfig().autoRetrieveEnabled

    private fun mmsRoamingAutoDownloadEnabled(): Boolean =
        if (prefs.mmsRoamingAutoDownload.isSet) prefs.mmsRoamingAutoDownload.get()
        else carrierMmsConfig().roamingAutoRetrieveEnabled
}
