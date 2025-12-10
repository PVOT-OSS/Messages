/*
 * Copyright (C) 2019 Moez Bhatti <moez.bhatti@gmail.com>
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
package org.prauga.messages.feature.compose.editing

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.prauga.messages.R
import org.prauga.messages.common.base.QkAdapter
import org.prauga.messages.common.base.QkViewHolder
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.extensions.forwardTouches
import org.prauga.messages.common.util.extensions.setTint
import org.prauga.messages.databinding.ContactListItemBinding
import org.prauga.messages.extensions.associateByNotNull
import org.prauga.messages.model.Contact
import org.prauga.messages.model.ContactGroup
import org.prauga.messages.model.Conversation
import org.prauga.messages.model.Recipient
import org.prauga.messages.repository.ConversationRepository
import javax.inject.Inject

class ComposeItemAdapter @Inject constructor(
    private val colors: Colors,
    private val conversationRepo: ConversationRepository
) : QkAdapter<ComposeItem, QkViewHolder>() {

    val clicks: Subject<ComposeItem> = PublishSubject.create()
    val longClicks: Subject<ComposeItem> = PublishSubject.create()

    private val numbersViewPool = RecyclerView.RecycledViewPool()
    private val disposables = CompositeDisposable()

    var recipients: Map<String, Recipient> = mapOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ContactListItemBinding.inflate(layoutInflater, parent, false)

        binding.icon.setTint(colors.theme().theme)

        binding.numbers.setRecycledViewPool(numbersViewPool)
        binding.numbers.adapter = PhoneNumberAdapter()
        binding.numbers.forwardTouches(binding.root)

        return QkViewHolder(binding.root).apply {
            binding.root.setOnClickListener {
                val item = getItem(adapterPosition)
                clicks.onNext(item)
            }
            binding.root.setOnLongClickListener {
                val item = getItem(adapterPosition)
                longClicks.onNext(item)
                true
            }
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val prevItem = if (position > 0) getItem(position - 1) else null
        val item = getItem(position)

        when (item) {
            is ComposeItem.New -> bindNew(holder, item.value)
            is ComposeItem.Recent -> bindRecent(holder, item.value, prevItem)
            is ComposeItem.Starred -> bindStarred(holder, item.value, prevItem)
            is ComposeItem.Person -> bindPerson(holder, item.value, prevItem)
            is ComposeItem.Group -> bindGroup(holder, item.value, prevItem)
        }
    }

    private fun bindNew(holder: QkViewHolder, contact: Contact) {
        val binding = ContactListItemBinding.bind(holder.containerView)

        binding.index.isVisible = false

        binding.icon.isVisible = false

        binding.avatar.recipients = listOf(createRecipient(contact))

        binding.title.text = contact.numbers.joinToString { it.address }

        binding.subtitle.isVisible = false

        binding.numbers.isVisible = false
    }

    private fun bindRecent(holder: QkViewHolder, conversation: Conversation, prev: ComposeItem?) {
        val binding = ContactListItemBinding.bind(holder.containerView)

        binding.index.isVisible = false

        binding.icon.isVisible = prev !is ComposeItem.Recent
        binding.icon.setImageResource(R.drawable.ic_history_black_24dp)

        binding.avatar.recipients = conversation.recipients

        binding.title.text = conversation.getTitle()

        binding.subtitle.isVisible = conversation.recipients.size > 1 && conversation.name.isBlank()
        binding.subtitle.text = conversation.recipients.joinToString(", ") { recipient ->
            recipient.contact?.name ?: recipient.address
        }
        binding.subtitle.collapseEnabled = conversation.recipients.size > 1

        binding.numbers.isVisible = conversation.recipients.size == 1
        (binding.numbers.adapter as PhoneNumberAdapter).data = conversation.recipients
            .mapNotNull { recipient -> recipient.contact }
            .flatMap { contact -> contact.numbers }
    }

    private fun bindStarred(holder: QkViewHolder, contact: Contact, prev: ComposeItem?) {
        val binding = ContactListItemBinding.bind(holder.containerView)

        binding.index.isVisible = false

        binding.icon.isVisible = prev !is ComposeItem.Starred
        binding.icon.setImageResource(R.drawable.ic_star_black_24dp)

        binding.avatar.recipients = listOf(createRecipient(contact))

        binding.title.text = contact.name

        binding.subtitle.isVisible = false

        binding.numbers.isVisible = true
        (binding.numbers.adapter as PhoneNumberAdapter).data = contact.numbers
    }

    private fun bindGroup(holder: QkViewHolder, group: ContactGroup, prev: ComposeItem?) {
        val binding = ContactListItemBinding.bind(holder.containerView)

        binding.index.isVisible = false

        binding.icon.isVisible = prev !is ComposeItem.Group
        binding.icon.setImageResource(R.drawable.ic_people_black_24dp)

        binding.avatar.recipients = group.contacts.map(::createRecipient)

        binding.title.text = group.title

        binding.subtitle.isVisible = true
        binding.subtitle.text = group.contacts.joinToString(", ") { it.name }
        binding.subtitle.collapseEnabled = group.contacts.size > 1

        binding.numbers.isVisible = false
    }

    private fun bindPerson(holder: QkViewHolder, contact: Contact, prev: ComposeItem?) {
        val binding = ContactListItemBinding.bind(holder.containerView)

        binding.index.isVisible = true
        binding.index.text =
            if (contact.name.getOrNull(0)?.isLetter() == true) contact.name[0].toString() else "#"
        binding.index.isVisible = prev !is ComposeItem.Person ||
                (contact.name[0].isLetter() && !contact.name[0].equals(
                    prev.value.name[0],
                    ignoreCase = true
                )) ||
                (!contact.name[0].isLetter() && prev.value.name[0].isLetter())

        binding.icon.isVisible = false

        binding.avatar.recipients = listOf(createRecipient(contact))

        binding.title.text = contact.name

        binding.subtitle.isVisible = false

        binding.numbers.isVisible = true
        (binding.numbers.adapter as PhoneNumberAdapter).data = contact.numbers
    }

    private fun createRecipient(contact: Contact): Recipient {
        return recipients[contact.lookupKey] ?: Recipient(
            address = contact.numbers.firstOrNull()?.address ?: "",
            contact = contact
        )
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        disposables += conversationRepo.getUnmanagedRecipients()
            .map { recipients -> recipients.associateByNotNull { recipient -> recipient.contact?.lookupKey } }
            .subscribe { recipients -> this@ComposeItemAdapter.recipients = recipients }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        disposables.clear()
    }

    override fun areItemsTheSame(old: ComposeItem, new: ComposeItem): Boolean {
        val oldIds = old.getContacts().map { contact -> contact.lookupKey }
        val newIds = new.getContacts().map { contact -> contact.lookupKey }
        return oldIds == newIds
    }

    override fun areContentsTheSame(old: ComposeItem, new: ComposeItem): Boolean {
        return false
    }

}
