/*
 * SPDX-FileCopyrightText: 2020 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.conversationinfo

import io.realm.RealmList
import org.prauga.messages.model.MmsPart
import org.prauga.messages.model.Recipient

sealed class ConversationInfoItem {

    data class ConversationInfoRecipient(val value: Recipient) : ConversationInfoItem()

    data class ConversationInfoSettings(
        val name: String,
        val recipients: RealmList<Recipient>,
        val archived: Boolean,
        val blocked: Boolean,
    ) : ConversationInfoItem()

    data class ConversationInfoMedia(val value: MmsPart) : ConversationInfoItem()
}

/** The recipients and settings, followed by [media] read one item at a time */
class ConversationInfoItems(
    private val header: List<ConversationInfoItem>,
    private val media: List<MmsPart>,
) : AbstractList<ConversationInfoItem>() {

    override val size: Int
        get() = header.size + media.size

    override fun get(index: Int): ConversationInfoItem =
        header.getOrNull(index)
            ?: ConversationInfoItem.ConversationInfoMedia(media[index - header.size])

    override fun equals(other: Any?) = this === other

    override fun hashCode() = System.identityHashCode(this)
}
