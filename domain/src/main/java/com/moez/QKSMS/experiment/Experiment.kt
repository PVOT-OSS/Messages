/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.experiment

import android.content.Context
import android.preference.PreferenceManager
import java.util.Locale
import java.util.Random

abstract class Experiment<T>(val context: Context) {

    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(context) }
    private val prefKey: String
        get() = "experiment_$key"

    protected abstract val key: String
    protected abstract val variants: List<Variant<T>>
    protected abstract val default: T

    /** Returns true if the current device qualifies for the experiment */
    protected open val qualifies: Boolean by lazy { Locale.getDefault().language.startsWith("en") }

    val variant: T by lazy {
        when {
            !qualifies -> null // Device doesn't quality for experiment

            prefs.contains(prefKey) -> { // Variant already set
                variants.firstOrNull { it.key == prefs.getString(prefKey, null) }?.value
            }

            else -> { // Variant hasn't been set yet
                variants[Random().nextInt(variants.size)]
                    .also { variant -> prefs.edit().putString(prefKey, variant.key).apply() }
                    .value
            }
        } ?: default
    }
}
