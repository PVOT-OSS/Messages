/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.blocking.messages

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import org.prauga.messages.R
import org.prauga.messages.common.base.QkRealmAdapter
import org.prauga.messages.common.base.QkViewHolder
import org.prauga.messages.common.util.DateFormatter
import org.prauga.messages.common.util.extensions.resolveThemeColor
import org.prauga.messages.databinding.BlockedListItemBinding
import org.prauga.messages.model.Conversation
import org.prauga.messages.util.Preferences

class BlockedMessagesAdapter
@Inject
constructor(private val context: Context, private val dateFormatter: DateFormatter) :
    QkRealmAdapter<Conversation, QkViewHolder>() {

    val clicks: PublishSubject<Long> = PublishSubject.create()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = BlockedListItemBinding.inflate(inflater, parent, false)

        if (viewType == 0) {
            binding.title.setTypeface(binding.title.typeface, Typeface.BOLD)
            binding.date.setTypeface(binding.date.typeface, Typeface.BOLD)
            binding.date.setTextColor(
                binding.root.context.resolveThemeColor(android.R.attr.textColorPrimary)
            )
        }

        return QkViewHolder(binding.root).apply {
            binding.root.setOnClickListener {
                val conversation = getItem(adapterPosition) ?: return@setOnClickListener
                when (toggleSelection(conversation.id, false)) {
                    true -> binding.root.isActivated = isSelected(conversation.id)
                    false -> clicks.onNext(conversation.id)
                }
            }
            binding.root.setOnLongClickListener {
                val conversation = getItem(adapterPosition) ?: return@setOnLongClickListener true
                toggleSelection(conversation.id)
                binding.root.isActivated = isSelected(conversation.id)
                true
            }
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val conversation = getItem(position) ?: return
        val binding = BlockedListItemBinding.bind(holder.containerView)

        holder.containerView.isActivated = isSelected(conversation.id)

        binding.avatars.recipients = conversation.recipients
        binding.title.collapseEnabled = conversation.recipients.size > 1
        binding.title.text = conversation.getTitle()
        binding.date.text = dateFormatter.getConversationTimestamp(conversation.date)

        binding.blocker.text =
            when (conversation.blockingClient) {
                Preferences.BLOCKING_MANAGER_CC ->
                    context.getString(R.string.blocking_manager_call_control_title)
                Preferences.BLOCKING_MANAGER_SIA ->
                    context.getString(R.string.blocking_manager_sia_title)
                else -> null
            }

        binding.reason.text = conversation.blockReason
        binding.blocker.isVisible = binding.blocker.text.isNotEmpty()
        binding.reason.isVisible = binding.blocker.text.isNotEmpty()
    }

    override fun getItemViewType(position: Int): Int {
        val conversation = getItem(position)
        return if (conversation?.unread == false) 1 else 0
    }
}
