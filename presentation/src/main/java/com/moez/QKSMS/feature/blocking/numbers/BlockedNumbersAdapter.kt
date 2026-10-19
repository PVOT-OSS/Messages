/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.blocking.numbers

import android.view.LayoutInflater
import android.view.ViewGroup
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.prauga.messages.common.base.QkBindingViewHolder
import org.prauga.messages.common.base.QkRealmAdapter
import org.prauga.messages.databinding.BlockedNumberListItemBinding
import org.prauga.messages.model.BlockedNumber

class BlockedNumbersAdapter :
    QkRealmAdapter<BlockedNumber, QkBindingViewHolder<BlockedNumberListItemBinding>>() {

    val unblockAddress: Subject<Long> = PublishSubject.create()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): QkBindingViewHolder<BlockedNumberListItemBinding> {
        val binding =
            BlockedNumberListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QkBindingViewHolder(binding).apply {
            binding.unblock.setOnClickListener {
                val number = getItem(adapterPosition) ?: return@setOnClickListener
                unblockAddress.onNext(number.id)
            }
        }
    }

    override fun onBindViewHolder(
        holder: QkBindingViewHolder<BlockedNumberListItemBinding>,
        position: Int,
    ) {
        val item = getItem(position)!!

        holder.binding.number.text = item.address
    }
}
