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
package org.prauga.messages.feature.compose

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.net.toUri
import com.jakewharton.rxbinding2.view.clicks
import com.moez.QKSMS.common.QkMediaPlayer
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import io.realm.RealmResults
import org.prauga.messages.R
import org.prauga.messages.common.Navigator
import org.prauga.messages.common.base.QkRealmAdapter
import org.prauga.messages.common.base.QkViewHolder
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.DateFormatter
import org.prauga.messages.common.util.TextViewStyler
import org.prauga.messages.common.util.extensions.dpToPx
import org.prauga.messages.common.util.extensions.setBackgroundTint
import org.prauga.messages.common.util.extensions.setPadding
import org.prauga.messages.common.util.extensions.setTint
import org.prauga.messages.common.util.extensions.setVisible
import org.prauga.messages.common.util.extensions.withAlpha
import org.prauga.messages.compat.SubscriptionManagerCompat
import org.prauga.messages.databinding.MessageListItemInBinding
import org.prauga.messages.databinding.MessageListItemOutBinding
import org.prauga.messages.extensions.isSmil
import org.prauga.messages.extensions.isText
import org.prauga.messages.extensions.joinTo
import org.prauga.messages.extensions.millisecondsToMinutes
import org.prauga.messages.extensions.truncateWithEllipses
import org.prauga.messages.feature.compose.BubbleUtils.canGroup
import org.prauga.messages.feature.compose.BubbleUtils.getBubble
import org.prauga.messages.feature.compose.part.PartsAdapter
import org.prauga.messages.feature.extensions.isEmojiOnly
import org.prauga.messages.model.Conversation
import org.prauga.messages.model.Message
import org.prauga.messages.model.Recipient
import org.prauga.messages.util.PhoneNumberUtils
import org.prauga.messages.util.Preferences
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider

class MessagesAdapter @Inject constructor(
    subscriptionManager: SubscriptionManagerCompat,
    private val context: Context,
    private val colors: Colors,
    private val dateFormatter: DateFormatter,
    private val partsAdapterProvider: Provider<PartsAdapter>,
    private val phoneNumberUtils: PhoneNumberUtils,
    private val prefs: Preferences,
    private val textViewStyler: TextViewStyler,
    private val navigator: Navigator,
) : QkRealmAdapter<Message, QkViewHolder>() {

    // Helper interface to access common views from both binding types
    private interface MessageBinding {
        val timestamp: org.prauga.messages.common.widget.QkTextView
        val sim: ImageView
        val simIndex: org.prauga.messages.common.widget.QkTextView
        val body: org.prauga.messages.common.widget.TightTextView
        val parts: org.prauga.messages.common.widget.QkContextMenuRecyclerViewLongMmsPart
        val status: org.prauga.messages.common.widget.QkTextView
        val reactions: android.widget.LinearLayout
        val reactionText: android.widget.TextView
    }

    // Wrapper for MessageListItemInBinding
    private class InBindingWrapper(private val binding: MessageListItemInBinding) : MessageBinding {
        override val timestamp = binding.timestamp
        override val sim = binding.sim
        override val simIndex = binding.simIndex
        override val body = binding.body
        override val parts = binding.parts
        override val status = binding.status
        override val reactions = binding.reactions
        override val reactionText = binding.reactionText
        val avatar = binding.avatar
    }

    // Wrapper for MessageListItemOutBinding
    private class OutBindingWrapper(private val binding: MessageListItemOutBinding) :
        MessageBinding {
        override val timestamp = binding.timestamp
        override val sim = binding.sim
        override val simIndex = binding.simIndex
        override val body = binding.body
        override val parts = binding.parts
        override val status = binding.status
        override val reactions = binding.reactions
        override val reactionText = binding.reactionText
        val cancelFrame = binding.cancelFrame
        val cancel = binding.cancel
        val sendNowIcon = binding.sendNowIcon
        val resendIcon = binding.resendIcon
    }

    class AudioState(
        var partId: Long = -1,
        var state: QkMediaPlayer.PlayingState = QkMediaPlayer.PlayingState.Stopped,
        var seekBarUpdater: Disposable? = null,
        var viewHolder: QkViewHolder? = null
    )

    companion object {
        private const val VIEW_TYPE_MESSAGE_IN = 0
        private const val VIEW_TYPE_MESSAGE_OUT = 1

        private const val MAX_MESSAGE_DISPLAY_LENGTH = 5000
    }

    // click events passed back to compose view model
    val partClicks: Subject<Long> = PublishSubject.create()
    val messageLinkClicks: Subject<Uri> = PublishSubject.create()
    val cancelSendingClicks: Subject<Long> = PublishSubject.create()
    val sendNowClicks: Subject<Long> = PublishSubject.create()
    val resendClicks: Subject<Long> = PublishSubject.create()
    val partContextMenuRegistrar: Subject<View> = PublishSubject.create()

    var data: Pair<Conversation, RealmResults<Message>>? = null
        set(value) {
            if (field === value) return

            field = value
            contactCache.clear()

            updateData(value?.second)
        }

    /**
     * Safely return the conversation, if available
     */
    private val conversation: Conversation?
        get() = data?.first?.takeIf { it.isValid }

    private val contactCache = ContactCache()
    private val expanded = HashMap<Long, Boolean>()
    private val subs = subscriptionManager.activeSubscriptionInfoList

    var theme: Colors.Theme = colors.theme()

    private val audioState = AudioState()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        // Use the parent's context to inflate the layout, otherwise link clicks will crash the app
        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == VIEW_TYPE_MESSAGE_OUT) {
            val binding = MessageListItemOutBinding.inflate(inflater, parent, false)
            binding.cancelIcon.setTint(theme.theme)
            binding.cancel.setTint(theme.theme)
            binding.sendNowIcon.setTint(theme.theme)
            binding.resendIcon.setTint(theme.theme)
            binding.body.hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE

            // register recycler view with compose activity for context menus
            partContextMenuRegistrar.onNext(binding.parts)

            QkViewHolder(binding.root).apply {
                containerView.setOnClickListener {
                    getItem(adapterPosition)?.let {
                        val statusBinding = MessageListItemOutBinding.bind(containerView)
                        when (toggleSelection(it.id, false)) {
                            true -> containerView.isActivated = isSelected(it.id)
                            false -> {
                                expanded[it.id] = statusBinding.status.visibility != View.VISIBLE
                                notifyItemChanged(adapterPosition)
                            }
                        }
                    }
                }
                containerView.setOnLongClickListener {
                    getItem(adapterPosition)?.let {
                        toggleSelection(it.id)
                        containerView.isActivated = isSelected(it.id)
                    }
                    true
                }
            }
        } else {
            val binding = MessageListItemInBinding.inflate(inflater, parent, false)
            binding.body.hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE

            // register recycler view with compose activity for context menus
            partContextMenuRegistrar.onNext(binding.parts)

            QkViewHolder(binding.root).apply {
                containerView.setOnClickListener {
                    getItem(adapterPosition)?.let {
                        val statusBinding = MessageListItemInBinding.bind(containerView)
                        when (toggleSelection(it.id, false)) {
                            true -> containerView.isActivated = isSelected(it.id)
                            false -> {
                                expanded[it.id] = statusBinding.status.visibility != View.VISIBLE
                                notifyItemChanged(adapterPosition)
                            }
                        }
                    }
                }
                containerView.setOnLongClickListener {
                    getItem(adapterPosition)?.let {
                        toggleSelection(it.id)
                        containerView.isActivated = isSelected(it.id)
                    }
                    true
                }
            }
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val message = getItem(position) ?: return
        val previous = if (position == 0) null else getItem(position - 1)
        val next = if (position == itemCount - 1) null else getItem(position + 1)

        val theme = when (message.isOutgoingMessage()) {
            true -> colors.theme()
            false -> colors.theme(contactCache[message.address])
        }

        // Create binding wrapper based on view type
        val binding: MessageBinding = if (message.isMe()) {
            val outBinding = MessageListItemOutBinding.bind(holder.containerView)
            val wrapper = OutBindingWrapper(outBinding)

            // Bind outgoing-specific views
            val isCancellable = message.isSending() && message.date > System.currentTimeMillis()
            wrapper.cancelFrame.visibility = if (isCancellable) View.VISIBLE else View.GONE
            wrapper.cancelFrame.clicks().subscribe { cancelSendingClicks.onNext(message.id) }
            wrapper.cancel.progress = 2

            if (isCancellable) {
                val delay = when (prefs.sendDelay.get()) {
                    Preferences.SEND_DELAY_SHORT -> 3000
                    Preferences.SEND_DELAY_MEDIUM -> 5000
                    Preferences.SEND_DELAY_LONG -> 10000
                    else -> 0
                }
                val progress =
                    (1 - (message.date - System.currentTimeMillis()) / delay.toFloat()) * 100

                ObjectAnimator.ofInt(wrapper.cancel, "progress", progress.toInt(), 100)
                    .setDuration(message.date - System.currentTimeMillis())
                    .start()
            }

            // bind the send now icon view
            if (message.isSending() && message.date > System.currentTimeMillis()) {
                wrapper.sendNowIcon.visibility = View.VISIBLE
                wrapper.sendNowIcon.clicks().subscribe { sendNowClicks.onNext(message.id) }
            } else {
                wrapper.sendNowIcon.visibility = View.GONE
            }

            // bind the resend icon view
            if (message.isFailedMessage()) {
                wrapper.resendIcon.visibility = View.VISIBLE
                wrapper.resendIcon.clicks().subscribe {
                    resendClicks.onNext(message.id)
                    wrapper.resendIcon.visibility = View.GONE
                }
            } else {
                wrapper.resendIcon.visibility = View.GONE
            }

            wrapper
        } else {
            val inBinding = MessageListItemInBinding.bind(holder.containerView)
            InBindingWrapper(inBinding)
        }

        // Update the selected state
        holder.containerView.isActivated = isSelected(message.id) || highlight == message.id

        val subject = message.getCleansedSubject()

        var isMsgTextTruncated = false

        // get message text to display, which may need to be truncated
        val displayText = subject.joinTo(message.getText(false), "\n").let {
            isMsgTextTruncated = (it.length > MAX_MESSAGE_DISPLAY_LENGTH)

            // make subject sub-string bold, if subject is not blank
            if (subject.isNotBlank())
                SpannableString(it.truncateWithEllipses(MAX_MESSAGE_DISPLAY_LENGTH)).apply {
                    setSpan(
                        StyleSpan(Typeface.BOLD),
                        0,
                        subject.length,
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                }
            else
                it.truncateWithEllipses(MAX_MESSAGE_DISPLAY_LENGTH)
        }

        // Bind the message status
        bindStatus(holder, binding, isMsgTextTruncated, message, next)

        // Bind the timestamp
        val subscription = subs.find { it.subscriptionId == message.subId }

        binding.timestamp.apply {
            text = dateFormatter.getMessageTimestamp(message.date)
            setVisible(
                ((message.date - (previous?.date ?: 0))
                    .millisecondsToMinutes() >= BubbleUtils.TIMESTAMP_THRESHOLD) ||
                        (message.subId != previous?.subId) &&
                        (subscription != null)
            )
        }

        binding.simIndex.text = subscription?.simSlotIndex?.plus(1)?.toString()

        ((message.subId != previous?.subId) && (subscription != null) && (subs.size > 1)).also {
            binding.sim.setVisible(it)
            binding.simIndex.setVisible(it)
        }

        // Bind the grouping
        holder.containerView.setPadding(
            bottom = if (canGroup(message, next)) 0 else 16.dpToPx(context)
        )

        val isDarkMode = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        if (!message.isMe()) {
            (binding as InBindingWrapper).avatar.apply {
                val isGroupChat = (conversation?.recipients?.size ?: 0) > 1
                if (isGroupChat) {
                    setRecipient(contactCache[message.address])
                    setVisible(!canGroup(message, next), View.INVISIBLE)
                } else {
                    visibility = View.GONE
                }
            }

            val incomingBubbleColor = if (isDarkMode) {
                context.getColor(R.color.bubbleIncomingDark)
            } else {
                context.getColor(R.color.bubbleIncomingLight)
            }
            val incomingTextColor = if (isDarkMode) {
                context.getColor(R.color.white)
            } else {
                context.getColor(R.color.black)
            }

            binding.body.apply {
                setTextColor(incomingTextColor)
                setBackgroundTint(incomingBubbleColor)
                highlightColor = incomingBubbleColor.withAlpha(0x5d)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    textSelectHandle?.setTint(incomingBubbleColor.withAlpha(0x7d))
                    textSelectHandleLeft?.setTint(incomingBubbleColor.withAlpha(0x7d))
                    textSelectHandleRight?.setTint(incomingBubbleColor.withAlpha(0x7d))
                }
            }
        } else {
            val outgoingBubbleColor = context.getColor(R.color.bubbleOutgoing)
            val outgoingTextColor = context.getColor(R.color.bubbleOutgoingText)

            binding.body.apply {
                setTextColor(outgoingTextColor)
                setBackgroundTint(outgoingBubbleColor)
                highlightColor = outgoingBubbleColor.withAlpha(0x5d)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    textSelectHandle?.setTint(outgoingBubbleColor.withAlpha(0xad))
                    textSelectHandleLeft?.setTint(outgoingBubbleColor.withAlpha(0xad))
                    textSelectHandleRight?.setTint(outgoingBubbleColor.withAlpha(0xad))
                }
            }
        }

        // Bind the body text
        val emojiOnly = displayText.isEmojiOnly()
        textViewStyler.setTextSize(
            binding.body,
            when (emojiOnly) {
                true -> TextViewStyler.SIZE_EMOJI
                false -> TextViewStyler.SIZE_PRIMARY
            }
        )

        val spanString = SpannableStringBuilder(displayText)

        when (prefs.messageLinkHandling.get()) {
            Preferences.MESSAGE_LINK_HANDLING_BLOCK -> binding.body.autoLinkMask = 0
            Preferences.MESSAGE_LINK_HANDLING_ASK -> {
                //  manually handle link clicks if user has set to ask before opening links
                binding.body.apply {
                    isClickable = false
                    linksClickable = false
                    movementMethod = LinkMovementMethod.getInstance()

                    Linkify.addLinks(spanString, autoLinkMask)
                }

                spanString.apply {
                    for (span in getSpans(0, length, URLSpan::class.java)) {
                        // set handler for when user touches a link into new span
                        setSpan(
                            object : ClickableSpan() {
                                override fun onClick(widget: View) {
                                    messageLinkClicks.onNext(span.url.toUri())
                                }
                            },
                            getSpanStart(span),
                            getSpanEnd(span),
                            getSpanFlags(span)
                        )

                        // remove original span
                        removeSpan(span)
                    }
                }
            }

            else -> binding.body.movementMethod = LinkMovementMethod.getInstance()
        }

        binding.body.apply {
            text = spanString
            setVisible(message.isSms() || spanString.isNotBlank())

            setBackgroundResource(
                getBubble(
                    emojiOnly = emojiOnly,
                    canGroupWithPrevious = canGroup(message, previous) ||
                            message.parts.any { !it.isSmil() && !it.isText() },
                    canGroupWithNext = canGroup(message, next),
                    isMe = message.isMe()
                )
            )
        }

        // Bind the parts
        binding.parts.adapter = partsAdapterProvider.get().apply {
            this.theme = theme
            setData(message, previous, next, holder, audioState)
            contextMenuValue = message.id
            clicks.subscribe(partClicks)    // part clicks gets passed back to compose view model
        }

        showEmojiReactions(binding, message)
    }

    private fun showEmojiReactions(binding: MessageBinding, message: Message) {
        val reactions = message.emojiReactions
        val hasReactions = reactions.isNotEmpty()

        if (hasReactions) {
            val reactionCounts = reactions.groupBy { it.emoji }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second } // Sort by count, most reactions first

            // For now, show just the first (most popular) reaction
            val topReaction = reactionCounts.first()
            val reactionText = if (topReaction.second == 1) {
                topReaction.first
            } else {
                // Use a non-breaking space to keep the emoji and count together
                "${topReaction.first}\u00A0${topReaction.second}"
            }

            binding.reactionText.text = reactionText
            binding.reactions.setVisible(true)
            makeRoomForEmojis(binding.reactions)
        } else {
            binding.reactions.setVisible(false)
        }
    }

    private fun makeRoomForEmojis(reactionsContainer: View) {
        val paddingBottom = context.resources.getDimensionPixelSize(R.dimen.padding_reactions)

        (reactionsContainer.parent?.parent as? ViewGroup)?.let { parent ->
            parent.setPadding(
                parent.paddingLeft,
                parent.paddingTop,
                parent.paddingRight,
                paddingBottom
            )
        }
    }

    private fun bindStatus(
        holder: QkViewHolder,
        binding: MessageBinding,
        bodyTextTruncated: Boolean,
        message: Message,
        next: Message?
    ) {
        binding.status.apply {
            text = when {
                message.isSending() -> context.getString(R.string.message_status_sending)
                message.isDelivered() -> context.getString(
                    R.string.message_status_delivered,
                    dateFormatter.getTimestamp(message.dateSent)
                )

                message.isFailedMessage() -> context.getString(R.string.message_status_failed)
                bodyTextTruncated -> context.getString(R.string.message_body_too_long_to_display)
                (!message.isMe() && (conversation?.recipients?.size ?: 0) > 1) ->
                    // incoming group message
                    "${contactCache[message.address]?.getDisplayName()} • ${
                        dateFormatter.getTimestamp(message.date)
                    }"

                else -> dateFormatter.getTimestamp(message.date)
            }

            val age = TimeUnit.MILLISECONDS.toMinutes(
                System.currentTimeMillis() - message.date
            )

            setVisible(
                when {
                    expanded[message.id] == true -> true
                    message.isSending() -> true
                    message.isFailedMessage() -> true
                    bodyTextTruncated -> true
                    expanded[message.id] == false -> false
                    ((conversation?.recipients?.size ?: 0) > 1) &&
                            !message.isMe() && next?.compareSender(message) != true -> true

                    (message.isDelivered() &&
                            (next?.isDelivered() != true) &&
                            (age <= BubbleUtils.TIMESTAMP_THRESHOLD)) -> true

                    else -> false
                }
            )
        }
    }

    override fun getItemId(position: Int): Long {
        return getItem(position)?.id ?: -1
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position) ?: return -1
        return when (message.isMe()) {
            true -> VIEW_TYPE_MESSAGE_OUT
            false -> VIEW_TYPE_MESSAGE_IN
        }
    }

    fun expandMessages(messageIds: List<Long>, expand: Boolean) {
        messageIds.forEach { expanded[it] = expand }
        notifyDataSetChanged()
    }

    /**
     * Cache the contacts in a map by the address, because the messages we're binding don't have
     * a reference to the contact.
     */
    private inner class ContactCache : HashMap<String, Recipient?>() {
        override fun get(key: String): Recipient? {
            if (super.get(key)?.isValid != true)
                set(
                    key,
                    conversation?.recipients?.firstOrNull {
                        phoneNumberUtils.compare(it.address, key)
                    }
                )

            return super.get(key)?.takeIf { it.isValid }
        }

    }
}
