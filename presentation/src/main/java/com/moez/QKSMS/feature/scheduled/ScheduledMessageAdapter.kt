/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.scheduled

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import org.prauga.messages.common.base.QkBindingViewHolder
import org.prauga.messages.common.base.QkRealmAdapter
import org.prauga.messages.common.util.DateFormatter
import org.prauga.messages.databinding.ScheduledMessageListItemBinding
import org.prauga.messages.model.Contact
import org.prauga.messages.model.Recipient
import org.prauga.messages.model.ScheduledMessage
import org.prauga.messages.repository.ContactRepository
import org.prauga.messages.util.PhoneNumberUtils

class ScheduledMessageAdapter
@Inject
constructor(
    private val context: Context,
    private val contactRepo: ContactRepository,
    private val dateFormatter: DateFormatter,
    private val phoneNumberUtils: PhoneNumberUtils,
) : QkRealmAdapter<ScheduledMessage, QkBindingViewHolder<ScheduledMessageListItemBinding>>() {

    private val contacts by lazy { contactRepo.getContacts() }
    private val contactCache = ContactCache()
    private val imagesViewPool = RecyclerView.RecycledViewPool()

    val clicks: Subject<Long> = PublishSubject.create()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): QkBindingViewHolder<ScheduledMessageListItemBinding> {
        val binding =
            ScheduledMessageListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )

        binding.attachments.adapter = ScheduledMessageAttachmentAdapter(context)
        binding.attachments.setRecycledViewPool(imagesViewPool)

        return QkBindingViewHolder(binding).apply {
            binding.root.setOnClickListener {
                val message = getItem(adapterPosition) ?: return@setOnClickListener
                if (toggleSelection(message.id, false))
                    binding.root.isActivated = isSelected(message.id)
            }
            binding.root.setOnClickListener {
                val message = getItem(adapterPosition) ?: return@setOnClickListener
                toggleSelection(message.id)
                binding.root.isActivated = isSelected(message.id)
            }
        }
    }

    override fun onBindViewHolder(
        holder: QkBindingViewHolder<ScheduledMessageListItemBinding>,
        position: Int,
    ) {
        val message = getItem(position) ?: return

        // GroupAvatarView only accepts recipients, so map the phone numbers to recipients
        holder.binding.avatars.recipients =
            message.recipients.map { address -> Recipient(address = address) }

        holder.binding.recipients.text =
            message.recipients.joinToString(",") { address ->
                contactCache[address]?.name?.takeIf { it.isNotBlank() } ?: address
            }

        holder.binding.date.text = dateFormatter.getScheduledTimestamp(message.date)
        holder.binding.body.text = message.body

        // update the selected/highlighted state
        holder.binding.root.isActivated = isSelected(message.id) || highlight == message.id

        val adapter = holder.binding.attachments.adapter as ScheduledMessageAttachmentAdapter
        adapter.data = message.attachments.map(Uri::parse)
        holder.binding.attachments.isVisible = message.attachments.isNotEmpty()
    }

    override fun getItemId(position: Int): Long {
        return getItem(position)?.id ?: -1
    }

    /**
     * Cache the contacts in a map by the address, because the messages we're binding don't have a
     * reference to the contact.
     */
    private inner class ContactCache : HashMap<String, Contact?>() {

        override fun get(key: String): Contact? {
            if (super.get(key)?.isValid != true) {
                set(
                    key,
                    contacts.firstOrNull { contact ->
                        contact.numbers.any { phoneNumberUtils.compare(it.address, key) }
                    },
                )
            }
            return super.get(key)?.takeIf { it.isValid }
        }
    }
}
