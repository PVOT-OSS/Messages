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
package org.prauga.messages.feature.main

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.prauga.messages.common.base.QkAdapter
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.DateFormatter
import org.prauga.messages.databinding.SearchItemHeaderBinding
import org.prauga.messages.databinding.SearchItemMessageBinding
import org.prauga.messages.extensions.removeAccents
import org.prauga.messages.model.SearchItem
import org.prauga.messages.repository.ConversationRepository
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.min

class SearchAdapter @Inject constructor(
    colors: Colors,
    private val context: Context,
    private val dateFormatter: DateFormatter,
    private val conversationRepo: ConversationRepository
) : QkAdapter<SearchItem, RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_MESSAGE = 1
    }

    private val highlightColor: Int by lazy { colors.theme().highlight }
    private var query: String = ""
    var onMessageClickListener: ((conversationId: Long, messageId: Long) -> Unit)? = null

    fun setQuery(query: String) {
        this.query = query
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SearchItem.Header -> VIEW_TYPE_HEADER
            is SearchItem.Message -> VIEW_TYPE_MESSAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = SearchItemHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }
            VIEW_TYPE_MESSAGE -> {
                val binding = SearchItemMessageBinding.inflate(inflater, parent, false)
                MessageViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SearchItem.Header -> (holder as HeaderViewHolder).bind(item)
            is SearchItem.Message -> (holder as MessageViewHolder).bind(item)
        }
    }

    override fun areItemsTheSame(old: SearchItem, new: SearchItem): Boolean {
        return when {
            old is SearchItem.Header && new is SearchItem.Header ->
                old.conversationId == new.conversationId
            old is SearchItem.Message && new is SearchItem.Message ->
                old.messageId == new.messageId
            else -> false
        }
    }

    override fun areContentsTheSame(old: SearchItem, new: SearchItem): Boolean {
        return when {
            old is SearchItem.Header && new is SearchItem.Header ->
                old.title == new.title
            old is SearchItem.Message && new is SearchItem.Message ->
                old.body == new.body && old.timestamp == new.timestamp
            else -> false
        }
    }

    inner class HeaderViewHolder(
        private val binding: SearchItemHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(header: SearchItem.Header) {
            binding.headerTitle.text = highlightText(header.title, query)
            
            // Get conversation to access recipients for avatar and phone number
            val conversation = conversationRepo.getConversation(header.conversationId)
            conversation?.let {
                binding.headerAvatar.recipients = it.recipients
                
                // Show phone number(s) as subtitle
                val phoneNumbers = it.recipients.joinToString(", ") { recipient -> 
                    recipient.address 
                }
                binding.headerSubtitle.text = phoneNumbers
            }
        }
    }

    inner class MessageViewHolder(
        private val binding: SearchItemMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    if (item is SearchItem.Message) {
                        onMessageClickListener?.invoke(item.conversationId, item.messageId)
                    }
                }
            }
        }

        fun bind(message: SearchItem.Message) {
            binding.messageBody.text = highlightText(message.body, query)
            // Use getConversationTimestamp for date (no time included)
            binding.messageDate.text = dateFormatter.getConversationTimestamp(message.timestamp)
            // Use getTimestamp for time only
            binding.messageTime.text = dateFormatter.getTimestamp(message.timestamp)
        }
    }

    private fun highlightText(text: CharSequence, query: CharSequence): SpannableString {
        if (query.isEmpty()) return SpannableString(text)

        val original = text.toString()
        val normalizedText = original.removeAccents()
        val normalizedQuery = query.toString()
        val lowerText = normalizedText.lowercase()
        val lowerQuery = normalizedQuery.lowercase()
        val spannable = SpannableString(original)

        fun applySpan(start: Int, end: Int) {
            if (start < 0 || end > spannable.length || start >= end) return
            spannable.setSpan(
                BackgroundColorSpan(highlightColor),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        var index = lowerText.indexOf(lowerQuery)
        if (index >= 0) {
            while (index >= 0) {
                applySpan(index, index + lowerQuery.length)
                index = lowerText.indexOf(lowerQuery, index + lowerQuery.length)
            }
            return spannable
        }

        // Fuzzy fallback: find the closest window of text roughly query-length.
        val window = lowerQuery.length
        var bestIndex = -1
        var bestDistance = window + 1
        for (i in 0..(lowerText.length - window).coerceAtLeast(0)) {
            val candidate = lowerText.substring(i, min(lowerText.length, i + window))
            val distance = levenshtein(candidate, lowerQuery, maxDistance = 2)
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = i
                if (bestDistance == 0) break
            }
        }

        if (bestIndex >= 0 && bestDistance <= 2) {
            applySpan(bestIndex, bestIndex + window)
        }

        return spannable
    }

    private fun levenshtein(lhs: String, rhs: String, maxDistance: Int): Int {
        if (lhs == rhs) return 0
        if (abs(lhs.length - rhs.length) > maxDistance) return maxDistance + 1
        if (lhs.isEmpty() || rhs.isEmpty()) return maxDistance + 1

        val prev = IntArray(rhs.length + 1) { it }
        val curr = IntArray(rhs.length + 1)

        lhs.forEachIndexed { i, lChar ->
            curr[0] = i + 1
            var bestInRow = curr[0]
            rhs.forEachIndexed { j, rChar ->
                val cost = if (lChar == rChar) 0 else 1
                curr[j + 1] = min(
                    prev[j + 1] + 1, // deletion
                    min(
                        curr[j] + 1, // insertion
                        prev[j] + cost // substitution
                    )
                )
                bestInRow = min(bestInRow, curr[j + 1])
            }
            if (bestInRow > maxDistance) return maxDistance + 1
            System.arraycopy(curr, 0, prev, 0, curr.size)
        }

        return prev[rhs.length]
    }
}
