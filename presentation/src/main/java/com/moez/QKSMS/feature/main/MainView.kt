/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
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
package org.prauga.messages.feature.main

import android.content.Intent
import io.reactivex.Observable
import org.prauga.messages.common.base.QkView
import org.prauga.messages.manager.ChangelogManager

interface MainView : QkView<MainState> {

    val onNewIntentIntent: Observable<Intent>
    val activityResumedIntent: Observable<Boolean>
    val queryChangedIntent: Observable<CharSequence>
    val composeIntent: Observable<Unit>
    val drawerToggledIntent: Observable<Boolean>
    val homeIntent: Observable<*>
    val navigationIntent: Observable<NavItem>
    val optionsItemIntent: Observable<Int>
    val filterSelectedIntent: Observable<MessageCategory>

    //    val plusBannerIntent: Observable<*>
    val dismissRatingIntent: Observable<*>
    val rateIntent: Observable<*>
    val conversationsSelectedIntent: Observable<List<Long>>
    val confirmDeleteIntent: Observable<List<Long>>
    val renameConversationIntent: Observable<String>
    val swipeConversationIntent: Observable<Pair<Long, Int>>
    val changelogMoreIntent: Observable<*>
    val undoArchiveIntent: Observable<Unit>
    val snackbarButtonIntent: Observable<Unit>

    fun requestDefaultSms()
    fun requestPermissions()
    fun restartActivity()
    fun clearSearch()
    fun clearSelection()
    fun toggleSelectAll()
    fun themeChanged()
    fun showBlockingDialog(conversations: List<Long>, block: Boolean)
    fun showDeleteDialog(conversations: List<Long>)
    fun showRenameDialog(conversationName: String)
    fun showChangelog(changelog: ChangelogManager.CumulativeChangelog)
    fun showArchivedSnackbar(countConversationsArchived: Int, isArchiving: Boolean)
    fun drawerToggled(opened: Boolean)
}

enum class NavItem { BACK, INBOX, ARCHIVED, BACKUP, SCHEDULED, BLOCKING, SETTINGS, PLUS, HELP, INVITE }
