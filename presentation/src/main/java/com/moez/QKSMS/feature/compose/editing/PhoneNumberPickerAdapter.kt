/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.compose.editing

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import org.prauga.messages.R
import org.prauga.messages.common.base.QkAdapter
import org.prauga.messages.common.base.QkViewHolder
import org.prauga.messages.common.util.extensions.forwardTouches
import org.prauga.messages.databinding.PhoneNumberListItemBinding
import org.prauga.messages.extensions.Optional
import org.prauga.messages.model.PhoneNumber

class PhoneNumberPickerAdapter @Inject constructor(private val context: Context) :
    QkAdapter<PhoneNumber, QkViewHolder>() {

    val selectedItemChanges: Subject<Optional<Long>> = BehaviorSubject.create()

    private var selectedItem: Long? = null
        set(value) {
            data
                .indexOfFirst { number -> number.id == field }
                .takeIf { it != -1 }
                ?.run(::notifyItemChanged)
            field = value
            data
                .indexOfFirst { number -> number.id == field }
                .takeIf { it != -1 }
                ?.run(::notifyItemChanged)
            selectedItemChanges.onNext(Optional(value))
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = PhoneNumberListItemBinding.inflate(inflater, parent, false)
        return QkViewHolder(binding.root).apply {
            binding.number.radioButton.forwardTouches(itemView)

            binding.root.setOnClickListener {
                val phoneNumber = getItem(adapterPosition)
                selectedItem = phoneNumber.id
            }
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val phoneNumber = getItem(position)
        val binding = PhoneNumberListItemBinding.bind(holder.containerView)

        binding.number.radioButton.isChecked = phoneNumber.id == selectedItem
        binding.number.titleView.text = phoneNumber.address
        binding.number.summaryView.text =
            when (phoneNumber.isDefault) {
                true -> context.getString(R.string.compose_number_picker_default, phoneNumber.type)
                false -> phoneNumber.type
            }
    }

    override fun onDatasetChanged() {
        super.onDatasetChanged()
        selectedItem = data.find { number -> number.isDefault }?.id ?: data.firstOrNull()?.id
    }
}
