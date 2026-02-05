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
package org.prauga.messages.feature.conversations

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.prauga.messages.R
import org.prauga.messages.common.Navigator
import org.prauga.messages.common.base.QkRealmAdapter
import org.prauga.messages.common.base.QkViewHolder
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.DateFormatter
import org.prauga.messages.common.util.OtpDetector
import org.prauga.messages.app.utils.ParcelDetector
import org.prauga.messages.common.util.extensions.resolveThemeColor
import org.prauga.messages.common.util.extensions.setTint
import org.prauga.messages.databinding.ConversationListItemBinding
import org.prauga.messages.model.Conversation
import org.prauga.messages.repository.ScheduledMessageRepository
import org.prauga.messages.util.PhoneNumberUtils
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class ConversationsAdapter @Inject constructor(
    private val colors: Colors,
    private val context: Context,
    private val dateFormatter: DateFormatter,
    private val scheduledMessageRepo: ScheduledMessageRepository,
    private val navigator: Navigator,
    private val phoneNumberUtils: PhoneNumberUtils
) : QkRealmAdapter<Conversation, QkViewHolder>() {
    private val disposables = CompositeDisposable()

    init {
        // This is how we access the threadId for the swipe actions
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ConversationListItemBinding.inflate(layoutInflater, parent, false)

        binding.title.setTypeface(binding.title.typeface, Typeface.BOLD)

        if (viewType == 1) {
            val textColorPrimary = parent.context.resolveThemeColor(android.R.attr.textColorPrimary)

            binding.snippet.setTypeface(binding.snippet.typeface, Typeface.BOLD)
            binding.snippet.setTextColor(textColorPrimary)
            binding.snippet.maxLines = 5

            binding.unread.isVisible = true

            binding.date.setTypeface(binding.date.typeface, Typeface.BOLD)
            binding.date.setTextColor(textColorPrimary)
        }

        return QkViewHolder(binding.root).apply {
            binding.root.setOnClickListener {
                val conversation = getItem(bindingAdapterPosition) ?: return@setOnClickListener
                when (toggleSelection(conversation.id, false)) {
                    true -> binding.root.isActivated = isSelected(conversation.id)
                    false -> navigator.showConversation(conversation.id)
                }
            }
            binding.root.setOnLongClickListener {
                val conversation = getItem(bindingAdapterPosition) ?: return@setOnLongClickListener true
                toggleSelection(conversation.id)
                binding.root.isActivated = isSelected(conversation.id)
                true
            }
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val conversation = getItem(position) ?: return
        val binding = ConversationListItemBinding.bind(holder.containerView)

        // If the last message wasn't incoming, then the colour doesn't really matter anyway
        val lastMessage = conversation.lastMessage
        val recipient = when {
            conversation.recipients.size == 1 || lastMessage == null -> conversation.recipients.firstOrNull()
            else -> conversation.recipients.find { recipient ->
                phoneNumberUtils.compare(recipient.address, lastMessage.address)
            }
        }
        val theme = colors.theme(recipient).theme

        holder.containerView.isActivated = isSelected(conversation.id)

        binding.avatars.recipients = conversation.recipients
        binding.title.collapseEnabled = conversation.recipients.size > 1
        binding.title.text = buildSpannedString {
            append(conversation.getTitle())
        }
        binding.date.text = conversation.date.takeIf { it > 0 }?.let(dateFormatter::getConversationTimestamp)
        binding.snippet.text = when {
            conversation.draft.isNotEmpty() -> context.getString(R.string.main_sender_draft, conversation.draft)
            conversation.me -> context.getString(R.string.main_sender_you, conversation.snippet)
            else -> conversation.snippet
        }

        // Make the preview in italics if draft
        if (conversation.draft.isNotEmpty()) binding.snippet.setTypeface(null, Typeface.ITALIC)

        // Get Scheduled Messages
        val disposable = scheduledMessageRepo
            .getScheduledMessagesForConversation(conversation.id)
            .asFlowable()
            .toObservable()
            .subscribe { messages ->
                binding.scheduled.isVisible = messages.isNotEmpty()
            }
        disposables.add(disposable)

        binding.pinned.isVisible = conversation.pinned

        // Check if the conversation contains OTP (One Time Password)
        // 1. Initialize OTP detector
        val otpDetector = OtpDetector()
        // 2. Get message snippet, handle possible null value
        val snippet = conversation.snippet ?: ""
        // 3. Perform OTP detection
        val otpResult = otpDetector.detect(snippet)
        // 4. Show or hide OTP tag based on detection result
        binding.otpTag.isVisible = otpResult.isOtp
        
        if (otpResult.isOtp) {
            // Get OTP tag text from string resources (Android will automatically use the appropriate translation)
            val otpText = context.getString(R.string.otp_tag)
            binding.otpTag.text = otpText
            
            // Set OTP tag background and text color to match theme
            val theme = colors.theme(recipient).theme
            binding.otpTag.background.setTint(theme)
            binding.otpTag.setTextColor(colors.theme(recipient).textPrimary)
        }
        
        // Check if the conversation contains parcel pickup code
        // 1. Initialize Parcel detector
        val parcelDetector = ParcelDetector()
        // 2. Perform parcel code detection
        val parcelResult = parcelDetector.detectParcel(snippet)
        // 3. Show or hide parcel tag based on detection result
        binding.parcelTag.isVisible = parcelResult.success
        
        if (parcelResult.success) {
            // Get parcel tag text from string resources (Android will automatically use the appropriate translation)
            val parcelText = context.getString(R.string.parcel_tag)
            binding.parcelTag.text = parcelText
            
            // Set parcel tag background and text color to match theme
            val theme = colors.theme(recipient).theme
            binding.parcelTag.background.setTint(theme)
            binding.parcelTag.setTextColor(colors.theme(recipient).textPrimary)
        }
    }

    override fun getItemId(position: Int): Long {
        return getItem(position)?.id ?: -1
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position)?.unread == false) 0 else 1
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        disposables.clear()
    }

    /**
     * Public method to start selection mode by selecting the first item
     */
    fun startSelectionMode() {
        if (itemCount > 0) {
            val firstId = getItemId(0)
            toggleSelection(firstId, force = true)
            notifyItemChanged(0)
        }
    }

}
