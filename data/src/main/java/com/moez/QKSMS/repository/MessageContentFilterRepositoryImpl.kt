/*
 * SPDX-FileCopyrightText: 2025 peanut250 <209579534+peanut250@users.noreply.github.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import io.realm.Realm
import io.realm.RealmResults
import javax.inject.Inject
import org.prauga.messages.model.MessageContentFilter
import org.prauga.messages.model.MessageContentFilterData

class MessageContentFilterRepositoryImpl @Inject constructor() : MessageContentFilterRepository {
    override fun createFilter(data: MessageContentFilterData) {
        Realm.getDefaultInstance().use { realm ->
            realm.refresh()
            val maxId = realm.where(MessageContentFilter::class.java).max("id")?.toLong() ?: -1

            realm.executeTransaction {
                realm.insert(
                    MessageContentFilter(
                        maxId + 1,
                        data.value,
                        data.caseSensitive,
                        data.isRegex,
                        data.includeContacts,
                    )
                )
            }
        }
    }

    override fun getMessageContentFilters(): RealmResults<MessageContentFilter> {
        return Realm.getDefaultInstance().where(MessageContentFilter::class.java).findAllAsync()
    }

    override fun getMessageContentFilter(id: Long): MessageContentFilter? {
        return Realm.getDefaultInstance()
            .where(MessageContentFilter::class.java)
            .equalTo("id", id)
            .findFirst()
    }

    override fun isBlocked(
        messageBody: String,
        address: String,
        contactsRepo: ContactRepository,
    ): Boolean {
        val isContact = contactsRepo.isContact(address)

        return Realm.getDefaultInstance().use { realm ->
            realm.where(MessageContentFilter::class.java).findAll().any { filter ->
                if (isContact && !filter.includeContacts) {
                    false
                } else if (filter.isRegex) {
                    Regex(filter.value).matches(messageBody)
                } else if (filter.caseSensitive) {
                    val regexp = "[\\s\\S]*\\b" + Regex.escape(filter.value) + "\\b[\\s\\S]*"
                    Regex(regexp).matches(messageBody)
                } else {
                    val regexp =
                        "[\\s\\S]*\\b" + Regex.escape(filter.value.lowercase()) + "\\b[\\s\\S]*"
                    Regex(regexp).matches(messageBody.lowercase())
                }
            }
        }
    }

    override fun removeFilter(id: Long) {
        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                realm
                    .where(MessageContentFilter::class.java)
                    .equalTo("id", id)
                    .findAll()
                    .deleteAllFromRealm()
            }
        }
    }
}
