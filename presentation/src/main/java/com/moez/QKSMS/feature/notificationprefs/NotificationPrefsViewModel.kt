/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
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

package org.prauga.messages.feature.notificationprefs

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.moez.QKSMS.common.base.PvotViewModel
import com.moez.QKSMS.util.asFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.prauga.messages.R
import org.prauga.messages.common.Navigator
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.util.Preferences
import javax.inject.Inject
import javax.inject.Named

class NotificationPrefsViewModel @Inject constructor(
    @Named("threadId") private val threadId: Long,
    private val context: Context,
    private val conversationRepo: ConversationRepository,
    private val navigator: Navigator,
    private val prefs: Preferences
) : PvotViewModel<NotificationPrefsState>(
    NotificationPrefsState(threadId = threadId)
) {

    private val notifications = prefs.notifications(threadId)
    private val previews = prefs.notificationPreviews(threadId)
    private val wake = prefs.wakeScreen(threadId)
    private val vibration = prefs.vibration(threadId)
    private val ringtone = prefs.ringtone(threadId)

    init {
        // title
        viewModelScope.launch(Dispatchers.IO) {
            val title = conversationRepo.getConversation(threadId)?.getTitle()
            if (title != null) {
                newState { copy(conversationTitle = title) }
            }
        }

        // notifications enabled
        viewModelScope.launch {
            notifications.flow.collect { enabled ->
                newState { copy(notificationsEnabled = enabled) }
            }
        }

        val previewLabels = context.resources.getStringArray(
            R.array.notification_preview_options
        )

        viewModelScope.launch {
            previews.asFlow().collect { previewId ->
                newState {
                    copy(
                        previewSummary = previewLabels[previewId],
                        previewId = previewId
                    )
                }
            }
        }

        val actionLabels =
            context.resources.getStringArray(R.array.notification_actions)

        viewModelScope.launch {
            prefs.notifAction1.asFlow().collect { id ->
                newState { copy(action1Summary = actionLabels[id]) }
            }
        }

        viewModelScope.launch {
            prefs.notifAction2.asFlow().collect { id ->
                newState { copy(action2Summary = actionLabels[id]) }
            }
        }

        viewModelScope.launch {
            prefs.notifAction3.asFlow().collect { id ->
                newState { copy(action3Summary = actionLabels[id]) }
            }
        }

        viewModelScope.launch {
            wake.asFlow().collect { enabled ->
                newState { copy(wakeEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            prefs.silentNotContact.asFlow().collect { enabled ->
                newState { copy(silentNotContact = enabled) }
            }
        }

        viewModelScope.launch {
            vibration.asFlow().collect { enabled ->
                newState { copy(vibrationEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            ringtone.asFlow().collect { uriString ->
                val title = uriString
                    .takeIf { it.isNotEmpty() }
                    ?.let(Uri::parse)
                    ?.let { uri -> RingtoneManager.getRingtone(context, uri) }
                    ?.getTitle(context)
                    ?: context.getString(R.string.settings_ringtone_none)

                newState { copy(ringtoneName = title) }
            }
        }

        viewModelScope.launch {
            prefs.qkreply.asFlow().collect { enabled ->
                newState { copy(qkReplyEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            prefs.qkreplyTapDismiss.asFlow().collect { enabled ->
                newState { copy(qkReplyTapDismiss = enabled) }
            }
        }
    }

    fun bindView(view: NotificationPrefsView) {
        super.bindView(view)

        var lastActionPreferenceId: Int? = null
        viewModelScope.launch {
            view.preferenceClickIntent.collect { pref ->
                when (pref.id) {
                    R.id.notificationsO -> navigator.showNotificationChannel(threadId)

                    R.id.notifications -> notifications.set(!notifications.get())

                    R.id.previews -> view.showPreviewModeDialog()

                    R.id.wake -> wake.set(!wake.get())

                    R.id.silentNotContact ->
                        prefs.silentNotContact.set(!prefs.silentNotContact.get())

                    R.id.vibration -> vibration.set(!vibration.get())

                    R.id.ringtone -> view.showRingtonePicker(
                        ringtone.get()
                            .takeIf { it.isNotEmpty() }
                            ?.let(Uri::parse)
                    )

                    R.id.action1 -> {
                        lastActionPreferenceId = R.id.action1
                        view.showActionDialog(prefs.notifAction1.get())
                    }

                    R.id.action2 -> {
                        lastActionPreferenceId = R.id.action2
                        view.showActionDialog(prefs.notifAction2.get())
                    }

                    R.id.action3 -> {
                        lastActionPreferenceId = R.id.action3
                        view.showActionDialog(prefs.notifAction3.get())
                    }

                    R.id.qkreply -> prefs.qkreply.set(!prefs.qkreply.get())

                    R.id.qkreplyTapDismiss ->
                        prefs.qkreplyTapDismiss.set(!prefs.qkreplyTapDismiss.get())
                }
            }
        }

        viewModelScope.launch {
            view.previewModeSelectedIntent.collect { mode ->
                previews.set(mode)
            }
        }

        viewModelScope.launch {
            view.ringtoneSelectedIntent.collect { ringtone ->
                this@NotificationPrefsViewModel.ringtone.set(ringtone)
            }
        }

        viewModelScope.launch {
            view.actionsSelectedIntent.collect { action ->
                when (lastActionPreferenceId) {
                    R.id.action1 -> prefs.notifAction1.set(action)
                    R.id.action2 -> prefs.notifAction2.set(action)
                    R.id.action3 -> prefs.notifAction3.set(action)
                }
            }
        }
    }
}