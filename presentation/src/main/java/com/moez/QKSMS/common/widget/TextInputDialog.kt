/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.widget

import android.app.Activity
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import org.prauga.messages.R
import org.prauga.messages.common.base.QkThemedActivity
import org.prauga.messages.databinding.TextInputDialogBinding

class TextInputDialog(context: Activity, hint: String, listener: (String) -> Unit) :
    AlertDialog(context, R.style.AppThemeDialog) {

    private val layout = TextInputDialogBinding.inflate(LayoutInflater.from(context))

    init {
        layout.field.hint = hint

        setView(layout.root)
        setButton(BUTTON_NEUTRAL, context.getString(R.string.button_cancel)) { _, _ -> }
        setButton(BUTTON_NEGATIVE, context.getString(R.string.button_delete)) { _, _ ->
            listener("")
        }
        setButton(BUTTON_POSITIVE, context.getString(R.string.button_save)) { _, _ ->
            listener(layout.field.text.toString())
        }

        setOnShowListener {
            (context as? QkThemedActivity<*>)?.colors?.theme()?.let { theme ->
                getButton(BUTTON_NEUTRAL)?.setTextColor(theme.theme)
                getButton(BUTTON_NEGATIVE)?.setTextColor(theme.theme)
                getButton(BUTTON_POSITIVE)?.setTextColor(theme.theme)
            }
        }
    }

    fun setText(text: String): TextInputDialog {
        layout.field.setText(text)
        return this
    }
}
