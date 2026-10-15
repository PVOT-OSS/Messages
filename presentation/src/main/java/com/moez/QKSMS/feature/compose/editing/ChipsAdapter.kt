/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.compose.editing

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import org.prauga.messages.app.R
import org.prauga.messages.common.base.QkAdapter
import org.prauga.messages.common.base.QkBindingViewHolder
import org.prauga.messages.databinding.ContactChipBinding
import org.prauga.messages.model.Recipient

class ChipsAdapter @Inject constructor() :
    QkAdapter<Recipient, QkBindingViewHolder<ContactChipBinding>>() {

    var view: RecyclerView? = null
    val chipDeleted: PublishSubject<Recipient> = PublishSubject.create<Recipient>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): QkBindingViewHolder<ContactChipBinding> {
        val binding = ContactChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QkBindingViewHolder(binding).apply {
            binding.root.setOnClickListener {
                val chip = getItem(adapterPosition)
                showDetailedChip(binding.root.context, chip)
            }
        }
    }

    override fun onBindViewHolder(holder: QkBindingViewHolder<ContactChipBinding>, position: Int) {
        val recipient = getItem(position)

        holder.binding.avatar.setRecipient(recipient)
        holder.binding.name.text =
            recipient.contact?.name?.takeIf { it.isNotBlank() } ?: recipient.address
    }

    /**
     * The [context] has to come from a view, because we're inflating a view that used themed attrs
     */
    private fun showDetailedChip(context: Context, recipient: Recipient) {
        val detailedChipView = DetailedChipView(context)
        detailedChipView.setRecipient(recipient)

        val rootView = view?.rootView as ViewGroup

        val layoutParams =
            RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )

        layoutParams.topMargin = context.resources.getDimensionPixelSize(R.dimen.margin_chip_top)
        layoutParams.marginStart =
            context.resources.getDimensionPixelSize(R.dimen.margin_chip_start)

        rootView.addView(detailedChipView, layoutParams)
        detailedChipView.show()

        detailedChipView.setOnDeleteListener {
            chipDeleted.onNext(recipient)
            detailedChipView.hide()
        }
    }
}
