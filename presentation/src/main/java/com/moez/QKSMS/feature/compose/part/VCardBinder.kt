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
package org.prauga.messages.feature.compose.part

import android.content.Context
import androidx.core.view.isVisible
import ezvcard.Ezvcard
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.prauga.messages.R
import org.prauga.messages.common.base.QkViewHolder
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.extensions.getDisplayName
import org.prauga.messages.common.util.extensions.resolveThemeColor
import org.prauga.messages.common.util.extensions.setBackgroundTint
import org.prauga.messages.common.util.extensions.setTint
import org.prauga.messages.databinding.MmsVcardListItemBinding
import org.prauga.messages.extensions.isVCard
import org.prauga.messages.extensions.mapNotNull
import org.prauga.messages.feature.compose.BubbleUtils
import org.prauga.messages.model.Message
import org.prauga.messages.model.MmsPart
import javax.inject.Inject

class VCardBinder @Inject constructor(colors: Colors, private val context: Context) : PartBinder() {

    override val partLayout = R.layout.mms_vcard_list_item
    override var theme = colors.theme()

    override fun canBindPart(part: MmsPart) = part.isVCard()

    override fun bindPart(
        holder: QkViewHolder,
        part: MmsPart,
        message: Message,
        canGroupWithPrevious: Boolean,
        canGroupWithNext: Boolean
    ) {
        val binding = MmsVcardListItemBinding.bind(holder.containerView)

        BubbleUtils.getBubble(false, canGroupWithPrevious, canGroupWithNext, message.isMe())
            .let(binding.vCardBackground::setBackgroundResource)

        holder.containerView.setOnClickListener { clicks.onNext(part.id) }

        Observable.just(part.getUri())
            .map(context.contentResolver::openInputStream)
            .mapNotNull { inputStream -> inputStream.use { Ezvcard.parse(it).first() } }
            .map { vcard -> vcard.getDisplayName() ?: "" }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { displayName ->
                binding.name?.text = displayName
                binding.name.isVisible = displayName.isNotEmpty()
            }

        if (!message.isMe()) {
            binding.vCardBackground.setBackgroundTint(theme.theme)
            binding.vCardAvatar.setTint(theme.textPrimary)
            binding.name.setTextColor(theme.textPrimary)
            binding.label.setTextColor(theme.textTertiary)
        } else {
            binding.vCardBackground.setBackgroundTint(
                holder.containerView.context.resolveThemeColor(
                    R.attr.bubbleColor
                )
            )
            binding.vCardAvatar.setTint(holder.containerView.context.resolveThemeColor(android.R.attr.textColorSecondary))
            binding.name.setTextColor(holder.containerView.context.resolveThemeColor(android.R.attr.textColorPrimary))
            binding.label.setTextColor(holder.containerView.context.resolveThemeColor(android.R.attr.textColorTertiary))
        }
    }

}
