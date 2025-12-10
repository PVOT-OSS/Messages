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
package org.prauga.messages.common.widget

import android.R
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.viewpager.widget.ViewPager
import com.uber.autodispose.android.ViewScopeProvider
import com.uber.autodispose.autoDispose
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.extensions.forEach
import org.prauga.messages.common.util.extensions.resolveThemeColor
import org.prauga.messages.databinding.TabViewBinding
import org.prauga.messages.extensions.Optional
import org.prauga.messages.injection.appComponent
import org.prauga.messages.repository.ConversationRepository
import javax.inject.Inject

class PagerTitleView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    @Inject
    lateinit var colors: Colors
    @Inject
    lateinit var conversationRepo: ConversationRepository

    private val recipientId: Subject<Long> = BehaviorSubject.create()

    var pager: ViewPager? = null
        set(value) {
            if (field !== value) {
                field = value
                recreate()
            }
        }

    init {
        if (!isInEditMode) appComponent.inject(this)
    }

    fun setRecipientId(id: Long) {
        recipientId.onNext(id)
    }

    private fun recreate() {
        removeAllViews()

        pager?.adapter?.count?.forEach { position ->
            val view = TabViewBinding.inflate(LayoutInflater.from(context), this, false)
            view.label.text = pager?.adapter?.getPageTitle(position)
            view.root.setOnClickListener { pager?.currentItem = position }

            addView(view.root)
        }

        childCount.forEach { index ->
            getChildAt(index).isActivated = index == pager?.currentItem
        }

        pager?.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                childCount.forEach { index ->
                    getChildAt(index).isActivated = index == position
                }
            }
        })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val states = arrayOf(
            intArrayOf(android.R.attr.state_activated),
            intArrayOf(-android.R.attr.state_activated)
        )

        recipientId
            .distinctUntilChanged()
            .map { recipientId -> Optional(conversationRepo.getRecipient(recipientId)) }
            .switchMap { recipient -> colors.themeObservable(recipient.value) }
            .map { theme ->
                val textSecondary = context.resolveThemeColor(R.attr.textColorSecondary)
                ColorStateList(states, intArrayOf(theme.theme, textSecondary))
            }
            .autoDispose(ViewScopeProvider.from(this))
            .subscribe { colorStateList ->
                childCount.forEach { index ->
                    (getChildAt(index) as? TextView)?.setTextColor(colorStateList)
                }
            }
    }

}
