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
package org.prauga.messages.interactor

import android.content.SharedPreferences
import io.reactivex.rxjava3.core.Flowable
import org.prauga.messages.util.NightModeManager
import org.prauga.messages.util.Preferences
import javax.inject.Inject

/**
 * When upgrading from 2.7.3 to 3.0, migrate the preferences
 *
 * Blocked conversations will be migrated in SyncManager
 */
class MigratePreferences @Inject constructor(
    private val nightModeManager: NightModeManager,
    private val prefs: Preferences,
    private val sharedPreferences: SharedPreferences
) : Interactor<Unit>() {

    override fun buildObservable(params: Unit): Flowable<*> {
        return Flowable.fromCallable { sharedPreferences.getBoolean("pref_key_welcome_seen", false) }
            .filter { it } // Only proceed if this value is true. It will be cleared at the end
            .doOnNext {
                // Theme
                val defaultTheme = prefs.theme().get().toString()
                val oldTheme = sharedPreferences.getString("pref_key_theme", defaultTheme) ?: defaultTheme
                prefs.theme().set(Integer.parseInt(oldTheme))

                // Night mode
                val background = sharedPreferences.getString("pref_key_background", "light") ?: "light"
                val autoNight = sharedPreferences.getBoolean("pref_key_night_auto", false)
                when {
                    autoNight -> nightModeManager.updateNightMode(Preferences.NIGHT_MODE_AUTO)
                    background == "light" -> nightModeManager.updateNightMode(Preferences.NIGHT_MODE_OFF)
                    background == "grey" -> nightModeManager.updateNightMode(Preferences.NIGHT_MODE_ON)
                    background == "black" -> nightModeManager.updateNightMode(Preferences.NIGHT_MODE_ON)
                }

                // Delivery
                prefs.delivery.set(sharedPreferences.getBoolean("pref_key_delivery", prefs.delivery.get()))

                // Quickreply
                prefs.qkreply.set(sharedPreferences.getBoolean("pref_key_quickreply_enabled", prefs.qkreply.get()))
                prefs.qkreplyTapDismiss.set(sharedPreferences.getBoolean("pref_key_quickreply_dismiss", prefs.qkreplyTapDismiss.get()))

                // Font size
                prefs.textSize.set((sharedPreferences.getString("pref_key_font_size", "${prefs.textSize.get()}") ?: "${prefs.textSize.get()}").toInt())

                // Unicode
                prefs.unicode.set(sharedPreferences.getBoolean("pref_key_strip_unicode", prefs.unicode.get()))
            }
            .doOnNext {
                sharedPreferences.edit().remove("pref_key_welcome_seen").apply()
            }
    }

}
