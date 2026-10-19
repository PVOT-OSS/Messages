/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import org.prauga.messages.common.util.extensions.getColorCompat
import org.prauga.messages.common.util.extensions.resolveThemeColor
import org.prauga.messages.common.util.extensions.setBackgroundTint
import org.prauga.messages.databinding.GroupAvatarViewBinding
import org.prauga.messages.model.Recipient

class GroupAvatarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    ConstraintLayout(context, attrs) {

    private var layout: GroupAvatarViewBinding =
        GroupAvatarViewBinding.inflate(LayoutInflater.from(context), this)

    var recipients: List<Recipient> = ArrayList()
        set(value) {
            field = value.sortedWith(compareByDescending { contact -> contact.contact?.lookupKey })
            updateView()
        }

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (!isInEditMode) {
            updateView()
        }
    }

    private fun updateView() {
        layout.avatar1Frame.setBackgroundTint(
            when (recipients.size > 1) {
                true -> context.resolveThemeColor(android.R.attr.windowBackground)
                false -> context.getColorCompat(android.R.color.transparent)
            }
        )
        layout.avatar1Frame.updateLayoutParams<LayoutParams> {
            matchConstraintPercentWidth = if (recipients.size > 1) 0.75f else 1.0f
        }
        layout.avatar2.isVisible = recipients.size > 1

        recipients.getOrNull(0).run(layout.avatar1::setRecipient)
        recipients.getOrNull(1).run(layout.avatar2::setRecipient)
    }
}
