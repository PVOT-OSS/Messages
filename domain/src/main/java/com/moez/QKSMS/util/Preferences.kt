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
package org.prauga.messages.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import io.reactivex.rxjava3.core.Observable
import org.prauga.messages.common.util.extensions.versionCode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Preferences @Inject constructor(
    context: Context,
    private val sharedPrefs: SharedPreferences
) {

    companion object {
        const val NIGHT_MODE_SYSTEM = 0
        const val NIGHT_MODE_OFF = 1
        const val NIGHT_MODE_ON = 2
        const val NIGHT_MODE_AUTO = 3

        const val TEXT_SIZE_SMALL = 0
        const val TEXT_SIZE_NORMAL = 1
        const val TEXT_SIZE_LARGE = 2
        const val TEXT_SIZE_LARGER = 3
        const val TEXT_SIZE_SUPER = 4

        const val NOTIFICATION_PREVIEWS_ALL = 0
        const val NOTIFICATION_PREVIEWS_NAME = 1
        const val NOTIFICATION_PREVIEWS_NONE = 2

        const val NOTIFICATION_ACTION_NONE = 0
        const val NOTIFICATION_ACTION_ARCHIVE = 1
        const val NOTIFICATION_ACTION_DELETE = 2
        const val NOTIFICATION_ACTION_BLOCK = 3
        const val NOTIFICATION_ACTION_CALL = 4
        const val NOTIFICATION_ACTION_READ = 5
        const val NOTIFICATION_ACTION_REPLY = 6
        const val NOTIFICATION_ACTION_SPEAK = 7

        const val SEND_DELAY_NONE = 0
        const val SEND_DELAY_SHORT = 1
        const val SEND_DELAY_MEDIUM = 2
        const val SEND_DELAY_LONG = 3

        const val SWIPE_ACTION_NONE = 0
        const val SWIPE_ACTION_ARCHIVE = 1
        const val SWIPE_ACTION_DELETE = 2
        const val SWIPE_ACTION_BLOCK = 3
        const val SWIPE_ACTION_CALL = 4
        const val SWIPE_ACTION_READ = 5
        const val SWIPE_ACTION_UNREAD = 6
        const val SWIPE_ACTION_SPEAK = 7

        const val BLOCKING_MANAGER_QKSMS = 0
        const val BLOCKING_MANAGER_CC = 1
        const val BLOCKING_MANAGER_SIA = 2
        const val BLOCKING_MANAGER_CB = 3

        const val MESSAGE_LINK_HANDLING_BLOCK = 0
        const val MESSAGE_LINK_HANDLING_ALLOW = 1
        const val MESSAGE_LINK_HANDLING_ASK = 2
    }

    // Internal
    val didSetReferrer = preference("didSetReferrer", false)
    val night = preference("night", false)
    val canUseSubId = preference("canUseSubId", true)
    val version = preference("version", context.versionCode)
    val changelogVersion = preference("changelogVersion", context.versionCode)
    val hasAskedForNotificationPermission = preference("hasAskedForNotificationPermission", false)
    val backupDirectory = preference("backupDirectory", Uri.EMPTY, UriPreferenceConverter())
    @Deprecated("This should only be accessed when migrating to @blockingManager")
    val sia = preference("sia", false)

    // User configurable
    val sendAsGroup = preference("sendAsGroup", true)
    val nightMode = preference("nightMode", when (Build.VERSION.SDK_INT >= 29) {
        true -> NIGHT_MODE_SYSTEM
        false -> NIGHT_MODE_OFF
    })
    val nightStart = preference("nightStart", "18:00")
    val nightEnd = preference("nightEnd", "6:00")
    val systemFont = preference("systemFont", false)
    val textSize = preference("textSize", TEXT_SIZE_NORMAL)
    val blockingManager = preference("blockingManager", BLOCKING_MANAGER_QKSMS)
    val drop = preference("drop", false)
    val silentNotContact = preference("silentNotContact", false)
    val notifAction1 = preference("notifAction1", NOTIFICATION_ACTION_READ)
    val notifAction2 = preference("notifAction2", NOTIFICATION_ACTION_REPLY)
    val notifAction3 = preference("notifAction3", NOTIFICATION_ACTION_NONE)
    val qkreply = preference("qkreply", Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
    val qkreplyTapDismiss = preference("qkreplyTapDismiss", true)
    val sendDelay = preference("sendDelay", SEND_DELAY_NONE)
    val swipeRight = preference("swipeRight", SWIPE_ACTION_ARCHIVE)
    val swipeLeft = preference("swipeLeft", SWIPE_ACTION_DELETE)
    val autoEmoji = preference("autoEmoji", true)
    val delivery = preference("delivery", false)
    val signature = preference("signature", "")
    val unicode = preference("unicode", false)
    val mobileOnly = preference("mobileOnly", false)
    val autoDelete = preference("autoDelete", 0)
    val longAsMms = preference("longAsMms", false)
    val mmsSize = preference("mmsSize", 300)
    val messageLinkHandling = preference("messageLinkHandling", MESSAGE_LINK_HANDLING_ASK)
    val disableScreenshots = preference("disableScreenshots", false)
    val logging = preference("logging", false)
    val unreadAtTop = preference("unreadAtTop", false)

    init {
        // Migrate from old night mode preference to new one, now that we support android Q night mode
        val nightModeSummary = preference("nightModeSummary", 0)
        if (nightModeSummary.isSet) {
            nightMode.set(when (nightModeSummary.get()) {
                0 -> NIGHT_MODE_OFF
                1 -> NIGHT_MODE_ON
                2 -> NIGHT_MODE_AUTO
                else -> NIGHT_MODE_OFF
            })
            nightModeSummary.delete()
        }
    }

    /**
     * Returns a stream of preference keys for changing preferences
     */
    val keyChanges: Observable<String> = Observable.create<String> { emitter ->
        // Making this a lambda would cause it to be GCd
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key != null)
                emitter.onNext(key)
        }

        emitter.setCancellable {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }

        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
    }.share()

    fun theme(
        recipientId: Long = 0,
        default: Int = preference("theme", 0xFF0097A7.toInt()).get()
    ): Preference<Int> {
        return when (recipientId) {
            0L -> preference("theme", 0xFF0097A7.toInt())
            else -> preference("theme_$recipientId", default)
        }
    }

    fun notifications(threadId: Long = 0): Preference<Boolean> {
        val default = preference("notifications", true)

        return when (threadId) {
            0L -> default
            else -> preference("notifications_$threadId", default.get())
        }
    }

    fun notificationPreviews(threadId: Long = 0): Preference<Int> {
        val default = preference("notification_previews", 0)

        return when (threadId) {
            0L -> default
            else -> preference("notification_previews_$threadId", default.get())
        }
    }

    fun wakeScreen(threadId: Long = 0): Preference<Boolean> {
        val default = preference("wake", false)

        return when (threadId) {
            0L -> default
            else -> preference("wake_$threadId", default.get())
        }
    }

    fun <T : Any> preferential(key: String, default: T, serializer: Preference.Serializer<T>? = null): Preference<T> =
        preference(key, default, serializer)

    private fun <T : Any> preference(key: String, default: T, serializer: Preference.Serializer<T>? = null): Preference<T> =
        Preference(sharedPrefs, key, default, serializer)
}
