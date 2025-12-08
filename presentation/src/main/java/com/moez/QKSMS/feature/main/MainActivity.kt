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

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewStub
import android.widget.ProgressBar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import dagger.android.AndroidInjection
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.prauga.messages.R
import org.prauga.messages.common.Navigator
import org.prauga.messages.common.androidxcompat.drawerOpen
import org.prauga.messages.common.base.QkThemedActivity
import org.prauga.messages.common.util.extensions.autoScrollToStart
import org.prauga.messages.common.util.extensions.dismissKeyboard
import org.prauga.messages.common.util.extensions.resolveThemeColor
import org.prauga.messages.common.util.extensions.scrapViews
import org.prauga.messages.common.util.extensions.setBackgroundTint
import org.prauga.messages.common.util.extensions.setTint
import org.prauga.messages.common.util.extensions.setVisible
import org.prauga.messages.common.widget.TextInputDialog
import org.prauga.messages.databinding.MainActivityBinding
import org.prauga.messages.feature.blocking.BlockingDialog
import org.prauga.messages.feature.changelog.ChangelogDialog
import org.prauga.messages.feature.conversations.ConversationItemTouchCallback
import org.prauga.messages.feature.conversations.ConversationsAdapter
import org.prauga.messages.manager.ChangelogManager
import org.prauga.messages.repository.SyncRepository
import javax.inject.Inject
import androidx.core.view.get

class MainActivity : QkThemedActivity<MainActivityBinding>(MainActivityBinding::inflate), MainView {

    @Inject
    lateinit var blockingDialog: BlockingDialog

    @Inject
    lateinit var disposables: CompositeDisposable

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var conversationsAdapter: ConversationsAdapter

    @Inject
    lateinit var drawerBadgesExperiment: DrawerBadgesExperiment

    @Inject
    lateinit var searchAdapter: SearchAdapter

    @Inject
    lateinit var itemTouchCallback: ConversationItemTouchCallback

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override val onNewIntentIntent: Subject<Intent> = PublishSubject.create()
    override val activityResumedIntent: Subject<Boolean> = PublishSubject.create()
    override val queryChangedIntent by lazy { binding.toolbarSearch.textChanges() }
    override val composeIntent by lazy { binding.compose.clicks() }
    override val drawerToggledIntent: Observable<Boolean> by lazy {
        binding.drawerLayout.drawerOpen(Gravity.START)
    }
    override val homeIntent: Subject<Unit> = PublishSubject.create()

    private val menuClickSubject: Subject<Unit> = PublishSubject.create()
    override val navigationIntent: Observable<NavItem> by lazy {
        Observable.merge(
            listOf(
                backPressedSubject,
                binding.drawer.inbox.clicks().map { NavItem.INBOX },
                binding.drawer.archived.clicks().map { NavItem.ARCHIVED },
                binding.drawer.backup.clicks().map { NavItem.BACKUP },
                binding.drawer.scheduled.clicks().map { NavItem.SCHEDULED },
                binding.drawer.blocking.clicks().map { NavItem.BLOCKING },
                binding.drawer.settings.clicks().map { NavItem.SETTINGS },
//                plus.clicks().map { NavItem.PLUS },
//                help.clicks().map { NavItem.HELP },
                binding.drawer.invite.clicks().map { NavItem.INVITE })
        )
    }
    override val optionsItemIntent: Subject<Int> = PublishSubject.create()

    //    override val plusBannerIntent by lazy { plusBanner.clicks() }
    override val dismissRatingIntent by lazy { binding.drawer.rateDismiss.clicks() }
    override val rateIntent by lazy { binding.drawer.rateOkay.clicks() }
    override val conversationsSelectedIntent by lazy { conversationsAdapter.selectionChanges }
    override val confirmDeleteIntent: Subject<List<Long>> = PublishSubject.create()
    override val renameConversationIntent: Subject<String> = PublishSubject.create()
    override val swipeConversationIntent by lazy { itemTouchCallback.swipes }
    override val changelogMoreIntent by lazy { changelogDialog.moreClicks }
    override val undoArchiveIntent: Subject<Unit> = PublishSubject.create()
    override val snackbarButtonIntent: Subject<Unit> = PublishSubject.create()

    private val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[MainViewModel::class.java]
    }
    private val toggle by lazy {
        ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.main_drawer_open_cd,
            0
        )
    }
    private val itemTouchHelper by lazy { ItemTouchHelper(itemTouchCallback) }
    private val changelogDialog by lazy { ChangelogDialog(this) }
    private val backPressedSubject: Subject<NavItem> = PublishSubject.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        viewModel.bindView(this)
        onNewIntentIntent.onNext(intent)

        (binding.snackbar as? ViewStub)?.setOnInflateListener { _, inflated ->
            inflated.findViewById<View>(R.id.snackbarButton).clicks()
                .autoDisposable(scope(Lifecycle.Event.ON_DESTROY))
                .subscribe(snackbarButtonIntent)
        }

        (binding.syncing as? ViewStub)?.setOnInflateListener { _, inflated ->
            inflated.findViewById<ProgressBar>(R.id.syncingProgress)?.let {
                it.progressTintList = ColorStateList.valueOf(theme.blockingFirst().theme)
                it.indeterminateTintList = ColorStateList.valueOf(theme.blockingFirst().theme)
            }
        }

        binding.changeDefaultSmsButton.setOnClickListener {
            requestDefaultSms()
        }

        toggle.syncState()
        title = ""
        binding.toolbar.setNavigationOnClickListener {
            dismissKeyboard()
            homeIntent.onNext(Unit)
        }

        binding.cVTopBar1.clicks()
            .autoDisposable(scope())
            .subscribe {
                showEditMenu()
            }

        binding.cVTopBar3.clicks()
            .autoDisposable(scope())
            .subscribe {
                showDrawerMenu()
            }

        itemTouchCallback.adapter = conversationsAdapter
        conversationsAdapter.autoScrollToStart(binding.recyclerView)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 0 && binding.cVTopBar2.translationY == 0f) {
                    // Hide
                    val translationY =
                        -binding.cVTopBar2.height.toFloat() - 8f * resources.displayMetrics.density
                    binding.cVTopBar1.animate().translationY(translationY).setDuration(200).start()
                    binding.cVTopBar2.animate().translationY(translationY).setDuration(200).start()
                    binding.cVTopBar3.animate().translationY(translationY).setDuration(200).start()
                } else if (dy < 0 && binding.cVTopBar2.translationY != 0f) {
                    // Show
                    binding.cVTopBar1.animate().translationY(0f).setDuration(200).start()
                    binding.cVTopBar2.animate().translationY(0f).setDuration(200).start()
                    binding.cVTopBar3.animate().translationY(0f).setDuration(200).start()
                }
            }
        })

        // Don't allow clicks to pass through the drawer layout
        binding.drawer.root.clicks().autoDisposable(scope()).subscribe()

        // Set the theme color tint to the recyclerView, progressbar, and FAB
        theme
            .autoDisposable(scope())
            .subscribe { theme ->
                // Set the color for the drawer icons
                val states = arrayOf(
                    intArrayOf(android.R.attr.state_activated),
                    intArrayOf(-android.R.attr.state_activated)
                )

                ColorStateList(
                    states, intArrayOf(
                        theme.theme,
                        resolveThemeColor(android.R.attr.textColorSecondary)
                    )
                )
                    .let { tintList ->
                        binding.drawer.inboxIcon.imageTintList = tintList
                        binding.drawer.archivedIcon.imageTintList = tintList
                    }

                // Miscellaneous views
                listOf(binding.drawer.plusBadge1, binding.drawer.plusBadge2).forEach { badge ->
                    badge.setBackgroundTint(theme.theme)
                    badge.setTextColor(theme.textPrimary)
                }
                (binding.syncing as? ViewStub)?.findViewById<ProgressBar>(R.id.syncingProgress)
                    ?.let {
                        it.progressTintList = ColorStateList.valueOf(theme.theme)
                        it.indeterminateTintList = ColorStateList.valueOf(theme.theme)
                    }
                binding.drawer.plusIcon.setTint(theme.theme)
                binding.drawer.rateIcon.setTint(theme.theme)

                val primaryTextColor = resolveThemeColor(android.R.attr.textColorPrimary)
                val secondaryTextColor = resolveThemeColor(android.R.attr.textColorSecondary)
                binding.searchIcon.setTint(primaryTextColor)
                binding.toolbarSearch.setTextColor(primaryTextColor)
                binding.toolbarSearch.setHintTextColor(secondaryTextColor)

                binding.editText.setTextColor(primaryTextColor)
                binding.messagesText.setTextColor(primaryTextColor)
                binding.menuIcon.setTint(primaryTextColor)

                binding.compose.setTint(primaryTextColor)
            }
    }

    override fun onNewIntent(intent: Intent?) =
        intent?.let {
            super.onNewIntent(intent)
            it.run(onNewIntentIntent::onNext)
        } ?: Unit

    override fun render(state: MainState) {
        if (state.hasError) {
            finish()
            return
        }

        if (!state.defaultSms) {
            binding.notDefaultSmsView.setVisible(true)
            binding.recyclerView.setVisible(false)
            binding.empty.setVisible(false)
            binding.searchPill.setVisible(false)
            binding.compose.setVisible(false)
            return
        } else {
            binding.notDefaultSmsView.setVisible(false)
        }

        val addContact = when (state.page) {
            is Inbox -> state.page.addContact
            is Archived -> state.page.addContact
            else -> false
        }

        val markPinned = when (state.page) {
            is Inbox -> state.page.markPinned
            is Archived -> state.page.markPinned
            else -> true
        }

        val markRead = when (state.page) {
            is Inbox -> state.page.markRead
            is Archived -> state.page.markRead
            else -> true
        }

        val selectedConversations = when (state.page) {
            is Inbox -> state.page.selected
            is Archived -> state.page.selected
            else -> 0
        }

        val hasSelection = selectedConversations > 0
        binding.toolbar.setVisible(hasSelection)

        binding.cVTopBar1.setVisible(!hasSelection)
        binding.cVTopBar2.setVisible(!hasSelection)
        binding.cVTopBar3.setVisible(!hasSelection)

        binding.toolbarSearch.setVisible(
            state.page is Inbox &&
                    state.page.selected == 0 ||
                    state.page is Searching
        )
        binding.toolbarTitle.setVisible(true)

        binding.toolbar.menu.apply {
            findItem(R.id.select_all)?.isVisible =
                (conversationsAdapter.itemCount > 1) && selectedConversations != 0
            findItem(R.id.archive)?.isVisible =
                state.page is Inbox && selectedConversations != 0
            findItem(R.id.unarchive)?.isVisible =
                state.page is Archived && selectedConversations != 0
            findItem(R.id.delete)?.isVisible = selectedConversations != 0
            findItem(R.id.add)?.isVisible = addContact && selectedConversations != 0
            findItem(R.id.pin)?.isVisible = markPinned && selectedConversations != 0
            findItem(R.id.unpin)?.isVisible = !markPinned && selectedConversations != 0
            findItem(R.id.read)?.isVisible = (markRead && selectedConversations != 0) ||
                    selectedConversations > 1
            findItem(R.id.unread)?.isVisible = (!markRead && selectedConversations != 0) ||
                    selectedConversations > 1
            findItem(R.id.block)?.isVisible = selectedConversations != 0
            findItem(R.id.rename)?.isVisible = selectedConversations == 1
        }

        listOf(binding.drawer.plusBadge1, binding.drawer.plusBadge2).forEach { badge ->
            badge.isVisible = drawerBadgesExperiment.variant && !state.upgraded
        }
//        plus.isVisible = state.upgraded
        binding.drawer.plusBanner.isVisible = !state.upgraded
        binding.drawer.rateLayout.setVisible(state.showRating)

        binding.compose.setVisible(state.page is Inbox || state.page is Archived)
        conversationsAdapter.emptyView = binding.empty.takeIf {
            state.page is Inbox || state.page is Archived
        }
        searchAdapter.emptyView = binding.empty.takeIf { state.page is Searching }

        when (state.page) {
            is Inbox -> {
                showBackButton(state.page.selected > 0)
                binding.toolbarTitle.text = when {
                    state.page.selected > 0 -> getString(
                        R.string.main_title_selected,
                        state.page.selected
                    )

                    else -> getString(R.string.app_name)
                }
                if (binding.recyclerView.adapter !== conversationsAdapter)
                    binding.recyclerView.adapter = conversationsAdapter
                conversationsAdapter.updateData(state.page.data)
                itemTouchHelper.attachToRecyclerView(binding.recyclerView)
                binding.empty.setText(R.string.inbox_empty_text)
            }

            is Searching -> {
                showBackButton(true)
                binding.toolbarTitle.text = getString(R.string.title_conversations)
                if (binding.recyclerView.adapter !== searchAdapter) binding.recyclerView.adapter =
                    searchAdapter
                searchAdapter.data = state.page.data ?: listOf()
                itemTouchHelper.attachToRecyclerView(null)
                binding.empty.setText(R.string.inbox_search_empty_text)
            }

            is Archived -> {
                showBackButton(state.page.selected > 0)
                binding.toolbarTitle.text = when {
                    state.page.selected > 0 -> getString(
                        R.string.main_title_selected,
                        state.page.selected
                    )

                    else -> getString(R.string.title_archived)
                }
                if (binding.recyclerView.adapter !== conversationsAdapter)
                    binding.recyclerView.adapter = conversationsAdapter
                conversationsAdapter.updateData(state.page.data)
                itemTouchHelper.attachToRecyclerView(null)
                binding.empty.setText(R.string.archived_empty_text)
            }

            else -> {
                binding.toolbarTitle.text = getString(R.string.app_name)
            }
        }

        binding.drawer.inbox.isActivated = state.page is Inbox
        binding.drawer.archived.isActivated = state.page is Archived

        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START) && !state.drawerOpen)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        else if (!binding.drawerLayout.isDrawerVisible(GravityCompat.START) && state.drawerOpen)
            binding.drawerLayout.openDrawer(GravityCompat.START)

        when (state.syncing) {
            is SyncRepository.SyncProgress.Idle -> {
                binding.syncing.isVisible = false
                binding.snackbar.isVisible = (!state.defaultSms ||
                        !state.smsPermission ||
                        !state.contactPermission ||
                        !state.notificationPermission)
            }

            is SyncRepository.SyncProgress.Running -> {
                binding.syncing.isVisible = true
                findViewById<ProgressBar>(R.id.syncingProgress)?.let { progress ->
                    progress.max = state.syncing.max
                    ObjectAnimator.ofInt(
                        progress,
                        "progress",
                        progress.progress,
                        state.syncing.progress
                    ).start()
                    progress.isIndeterminate = state.syncing.indeterminate
                }
                binding.snackbar.isVisible = false
            }
        }

        when {
            !state.defaultSms -> {
                (binding.snackbar as? ViewStub)?.findViewById<View>(R.id.snackbarTitle)?.let {
                    (it as? org.prauga.messages.common.widget.QkTextView)?.setText(R.string.main_default_sms_title)
                }
                (binding.snackbar as? ViewStub)?.findViewById<View>(R.id.snackbarMessage)?.let {
                    (it as? org.prauga.messages.common.widget.QkTextView)?.setText(R.string.main_default_sms_message)
                }
                (binding.snackbar as? ViewStub)?.findViewById<View>(R.id.snackbarButton)?.let {
                    (it as? org.prauga.messages.common.widget.QkTextView)?.setText(R.string.main_default_sms_change)
                }
            }

            !state.smsPermission -> {
                (binding.snackbar as? ViewStub)?.findViewById<View>(R.id.snackbarTitle)?.let {
                    (it as? org.prauga.messages.common.widget.QkTextView)?.setText(R.string.main_permission_required)
                }
                (binding.snackbar as? ViewStub)?.findViewById<View>(R.id.snackbarMessage)?.let {
                    (it as? org.prauga.messages.common.widget.QkTextView)?.setText(R.string.main_permission_sms)
                }
                (binding.snackbar as? ViewStub)?.findViewById<View>(R.id.snackbarButton)?.let {
                    (it as? org.prauga.messages.common.widget.QkTextView)?.setText(R.string.main_permission_allow)
                }
            }

            !state.contactPermission -> {
                (binding.snackbar as? ViewStub)?.findViewById<View>(R.id.snackbarTitle)?.let {
                    (it as? org.prauga.messages.common.widget.QkTextView)?.setText(R.string.main_permission_required)
                }
                (binding.snackbar as? ViewStub)?.findViewById<View>(R.id.snackbarMessage)?.let {
                    (it as? org.prauga.messages.common.widget.QkTextView)?.setText(R.string.main_permission_contacts)
                }
                (binding.snackbar as? ViewStub)?.findViewById<View>(R.id.snackbarButton)?.let {
                    (it as? org.prauga.messages.common.widget.QkTextView)?.setText(R.string.main_permission_allow)
                }
            }

            !state.notificationPermission -> {
                (binding.snackbar as? ViewStub)?.findViewById<View>(R.id.snackbarTitle)?.let {
                    (it as? org.prauga.messages.common.widget.QkTextView)?.setText(R.string.main_permission_required)
                }
                (binding.snackbar as? ViewStub)?.findViewById<View>(R.id.snackbarMessage)?.let {
                    (it as? org.prauga.messages.common.widget.QkTextView)?.setText(R.string.main_permission_notifications)
                }
                (binding.snackbar as? ViewStub)?.findViewById<View>(R.id.snackbarButton)?.let {
                    (it as? org.prauga.messages.common.widget.QkTextView)?.setText(R.string.main_permission_allow)
                }
            }
        }
    }

    override fun onResume() =
        super.onResume().also { activityResumedIntent.onNext(true) }

    override fun onPause() =
        super.onPause().also { activityResumedIntent.onNext(false) }

    override fun onDestroy() =
        super.onDestroy().also { disposables.dispose() }

    override fun showBackButton(show: Boolean) =
        toggle.let {
            it.onDrawerSlide(binding.drawer.root, if (show) 1f else 0f)
            it.drawerArrowDrawable.color = when (show) {
                true -> resolveThemeColor(android.R.attr.textColorSecondary)
                false -> resolveThemeColor(android.R.attr.textColorPrimary)
            }
        }

    override fun requestDefaultSms() =
        navigator.showDefaultSmsDialog(this)

    override fun restartActivity() {
        finish()
        startActivity(intent)
    }

    override fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            permissions += Manifest.permission.POST_NOTIFICATIONS

        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 0)
    }

    override fun clearSearch() {
        dismissKeyboard()
        binding.toolbarSearch.text = null
    }

    override fun clearSelection() = conversationsAdapter.clearSelection()

    override fun toggleSelectAll() = conversationsAdapter.toggleSelectAll()

    override fun themeChanged() = binding.recyclerView.scrapViews()

    override fun showBlockingDialog(conversations: List<Long>, block: Boolean) {
        blockingDialog.show(this, conversations, block)
    }

    override fun showDeleteDialog(conversations: List<Long>) {
        val dialog = AlertDialog.Builder(this, R.style.AppThemeDialog)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(
                resources.getQuantityString(
                    R.plurals.dialog_delete_message,
                    conversations.size,
                    conversations.size
                )
            )
            .setPositiveButton(R.string.button_delete) { _, _ ->
                confirmDeleteIntent.onNext(
                    conversations
                )
            }
            .setNegativeButton(R.string.button_cancel, null)
            .create()

        dialog.show()

        theme.take(1)
            .autoDisposable(scope())
            .subscribe { theme ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(theme.theme)
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(theme.theme)
            }
    }

    override fun showRenameDialog(conversationName: String) =
        TextInputDialog(
            this,
            getString(R.string.info_name),
            renameConversationIntent::onNext
        )
            .setText(conversationName)
            .show()

    override fun showChangelog(changelog: ChangelogManager.CumulativeChangelog) =
        changelogDialog.show(changelog)

    override fun showArchivedSnackbar(countConversationsArchived: Int, isArchiving: Boolean) =
        Snackbar.make(
            binding.drawerLayout,
            if (isArchiving) {
                resources.getQuantityString(
                    R.plurals.toast_archived,
                    countConversationsArchived,
                    countConversationsArchived
                )
            } else {
                resources.getQuantityString(
                    R.plurals.toast_unarchived,
                    countConversationsArchived,
                    countConversationsArchived
                )
            },
            if (countConversationsArchived < 10) Snackbar.LENGTH_LONG
            else Snackbar.LENGTH_INDEFINITE
        ).let {
            it.setAction(R.string.button_undo) { undoArchiveIntent.onNext(Unit) }
            it.setActionTextColor(colors.theme().theme)
            it.show()
        }

    override fun onCreateOptionsMenu(menu: Menu?) =
        menu?.let {
            menuInflater.inflate(R.menu.main, it)
            super.onCreateOptionsMenu(it)
        } ?: false

    override fun onOptionsItemSelected(item: MenuItem) =
        optionsItemIntent.onNext(item.itemId).let { true }

    override fun onBackPressed() = backPressedSubject.onNext(NavItem.BACK)

    override fun drawerToggled(opened: Boolean) {
        if (opened) {
            dismissKeyboard()
            if (!binding.drawer.inbox.isInTouchMode)
                binding.drawer.inbox.requestFocus()
        } else
            binding.toolbarSearch.requestFocus()
    }

    private fun showEditMenu() {
        val popup = PopupMenu(this, binding.cVTopBar1, Gravity.START, 0, R.style.DrawerPopupMenu)
        popup.menuInflater.inflate(R.menu.edit_menu, popup.menu)

        try {
            val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popup)
            mPopup.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(mPopup, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val iconColor = resolveThemeColor(android.R.attr.textColorPrimary)
        for (i in 0 until popup.menu.size) {
            val menuItem = popup.menu[i]
            menuItem.icon?.setTint(iconColor)
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_select_messages -> {
                     conversationsAdapter.startSelectionMode()
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun showDrawerMenu() {
        val popup = PopupMenu(this, binding.cVTopBar3, Gravity.END, 0, R.style.DrawerPopupMenu)
        popup.menuInflater.inflate(R.menu.drawer_menu, popup.menu)

        try {
            val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popup)
            mPopup.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(mPopup, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val iconColor = resolveThemeColor(android.R.attr.textColorPrimary)
        for (i in 0 until popup.menu.size) {
            val menuItem = popup.menu[i]
            menuItem.icon?.setTint(iconColor)
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_inbox -> {
                    backPressedSubject.onNext(NavItem.INBOX)
                    true
                }

                R.id.menu_archived -> {
                    backPressedSubject.onNext(NavItem.ARCHIVED)
                    true
                }

                R.id.menu_backup -> {
                    backPressedSubject.onNext(NavItem.BACKUP)
                    true
                }

                R.id.menu_scheduled -> {
                    backPressedSubject.onNext(NavItem.SCHEDULED)
                    true
                }

                R.id.menu_blocking -> {
                    backPressedSubject.onNext(NavItem.BLOCKING)
                    true
                }

                R.id.menu_settings -> {
                    backPressedSubject.onNext(NavItem.SETTINGS)
                    true
                }

//                R.id.menu_invite -> {
//                    backPressedSubject.onNext(NavItem.INVITE)
//                    true
//                }

                else -> false
            }
        }

        popup.show()
    }
}
