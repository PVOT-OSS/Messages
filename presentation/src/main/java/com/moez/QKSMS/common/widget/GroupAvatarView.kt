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

class GroupAvatarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

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
