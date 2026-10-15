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
