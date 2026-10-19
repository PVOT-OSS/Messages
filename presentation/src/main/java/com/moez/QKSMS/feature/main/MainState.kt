/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.main

import io.realm.RealmResults
import org.prauga.messages.model.Conversation
import org.prauga.messages.model.SearchItem
import org.prauga.messages.repository.SyncRepository

data class MainState(
    val hasError: Boolean = false,
    val page: MainPage = Inbox(),
    val drawerOpen: Boolean = false,
    val upgraded: Boolean = true,
    val showRating: Boolean = false,
    val syncing: SyncRepository.SyncProgress = SyncRepository.SyncProgress.Idle,
    val defaultSms: Boolean = true,
    val smsPermission: Boolean = true,
    val contactPermission: Boolean = true,
    val notificationPermission: Boolean = true,
    val currentFilter: ConversationFilterType = ConversationFilterType.ALL,
    val activeChip: MessageCategory = MessageCategory.ALL,
)

sealed class MainPage

data class Inbox(
    val addContact: Boolean = false,
    val markPinned: Boolean = true,
    val markRead: Boolean = false,
    val data: RealmResults<Conversation>? = null,
    val selected: Int = 0,
) : MainPage()

data class Searching(val loading: Boolean = false, val data: List<SearchItem>? = null) : MainPage()

data class Archived(
    val addContact: Boolean = false,
    val markPinned: Boolean = true,
    val markRead: Boolean = false,
    val data: RealmResults<Conversation>? = null,
    val selected: Int = 0,
) : MainPage()

enum class ConversationFilterType {
    ALL,
    UNREAD,
}

enum class MessageCategory {
    ALL,
    UNREAD,
    ARCHIVED,
}
