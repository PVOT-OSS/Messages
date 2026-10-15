/*
 * SPDX-FileCopyrightText: 2025 peanut250 <209579534+peanut250@users.noreply.github.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.blocking.filters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.prauga.messages.common.base.QkBindingViewHolder
import org.prauga.messages.common.base.QkRealmAdapter
import org.prauga.messages.databinding.MessageContentFilterListItemBinding
import org.prauga.messages.model.MessageContentFilter

class MessageContentFiltersAdapter :
    QkRealmAdapter<
        MessageContentFilter,
        QkBindingViewHolder<MessageContentFilterListItemBinding>,
    >() {

    val removeMessageContentFilter: Subject<Long> = PublishSubject.create()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): QkBindingViewHolder<MessageContentFilterListItemBinding> {
        val binding =
            MessageContentFilterListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return QkBindingViewHolder(binding).apply {
            binding.removeFilter.setOnClickListener {
                val filter = getItem(adapterPosition) ?: return@setOnClickListener
                removeMessageContentFilter.onNext(filter.id)
            }
        }
    }

    override fun onBindViewHolder(
        holder: QkBindingViewHolder<MessageContentFilterListItemBinding>,
        position: Int,
    ) {
        val item = getItem(position)!!
        holder.binding.caseIcon.visibility = if (item.caseSensitive) View.VISIBLE else View.GONE
        holder.binding.regexIcon.visibility = if (item.isRegex) View.VISIBLE else View.GONE
        holder.binding.contactsIcon.visibility =
            if (item.includeContacts) View.VISIBLE else View.GONE
        holder.binding.filter.text = item.value
    }
}
