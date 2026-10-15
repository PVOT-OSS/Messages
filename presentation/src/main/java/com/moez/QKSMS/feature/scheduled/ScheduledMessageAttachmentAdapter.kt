/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.scheduled

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import javax.inject.Inject
import org.prauga.messages.R
import org.prauga.messages.common.base.QkAdapter
import org.prauga.messages.common.base.QkBindingViewHolder
import org.prauga.messages.databinding.ScheduledMessageImageListItemBinding
import org.prauga.messages.extensions.getName
import org.prauga.messages.feature.extensions.LoadBestIconIntoImageView
import org.prauga.messages.feature.extensions.loadBestIconIntoImageView

class ScheduledMessageAttachmentAdapter @Inject constructor(private val context: Context) :
    QkAdapter<Uri, QkBindingViewHolder<ScheduledMessageImageListItemBinding>>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): QkBindingViewHolder<ScheduledMessageImageListItemBinding> =
        QkBindingViewHolder(
            ScheduledMessageImageListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        )

    override fun onBindViewHolder(
        holder: QkBindingViewHolder<ScheduledMessageImageListItemBinding>,
        position: Int,
    ) {
        val uri = getItem(position)

        // set best image and text to use for icon
        when (getItem(position).loadBestIconIntoImageView(context, holder.binding.thumbnail)) {
            LoadBestIconIntoImageView.Missing -> {
                holder.binding.fileName.text = context.getString(R.string.attachment_missing)
                holder.binding.fileName.visibility = View.VISIBLE
            }
            LoadBestIconIntoImageView.ActivityIcon,
            LoadBestIconIntoImageView.DefaultAudioIcon,
            LoadBestIconIntoImageView.GenericIcon -> {
                // generic style icon used, also show name
                holder.binding.fileName.text = uri.getName(context)
                holder.binding.fileName.visibility = View.VISIBLE
            }
            else -> holder.binding.fileName.visibility = View.GONE
        }
    }
}
