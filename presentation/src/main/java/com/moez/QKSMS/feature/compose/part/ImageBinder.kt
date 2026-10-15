/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.compose.part

import android.content.Context
import javax.inject.Inject
import org.prauga.messages.R
import org.prauga.messages.common.base.QkViewHolder
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.extensions.setVisible
import org.prauga.messages.common.widget.BubbleImageView
import org.prauga.messages.databinding.MmsImagePreviewListItemBinding
import org.prauga.messages.extensions.isImage
import org.prauga.messages.extensions.isVideo
import org.prauga.messages.model.Message
import org.prauga.messages.model.MmsPart
import org.prauga.messages.util.GlideApp

class ImageBinder @Inject constructor(colors: Colors, private val context: Context) : PartBinder() {

    override val partLayout = R.layout.mms_image_preview_list_item
    override var theme = colors.theme()

    override fun canBindPart(part: MmsPart) = part.isImage() || part.isVideo()

    override fun bindPart(
        holder: QkViewHolder,
        part: MmsPart,
        message: Message,
        canGroupWithPrevious: Boolean,
        canGroupWithNext: Boolean,
    ) {
        val binding = MmsImagePreviewListItemBinding.bind(holder.containerView)

        binding.video.setVisible(part.isVideo())
        holder.containerView.setOnClickListener { clicks.onNext(part.id) }

        binding.thumbnail.bubbleStyle =
            when {
                !canGroupWithPrevious && canGroupWithNext ->
                    if (message.isMe()) BubbleImageView.Style.OUT_FIRST
                    else BubbleImageView.Style.IN_FIRST
                canGroupWithPrevious && canGroupWithNext ->
                    if (message.isMe()) BubbleImageView.Style.OUT_MIDDLE
                    else BubbleImageView.Style.IN_MIDDLE
                canGroupWithPrevious && !canGroupWithNext ->
                    if (message.isMe()) BubbleImageView.Style.OUT_LAST
                    else BubbleImageView.Style.IN_LAST
                else -> BubbleImageView.Style.ONLY
            }

        GlideApp.with(context).load(part.getUri()).fitCenter().into(binding.thumbnail)
    }
}
