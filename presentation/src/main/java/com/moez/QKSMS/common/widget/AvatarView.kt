/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import javax.inject.Inject
import org.prauga.messages.R
import org.prauga.messages.common.Navigator
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.extensions.getColorCompat
import org.prauga.messages.common.util.extensions.setBackgroundTint
import org.prauga.messages.common.util.extensions.setTint
import org.prauga.messages.databinding.AvatarViewBinding
import org.prauga.messages.injection.appComponent
import org.prauga.messages.model.Recipient
import org.prauga.messages.util.GlideApp

class AvatarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    @Inject lateinit var colors: Colors
    @Inject lateinit var navigator: Navigator

    private var lookupKey: String? = null
    private var fullName: String? = null
    private var photoUri: String? = null
    private var lastUpdated: Long? = null
    private var theme: Colors.Theme
    private var layout: AvatarViewBinding

    init {
        if (!isInEditMode) {
            appComponent.inject(this)
        }

        theme = colors.theme()

        layout = AvatarViewBinding.inflate(LayoutInflater.from(context), this)
        setBackgroundResource(R.drawable.avatar_circle)
        clipToOutline = true
    }

    /** Use the [contact] information to display the avatar. */
    fun setRecipient(recipient: Recipient?) {
        lookupKey = recipient?.contact?.lookupKey
        fullName = recipient?.contact?.name
        photoUri = recipient?.contact?.photoUri
        lastUpdated = recipient?.contact?.lastUpdate
        theme = colors.theme(recipient)
        updateView()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (!isInEditMode) {
            updateView()
        }
    }

    private fun updateView() {
        // Apply theme
        layout.initial.setTextColor(theme.textPrimary)
        layout.icon.setTint(context.getColorCompat(R.color.avatar_icon_color))

        val initials =
            fullName
                ?.substringBefore(',')
                ?.split(" ")
                .orEmpty()
                .filter { name -> name.isNotEmpty() }
                .map { name -> name[0] }
                .filter { initial -> initial.isLetterOrDigit() }
                .map { initial -> initial.toString() }

        val hasAlphabetInitial =
            initials.any { initial -> initial.firstOrNull()?.isLetter() == true }

        if (initials.isNotEmpty()) {
            layout.initial.text =
                if (initials.size > 1) initials.first() + initials.last() else initials.first()
            layout.icon.visibility = GONE
        } else {
            layout.initial.text = null
            layout.icon.visibility = VISIBLE
        }

        setBackgroundTint(
            if (hasAlphabetInitial) context.getColorCompat(R.color.avatar_background) else null
        )

        layout.photo.setImageDrawable(null)
        photoUri?.let { photoUri -> GlideApp.with(layout.photo).load(photoUri).into(layout.photo) }
    }
}
