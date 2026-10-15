/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
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

enum class NavItem {
    BACK,
    INBOX,
    ARCHIVED,
    BACKUP,
    SCHEDULED,
    BLOCKING,
    SETTINGS,
    PLUS,
    HELP,
    INVITE,
}
