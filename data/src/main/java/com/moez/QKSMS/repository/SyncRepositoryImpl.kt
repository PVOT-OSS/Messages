/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.provider.Telephony
import com.f2prateek.rx.preferences2.Preference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import io.realm.Realm
import io.realm.RealmList
import io.realm.Sort
import javax.inject.Inject
import javax.inject.Singleton
import org.prauga.messages.extensions.forEach
import org.prauga.messages.extensions.insertOrUpdate
import org.prauga.messages.extensions.map
import org.prauga.messages.manager.KeyManager
import org.prauga.messages.mapper.CursorToContact
import org.prauga.messages.mapper.CursorToContactGroup
import org.prauga.messages.mapper.CursorToContactGroupMember
import org.prauga.messages.mapper.CursorToConversation
import org.prauga.messages.mapper.CursorToMessage
import org.prauga.messages.mapper.CursorToPart
import org.prauga.messages.mapper.CursorToRecipient
import org.prauga.messages.model.Contact
import org.prauga.messages.model.ContactGroup
import org.prauga.messages.model.Conversation
import org.prauga.messages.model.Message
import org.prauga.messages.model.MmsPart
import org.prauga.messages.model.PhoneNumber
import org.prauga.messages.model.Recipient
import org.prauga.messages.model.SyncLog
import org.prauga.messages.util.PhoneNumberUtils
import org.prauga.messages.util.tryOrNull
import timber.log.Timber

@Singleton
class SyncRepositoryImpl
@Inject
constructor(
    private val contentResolver: ContentResolver,
    private val conversationRepo: ConversationRepository,
    private val cursorToConversation: CursorToConversation,
    private val cursorToMessage: CursorToMessage,
    private val cursorToPart: CursorToPart,
    private val cursorToRecipient: CursorToRecipient,
    private val cursorToContact: CursorToContact,
    private val cursorToContactGroup: CursorToContactGroup,
    private val cursorToContactGroupMember: CursorToContactGroupMember,
    private val keys: KeyManager,
    private val phoneNumberUtils: PhoneNumberUtils,
    private val rxPrefs: RxSharedPreferences,
    private val reactions: EmojiReactionRepository,
) : SyncRepository {

    override val syncProgress: Subject<SyncRepository.SyncProgress> =
        BehaviorSubject.createDefault(SyncRepository.SyncProgress.Idle)

    override fun syncMessages() {

        val oldBlockedSenders = rxPrefs.getStringSet("pref_key_blocked_senders")

        // If the sync is already running, don't try to do another one
        if (syncProgress.blockingFirst() is SyncRepository.SyncProgress.Running) return
        syncProgress.onNext(SyncRepository.SyncProgress.Running(0, 0, true))

        val handlerThread = HandlerThread("RealmSyncThread")
        handlerThread.start()
        Handler(handlerThread.looper).post {
            try {
                Realm.getDefaultInstance().use { realm -> syncAll(realm, oldBlockedSenders) }
                oldBlockedSenders.delete()
            } catch (e: Exception) {
                Timber.e(e, "syncMessages Failed")
            } finally {
                handlerThread.quitSafely()
                syncProgress.onNext(SyncRepository.SyncProgress.Idle)
            }
        }
    }

    private fun syncAll(realm: Realm, oldBlockedSenders: Preference<Set<String>>) {
        realm.executeTransaction { r ->
            r.delete(SyncLog::class.java)
            r.delete(MmsPart::class.java)
            r.delete(Message::class.java)
        }

        keys.reset()

        val partsCursor = cursorToPart.getPartsCursor()
        val messageCursor = cursorToMessage.getMessagesCursor()
        val conversationCursor = cursorToConversation.getConversationsCursor()
        val recipientCursor = cursorToRecipient.getRecipientCursor()

        val max =
            (partsCursor?.count ?: 0) +
                (messageCursor?.count ?: 0) +
                (conversationCursor?.count ?: 0) +
                (recipientCursor?.count ?: 0)

        var progress = 0

        realm.inBatches { written ->
            // Sync message parts
            partsCursor?.use {
                partsCursor.forEach { cursor ->
                    tryOrNull {
                        val part = cursorToPart.map(partsCursor)
                        realm.insertOrUpdate(part)
                        progress++
                    }
                    written()
                }
            }

            // Sync messages
            messageCursor?.use {
                val messageColumns = CursorToMessage.MessageColumns(messageCursor)
                messageCursor.forEach { cursor ->
                    tryOrNull {
                        syncProgress.onNext(
                            SyncRepository.SyncProgress.Running(max, ++progress, false)
                        )
                        val message =
                            cursorToMessage.map(Pair(cursor, messageColumns)).apply {
                                if (isMms()) {
                                    parts =
                                        RealmList<MmsPart>().apply {
                                            addAll(
                                                realm
                                                    .where(MmsPart::class.java)
                                                    .equalTo("messageId", contentId)
                                                    .findAll()
                                            )
                                        }
                                }
                            }
                        realm.insertOrUpdate(message)
                    }
                    written()
                }
            }
        }

        realm.executeTransaction { r ->
            // Prepare existing conversation data
            val persistedData =
                r.copyFromRealm(
                        r.where(Conversation::class.java)
                            .beginGroup()
                            .equalTo("archived", true)
                            .or()
                            .equalTo("blocked", true)
                            .or()
                            .equalTo("pinned", true)
                            .or()
                            .isNotEmpty("name")
                            .or()
                            .isNotNull("blockingClient")
                            .or()
                            .isNotEmpty("blockReason")
                            .endGroup()
                            .findAll()
                    )
                    .associateBy { conversation -> conversation.id }
                    .toMutableMap()

            r.delete(Contact::class.java)
            r.delete(ContactGroup::class.java)
            r.delete(Conversation::class.java)
            r.delete(Recipient::class.java)

            // Migrate blocked conversations from 2.7.3
            oldBlockedSenders
                .get()
                .map { threadIdString -> threadIdString.toLong() }
                .filter { threadId -> !persistedData.contains(threadId) }
                .forEach { threadId ->
                    persistedData[threadId] = Conversation(id = threadId, blocked = true)
                }

            // Sync conversations
            conversationCursor?.use {
                conversationCursor.forEach { cursor ->
                    tryOrNull {
                        syncProgress.onNext(
                            SyncRepository.SyncProgress.Running(max, ++progress, false)
                        )
                        val conversation =
                            cursorToConversation.map(cursor).apply {
                                persistedData[id]?.let { persistedConversation ->
                                    archived = persistedConversation.archived
                                    blocked = persistedConversation.blocked
                                    pinned = persistedConversation.pinned
                                    name = persistedConversation.name
                                    blockingClient = persistedConversation.blockingClient
                                    blockReason = persistedConversation.blockReason
                                }
                                lastMessage =
                                    r.where(Message::class.java)
                                        .sort("date", Sort.DESCENDING)
                                        .equalTo("threadId", id)
                                        .findFirst()
                            }
                        r.insertOrUpdate(conversation)
                    }
                }
            }

            // Sync recipients
            val contacts = r.copyToRealmOrUpdate(getContacts())
            recipientCursor?.use {
                recipientCursor.forEach { cursor ->
                    tryOrNull {
                        syncProgress.onNext(
                            SyncRepository.SyncProgress.Running(max, ++progress, false)
                        )
                        val rec =
                            cursorToRecipient.map(cursor).apply {
                                contact =
                                    contacts.firstOrNull { c ->
                                        c.numbers.any { num ->
                                            phoneNumberUtils.compare(address, num.address)
                                        }
                                    }
                            }
                        r.insertOrUpdate(rec)
                    }
                }
            }

            syncProgress.onNext(SyncRepository.SyncProgress.Running(0, 0, true))

            // Now that we have all the messages, we can scan for emoji reactions
            reactions.deleteAndReparseAllEmojiReactions(r)

            r.insert(SyncLog())
        }
    }

    private fun Realm.inBatches(block: (written: () -> Unit) -> Unit) {
        var pending = 0
        beginTransaction()
        try {
            block {
                if (++pending >= SYNC_BATCH_SIZE) {
                    commitTransaction()
                    beginTransaction()
                    pending = 0
                }
            }
            commitTransaction()
        } catch (e: Throwable) {
            if (isInTransaction) cancelTransaction()
            throw e
        }
    }

    override fun syncMessage(uri: Uri): Message? {

        // If we don't have a valid type, return null
        val type =
            when {
                uri.toString().contains("mms") -> "mms"
                uri.toString().contains("sms") -> "sms"
                else -> return null
            }

        // If we don't have a valid id, return null
        val id = tryOrNull(false) { ContentUris.parseId(uri) } ?: return null

        // Check if the message already exists, so we can reuse the id
        val existingId =
            Realm.getDefaultInstance().use { realm ->
                realm.refresh()
                realm
                    .where(Message::class.java)
                    .equalTo("type", type)
                    .equalTo("contentId", id)
                    .findFirst()
                    ?.id
            }

        // The uri might be something like content://mms/inbox/id
        // The box might change though, so we should just use the mms/id uri
        val stableUri =
            when (type) {
                "mms" -> ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, id)
                else -> ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, id)
            }

        return contentResolver.query(stableUri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) {
                existingId?.let(::removeMessage)
                return null
            }

            val columnsMap = CursorToMessage.MessageColumns(cursor)
            cursorToMessage.map(Pair(cursor, columnsMap)).apply {
                existingId?.let { this.id = it }

                if (isMms()) {
                    parts =
                        RealmList<MmsPart>().apply {
                            addAll(
                                cursorToPart
                                    .getPartsCursor(contentId)
                                    ?.map { cursorToPart.map(it) }
                                    .orEmpty()
                            )
                        }
                }

                conversationRepo.getOrCreateConversation(threadId)
                insertOrUpdate()

                val text = getText(false)
                val parsedReaction = reactions.parseEmojiReaction(text)
                if (parsedReaction != null) {
                    Realm.getDefaultInstance().use { realm ->
                        val targetMessage =
                            reactions.findTargetMessage(
                                threadId,
                                parsedReaction.originalMessage,
                                realm,
                            )
                        realm.executeTransaction {
                            reactions.saveEmojiReaction(this, parsedReaction, targetMessage, realm)
                        }
                    }
                }
            }
        }
    }

    private fun removeMessage(id: Long) {
        val threadId =
            Realm.getDefaultInstance().use { realm ->
                val message =
                    realm.where(Message::class.java).equalTo("id", id).findFirst() ?: return
                val threadId = message.threadId

                realm.executeTransaction {
                    message.parts.deleteAllFromRealm()
                    message.deleteFromRealm()
                }

                threadId
            }

        conversationRepo.updateConversations(threadId)
    }

    override fun syncContacts() {
        // Load all the contacts
        var contacts = getContacts()

        Realm.getDefaultInstance()?.use { realm ->
            val recipients = realm.where(Recipient::class.java).findAll()

            realm.executeTransaction {
                realm.delete(Contact::class.java)
                realm.delete(ContactGroup::class.java)

                contacts = realm.copyToRealmOrUpdate(contacts)
                realm.insertOrUpdate(getContactGroups(contacts))

                // Update all the recipients with the new contacts
                recipients.forEach { recipient ->
                    recipient.contact =
                        contacts.find { contact ->
                            contact.numbers.any {
                                phoneNumberUtils.compare(recipient.address, it.address)
                            }
                        }
                }

                realm.insertOrUpdate(recipients)
            }
        }
    }

    private fun getContacts(): List<Contact> {
        val defaultNumberIds =
            Realm.getDefaultInstance().use { realm ->
                realm.where(PhoneNumber::class.java).equalTo("isDefault", true).findAll().map {
                    number ->
                    number.id
                }
            }

        return cursorToContact
            .getContactsCursor()
            ?.map { cursor -> cursorToContact.map(cursor) }
            ?.groupBy { contact -> contact.lookupKey }
            ?.map { contacts ->
                // Sometimes, contacts providers on the phone will create duplicate phone number
                // entries. This
                // commonly happens with Whatsapp. Let's try to detect these duplicate entries and
                // filter them out
                val uniqueNumbers = mutableListOf<PhoneNumber>()
                contacts.value
                    .flatMap { it.numbers }
                    .forEach { number ->
                        number.isDefault = defaultNumberIds.any { id -> id == number.id }
                        val duplicate =
                            uniqueNumbers.find { other ->
                                phoneNumberUtils.compare(number.address, other.address)
                            }

                        if (duplicate == null) {
                            uniqueNumbers += number
                        } else if (!duplicate.isDefault && number.isDefault) {
                            duplicate.isDefault = true
                        }
                    }

                contacts.value.first().apply {
                    numbers.clear()
                    numbers.addAll(uniqueNumbers)
                }
            } ?: listOf()
    }

    private fun getContactGroups(contacts: List<Contact>): List<ContactGroup> {
        val groupMembers =
            cursorToContactGroupMember
                .getGroupMembersCursor()
                ?.map(cursorToContactGroupMember::map)
                .orEmpty()

        val groups =
            cursorToContactGroup.getContactGroupsCursor()?.map(cursorToContactGroup::map).orEmpty()

        groups.forEach { group ->
            group.contacts.addAll(
                groupMembers
                    .filter { member -> member.groupId == group.id }
                    .mapNotNull { member ->
                        contacts.find { contact -> contact.lookupKey == member.lookupKey }
                    }
            )
        }

        return groups
    }

    companion object {
        const val SYNC_BATCH_SIZE = 2_000
    }
}
