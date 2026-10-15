/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import io.reactivex.Completable
import io.reactivex.Observable
import io.realm.RealmResults
import org.prauga.messages.model.Conversation
import org.prauga.messages.model.Recipient
import org.prauga.messages.model.SearchResult

interface ConversationRepository {

    fun getConversations(
        unreadAtTop: Boolean,
        archived: Boolean = false,
        onlyUnread: Boolean = false,
    ): RealmResults<Conversation>

    fun getConversationsSnapshot(unreadAtTop: Boolean): List<Conversation>

    /** Returns the top conversations that were active in the last week */
    fun getTopConversations(): List<Conversation>

    fun setConversationName(id: Long, name: String): Completable

    fun searchConversations(query: CharSequence): List<SearchResult>

    fun searchConversationsGrouped(query: CharSequence): List<org.prauga.messages.model.SearchItem>

    fun getBlockedConversations(): RealmResults<Conversation>

    fun getBlockedConversationsAsync(): RealmResults<Conversation>

    fun getConversationAsync(threadId: Long): Conversation

    fun getConversation(threadId: Long): Conversation?

    fun getUnseenIds(archived: Boolean = false): List<Long>

    fun getUnreadIds(archived: Boolean = false): List<Long>

    fun getConversationAndLastSenderContactName(threadId: Long): Pair<Conversation?, String?>?

    /** Returns all conversations with an id in [threadIds] */
    fun getConversations(vararg threadIds: Long): RealmResults<Conversation>

    fun getUnmanagedConversations(): Observable<List<Conversation>>

    fun getRecipients(): RealmResults<Recipient>

    fun getUnmanagedRecipients(): Observable<List<Recipient>>

    fun getRecipient(recipientId: Long): Recipient?

    fun getConversation(recipient: String): Conversation?

    fun getConversation(recipients: Collection<String>): Conversation?

    fun getOrCreateConversation(threadId: Long): Conversation?

    fun getOrCreateConversation(address: String): Conversation?

    fun getOrCreateConversation(addresses: Collection<String>): Conversation?

    fun saveDraft(threadId: Long, draft: String)

    /** Updates message-related fields in the conversation, like the date and snippet */
    fun updateConversations(vararg threadIds: Long)

    fun markArchived(vararg threadIds: Long)

    fun markUnarchived(vararg threadIds: Long)

    fun markPinned(vararg threadIds: Long)

    fun markUnpinned(vararg threadIds: Long)

    fun markBlocked(threadIds: Collection<Long>, blockingClient: Int, blockReason: String?)

    fun markUnblocked(vararg threadIds: Long)

    fun deleteConversations(vararg threadIds: Long)
}
