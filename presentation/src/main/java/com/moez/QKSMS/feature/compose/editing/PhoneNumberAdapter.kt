/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.compose.editing

import android.view.LayoutInflater
import android.view.ViewGroup
import org.prauga.messages.common.base.QkAdapter
import org.prauga.messages.common.base.QkBindingViewHolder
import org.prauga.messages.databinding.ContactNumberListItemBinding
import org.prauga.messages.model.PhoneNumber

class PhoneNumberAdapter :
    QkAdapter<PhoneNumber, QkBindingViewHolder<ContactNumberListItemBinding>>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): QkBindingViewHolder<ContactNumberListItemBinding> {
        val binding =
            ContactNumberListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QkBindingViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: QkBindingViewHolder<ContactNumberListItemBinding>,
        position: Int,
    ) {
        val number = getItem(position)

        holder.binding.address.text = number.address
        holder.binding.type.text = number.type
    }

    override fun areItemsTheSame(old: PhoneNumber, new: PhoneNumber): Boolean {
        return old.type == new.type && old.address == new.address
    }

    override fun areContentsTheSame(old: PhoneNumber, new: PhoneNumber): Boolean {
        return old.type == new.type && old.address == new.address
    }
}
