/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import com.f2prateek.rx.preferences2.RxSharedPreferences
import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.util.NightModeManager
import org.prauga.messages.util.Preferences

/**
 * When upgrading from 2.7.3 to 3.0, migrate the preferences
 *
 * Blocked conversations will be migrated in SyncManager
 */
class MigratePreferences
@Inject
constructor(
    private val nightModeManager: NightModeManager,
    private val prefs: Preferences,
    private val rxPrefs: RxSharedPreferences,
) : Interactor<Unit>() {

    override fun buildObservable(params: Unit): Flowable<*> {
        return Flowable.fromCallable { rxPrefs.getBoolean("pref_key_welcome_seen", false) }
            .filter { seen ->
                seen.get()
            } // Only proceed if this value is true. It will be set false at the end
            .doOnNext {
                // Theme
                val defaultTheme = prefs.theme().get().toString()
                val oldTheme = rxPrefs.getString("pref_key_theme", defaultTheme).get()
                prefs.theme().set(Integer.parseInt(oldTheme))

                // Night mode
                val background = rxPrefs.getString("pref_key_background", "light").get()
                val autoNight = rxPrefs.getBoolean("pref_key_night_auto", false).get()
                when {
                    autoNight -> nightModeManager.updateNightMode(Preferences.NIGHT_MODE_AUTO)
                    background == "light" ->
                        nightModeManager.updateNightMode(Preferences.NIGHT_MODE_OFF)
                    background == "grey" ->
                        nightModeManager.updateNightMode(Preferences.NIGHT_MODE_ON)
                    background == "black" ->
                        nightModeManager.updateNightMode(Preferences.NIGHT_MODE_ON)
                }

                // Delivery
                prefs.delivery.set(
                    rxPrefs.getBoolean("pref_key_delivery", prefs.delivery.get()).get()
                )

                // Quickreply
                prefs.qkreply.set(
                    rxPrefs.getBoolean("pref_key_quickreply_enabled", prefs.qkreply.get()).get()
                )
                prefs.qkreplyTapDismiss.set(
                    rxPrefs
                        .getBoolean("pref_key_quickreply_dismiss", prefs.qkreplyTapDismiss.get())
                        .get()
                )

                // Font size
                prefs.textSize.set(
                    rxPrefs.getString("pref_key_font_size", "${prefs.textSize.get()}").get().toInt()
                )

                // Unicode
                prefs.unicode.set(
                    rxPrefs.getBoolean("pref_key_strip_unicode", prefs.unicode.get()).get()
                )
            }
            .doOnNext { seen ->
                seen.delete()
            } // Clear this value so that we don't need to migrate again
    }
}
