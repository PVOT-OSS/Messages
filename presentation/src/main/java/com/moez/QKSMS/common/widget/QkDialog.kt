/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.widget

import android.app.Activity
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import org.prauga.messages.common.base.QkAdapter
import org.prauga.messages.databinding.QkDialogBinding

class QkDialog(private val context: Activity) : AlertDialog(context) {

    private val view = QkDialogBinding.inflate(LayoutInflater.from(context))

    @StringRes
    var titleRes: Int? = null
        set(value) {
            field = value
            title = value?.let(context::getString)
        }

    var title: String? = null
        set(value) {
            field = value
            view.title.text = value
            view.title.isVisible = !value.isNullOrBlank()
        }

    @StringRes
    var subtitleRes: Int? = null
        set(value) {
            field = value
            subtitle = value?.let(context::getString)
        }

    var subtitle: String? = null
        set(value) {
            field = value
            view.subtitle.text = value
            view.subtitle.isVisible = !value.isNullOrBlank()
        }

    var adapter: QkAdapter<*, *>? = null
        set(value) {
            field = value
            view.list.isVisible = value != null
            view.list.adapter = value
        }

    var positiveButtonListener: (() -> Unit)? = null

    @StringRes
    var positiveButton: Int? = null
        set(value) {
            field = value
            value?.run(view.positiveButton::setText)
            view.positiveButton.isVisible = value != null
            view.positiveButton.setOnClickListener { positiveButtonListener?.invoke() ?: dismiss() }
        }

    var negativeButtonListener: (() -> Unit)? = null

    @StringRes
    var negativeButton: Int? = null
        set(value) {
            field = value
            value?.run(view.negativeButton::setText)
            view.negativeButton.isVisible = value != null
            view.negativeButton.setOnClickListener { negativeButtonListener?.invoke() ?: dismiss() }
        }

    var cancelListener: (() -> Unit)? = null
        set(value) {
            field = value
            setOnCancelListener { value?.invoke() }
        }

    init {
        setView(view.root)
    }
}
