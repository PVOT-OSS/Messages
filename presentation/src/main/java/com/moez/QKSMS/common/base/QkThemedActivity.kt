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

package org.prauga.messages.common.base

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.iterator
import androidx.lifecycle.Lifecycle
import androidx.viewbinding.ViewBinding
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.prauga.messages.R
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.extensions.resolveThemeBoolean
import org.prauga.messages.common.util.extensions.resolveThemeColor
import org.prauga.messages.extensions.Optional
import org.prauga.messages.extensions.asObservable
import org.prauga.messages.extensions.mapNotNull
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageRepository
import org.prauga.messages.util.PhoneNumberUtils
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Base activity that automatically applies any necessary theme theme settings and colors
 *
 * In most cases, this should be used instead of the base QkActivity, except for when
 * an activity does not depend on the theme
 */
abstract class QkThemedActivity<Vb : ViewBinding>(
    inflate: (LayoutInflater) -> Vb
) : QkActivity<Vb>(inflate) {

    @Inject
    lateinit var colors: Colors

    @Inject
    lateinit var conversationRepo: ConversationRepository

    @Inject
    lateinit var messageRepo: MessageRepository

    @Inject
    lateinit var phoneNumberUtils: PhoneNumberUtils

    val toolbarView: Toolbar? get() = findViewById(R.id.toolbar)

    /**
     * In case the activity should be themed for a specific conversation, the selected conversation
     * can be changed by pushing the threadId to this subject
     */
    val threadId: Subject<Long> = BehaviorSubject.createDefault(0)

    /**
     * Switch the theme if the threadId changes
     * Set it based on the latest message in the conversation
     */
    val theme: Observable<Colors.Theme> = threadId
        .distinctUntilChanged()
        .switchMap { threadId ->
            val conversation = conversationRepo.getConversation(threadId)
            when {
                conversation == null -> Observable.just(Optional(null))

                conversation.recipients.size == 1 -> Observable.just(Optional(conversation.recipients.first()))

                else -> messageRepo.getLastIncomingMessage(conversation.id)
                    .asObservable()
                    .mapNotNull { messages -> messages.firstOrNull() }
                    .distinctUntilChanged { message -> message.address }
                    .mapNotNull { message ->
                        conversation.recipients.find { recipient ->
                            phoneNumberUtils.compare(recipient.address, message.address)
                        }
                    }
                    .map { recipient -> Optional(recipient) }
                    .startWith(Optional(conversation.recipients.firstOrNull()))
                    .distinctUntilChanged()
            }
        }
        .switchMap { colors.themeObservable(it.value) }

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getActivityThemeRes())
        super.onCreate(savedInstanceState)

        // When certain preferences change, we need to recreate the activity
        val triggers =
            listOf(prefs.nightMode, prefs.night, prefs.textSize, prefs.systemFont)
        Observable.merge(triggers.map { it.asObservable().skip(1) })
            .debounce(400, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(scope())
            .subscribe { recreate() }

        // We can only set light nav bar on API 27 in attrs, but we can do it in API 26 here
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
            val night = !resolveThemeBoolean(R.attr.isLightTheme)
            window.decorView.systemUiVisibility = if (night) 0 else
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        // Some devices don't let you modify android.R.attr.navigationBarColor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = resolveThemeColor(android.R.attr.windowBackground)
        }

        // Set the color for the recent apps title
        val toolbarColor = resolveThemeColor(R.attr.colorPrimary)
        val icon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        val taskDesc =
            ActivityManager.TaskDescription(getString(R.string.app_name), icon, toolbarColor)
        setTaskDescription(taskDesc)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Set the color for the overflow and navigation icon
        val textSecondary = resolveThemeColor(android.R.attr.textColorSecondary)
        toolbarView?.overflowIcon = toolbarView?.overflowIcon?.apply { setTint(textSecondary) }

        // Update the colours of the menu items
        Observables.combineLatest(menu, theme) { menu, theme ->
            menu.iterator().forEach { menuItem ->
                val tint = when (menuItem.itemId) {
                    in getColoredMenuItems() -> theme.theme
                    else -> textSecondary
                }

                menuItem.icon = menuItem.icon?.apply { setTint(tint) }
            }
        }.autoDispose(scope(Lifecycle.Event.ON_DESTROY)).subscribe()
    }

    open fun getColoredMenuItems(): List<Int> {
        return listOf()
    }

    /**
     * This can be overridden in case an activity does not want to use the default themes
     */
    open fun getActivityThemeRes(): Int = R.style.AppTheme
}
