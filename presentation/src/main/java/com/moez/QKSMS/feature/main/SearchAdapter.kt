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
import org.prauga.messages.R
import org.prauga.messages.common.Navigator
import org.prauga.messages.common.base.QkAdapter
import org.prauga.messages.common.base.QkBindingViewHolder
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.DateFormatter
import org.prauga.messages.common.util.extensions.setVisible
import org.prauga.messages.extensions.removeAccents
import org.prauga.messages.model.SearchResult
import org.prauga.messages.databinding.SearchListItemBinding
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.min

class SearchAdapter @Inject constructor(
    colors: Colors,
    private val context: Context,
    private val dateFormatter: DateFormatter,
    private val navigator: Navigator
) : QkAdapter<SearchResult, QkBindingViewHolder<SearchListItemBinding>>() {

    private val highlightColor: Int by lazy { colors.theme().highlight }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkBindingViewHolder<SearchListItemBinding> {
        val binding = SearchListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QkBindingViewHolder(binding).apply {
            itemView.setOnClickListener {
                val result = getItem(adapterPosition)
                navigator.showConversation(result.conversation.id, result.query.takeIf { result.messages > 0 })
            }
        }
    }

    override fun onBindViewHolder(holder: QkBindingViewHolder<SearchListItemBinding>, position: Int) {
        val previous = data.getOrNull(position - 1)
        val result = getItem(position)

        holder.binding.resultsHeader.setVisible(result.messages > 0 && previous?.messages == 0)

        val query = result.query
        holder.binding.title.text = highlightText(result.conversation.getTitle(), query)

        holder.binding.avatars.recipients = result.conversation.recipients

        when (result.messages == 0) {
            true -> {
                holder.binding.date.setVisible(true)
                holder.binding.date.text = dateFormatter.getConversationTimestamp(result.conversation.date)
                val snippetText = when (result.conversation.me) {
                    true -> context.getString(R.string.main_sender_you, result.conversation.snippet)
                    false -> result.conversation.snippet
                }
                holder.binding.snippet.text = highlightText(snippetText ?: "", query)
            }

            false -> {
                holder.binding.date.setVisible(false)
                holder.binding.snippet.text = context.getString(R.string.main_message_results, result.messages)
            }
        }
    }

    override fun areItemsTheSame(old: SearchResult, new: SearchResult): Boolean {
        return old.conversation.id == new.conversation.id && old.messages > 0 == new.messages > 0
    }

    override fun areContentsTheSame(old: SearchResult, new: SearchResult): Boolean {
        return old.query == new.query && // Queries are the same
                old.conversation.id == new.conversation.id // Conversation id is the same
                && old.messages == new.messages // Result count is the same
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
