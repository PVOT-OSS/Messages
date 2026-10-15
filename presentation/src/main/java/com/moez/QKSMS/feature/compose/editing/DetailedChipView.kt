/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.compose.editing

import android.content.Context
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.RelativeLayout
import javax.inject.Inject
import org.prauga.messages.common.util.AnimationConfig
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.extensions.setBackgroundTint
import org.prauga.messages.common.util.extensions.setTint
import org.prauga.messages.databinding.ContactChipDetailedBinding
import org.prauga.messages.injection.appComponent
import org.prauga.messages.model.Recipient

class DetailedChipView(context: Context) : RelativeLayout(context) {

    @Inject lateinit var colors: Colors

    private val binding: ContactChipDetailedBinding

    init {
        binding =
            ContactChipDetailedBinding.inflate(
                android.view.LayoutInflater.from(context),
                this,
                true,
            )
        appComponent.inject(this)

        setOnClickListener { hide() }

        visibility = View.GONE

        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun setRecipient(recipient: Recipient) {
        binding.avatar.setRecipient(recipient)
        binding.name.text = recipient.contact?.name?.takeIf { it.isNotBlank() } ?: recipient.address
        binding.info.text = recipient.address

        colors.theme(recipient).let { theme ->
            binding.card.setBackgroundTint(theme.theme)
            binding.name.setTextColor(theme.textPrimary)
            binding.info.setTextColor(theme.textTertiary)
            binding.delete.setTint(theme.textPrimary)
        }
    }

    fun show() {
        startAnimation(AlphaAnimation(0f, 1f).apply { duration = AnimationConfig.FADE_DURATION_MS })

        visibility = View.VISIBLE
        requestFocus()
        isClickable = true
    }

    fun hide() {
        startAnimation(AlphaAnimation(1f, 0f).apply { duration = AnimationConfig.FADE_DURATION_MS })

        visibility = View.GONE
        clearFocus()
        isClickable = false
    }

    fun setOnDeleteListener(listener: (View) -> Unit) {
        binding.delete.setOnClickListener(listener)
    }
}
