/*
 * Copyright (C) 2019 Moez Bhatti <moez.bhatti@gmail.com>
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
package dev.danascape.messages.feature.compose.part

import android.annotation.SuppressLint
import android.content.Context
import dev.danascape.messages.R
import dev.danascape.messages.common.Navigator
import dev.danascape.messages.common.base.QkViewHolder
import dev.danascape.messages.common.util.Colors
import dev.danascape.messages.common.util.extensions.resolveThemeColor
import dev.danascape.messages.common.util.extensions.setBackgroundTint
import dev.danascape.messages.common.util.extensions.setTint
import dev.danascape.messages.databinding.MmsFileListItemBinding
import dev.danascape.messages.feature.compose.BubbleUtils
import dev.danascape.messages.model.Message
import dev.danascape.messages.model.MmsPart
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class FileBinder @Inject constructor(colors: Colors, private val context: Context) : PartBinder() {

    @Inject lateinit var navigator: Navigator

    override val partLayout = R.layout.mms_file_list_item
    override var theme = colors.theme()

    // This is the last binder we check. If we're here, we can bind the part
    override fun canBindPart(part: MmsPart) = true

    @SuppressLint("CheckResult")
    override fun bindPart(
        holder: QkViewHolder,
        part: MmsPart,
        message: Message,
        canGroupWithPrevious: Boolean,
        canGroupWithNext: Boolean
    ) {
        val binding = MmsFileListItemBinding.bind(holder.containerView)

        BubbleUtils.getBubble(false, canGroupWithPrevious, canGroupWithNext, message.isMe())
                .let(binding.fileBackground::setBackgroundResource)

        Observable.just(part.getUri())
                .map(context.contentResolver::openInputStream)
                .map { inputStream -> inputStream.use { it.available() } }
                .map { bytes ->
                    when (bytes) {
                        in 0..999 -> "$bytes B"
                        in 1000..999999 -> "${"%.1f".format(bytes / 1000f)} KB"
                        in 1000000..9999999 -> "${"%.1f".format(bytes / 1000000f)} MB"
                        else -> "${"%.1f".format(bytes / 1000000000f)} GB"
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { size -> binding.size.text = size }

        binding.filename.text = part.name

        if (!message.isMe()) {
            binding.fileBackground.setBackgroundTint(theme.theme)
            binding.icon.setTint(theme.textPrimary)
            binding.filename.setTextColor(theme.textPrimary)
            binding.size.setTextColor(theme.textTertiary)
        } else {
            binding.fileBackground.setBackgroundTint(holder.containerView.context.resolveThemeColor(R.attr.bubbleColor))
            binding.icon.setTint(holder.containerView.context.resolveThemeColor(android.R.attr.textColorSecondary))
            binding.filename.setTextColor(holder.containerView.context.resolveThemeColor(android.R.attr.textColorPrimary))
            binding.size.setTextColor(holder.containerView.context.resolveThemeColor(android.R.attr.textColorTertiary))
        }
    }

}