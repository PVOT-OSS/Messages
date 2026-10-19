/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.util

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import javax.inject.Inject
import javax.inject.Singleton
import org.prauga.messages.R
import timber.log.Timber

@Singleton
class FontProvider @Inject constructor(context: Context) {

    private var lato: Typeface? = null
    private val pendingCallbacks = ArrayList<(Typeface) -> Unit>()

    init {
        ResourcesCompat.getFont(
            context,
            R.font.sora,
            object : ResourcesCompat.FontCallback() {
                override fun onFontRetrievalFailed(reason: Int) {
                    Timber.w("Font retrieval failed: $reason")
                }

                override fun onFontRetrieved(typeface: Typeface) {
                    lato = typeface

                    pendingCallbacks.forEach { lato?.run(it) }
                    pendingCallbacks.clear()
                }
            },
            null,
        )
    }

    fun getLato(callback: (Typeface) -> Unit) {
        lato?.run(callback) ?: pendingCallbacks.add(callback)
    }
}
