/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.base

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

open class QkViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val containerView: View = view
}

open class QkBindingViewHolder<T : ViewBinding>(val binding: T) : QkViewHolder(binding.root)
