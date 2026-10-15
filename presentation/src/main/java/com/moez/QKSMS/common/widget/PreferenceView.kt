/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import org.prauga.messages.R
import org.prauga.messages.common.util.extensions.resolveThemeAttribute
import org.prauga.messages.common.util.extensions.resolveThemeColorStateList
import org.prauga.messages.common.util.extensions.setVisible
import org.prauga.messages.databinding.PreferenceViewBinding
import org.prauga.messages.injection.appComponent

class PreferenceView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayoutCompat(context, attrs) {

    private var layout: PreferenceViewBinding

    val titleTextView: TextView
        get() = layout.titleView

    var title: String? = null
        set(value) {
            field = value

            if (isInEditMode) {
                findViewById<TextView>(R.id.titleView).text = value
            } else {
                layout.titleView.text = value
            }
        }

    var summary: String? = null
        set(value) {
            field = value

            if (isInEditMode) {
                findViewById<TextView>(R.id.summaryView).run {
                    text = value
                    setVisible(value?.isNotEmpty() == true)
                }
            } else {
                layout.summaryView.text = value
                layout.summaryView.setVisible(value?.isNotEmpty() == true)
            }
        }

    init {
        if (!isInEditMode) {
            appComponent.inject(this)
        }

        layout = PreferenceViewBinding.inflate(LayoutInflater.from(context), this)
        setBackgroundResource(context.resolveThemeAttribute(R.attr.selectableItemBackground))
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        layout.icon.imageTintList =
            context.resolveThemeColorStateList(android.R.attr.textColorSecondary)

        context.obtainStyledAttributes(attrs, R.styleable.PreferenceView).run {
            title = getString(R.styleable.PreferenceView_title)
            summary = getString(R.styleable.PreferenceView_summary)

            // If there's a custom view used for the preference's widget, inflate it
            getResourceId(R.styleable.PreferenceView_widget, -1)
                .takeIf { it != -1 }
                ?.let { id -> View.inflate(context, id, layout.widgetFrame) }

            // If an icon is being used, set up the icon view
            getResourceId(R.styleable.PreferenceView_icon, -1)
                .takeIf { it != -1 }
                ?.let { id ->
                    layout.icon.setVisible(true)
                    layout.icon.setImageResource(id)
                }

            recycle()
        }
    }
}
