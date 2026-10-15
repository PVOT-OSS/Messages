/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.changelog

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import org.prauga.messages.R
import org.prauga.messages.common.base.QkAdapter
import org.prauga.messages.common.base.QkBindingViewHolder
import org.prauga.messages.databinding.ChangelogListItemBinding
import org.prauga.messages.manager.ChangelogManager

class ChangelogAdapter(private val context: Context) :
    QkAdapter<ChangelogAdapter.ChangelogItem, QkBindingViewHolder<ChangelogListItemBinding>>() {

    data class ChangelogItem(val type: Int, val label: String)

    fun setChangelog(changelog: ChangelogManager.CumulativeChangelog) {
        val changes = mutableListOf<ChangelogItem>()
        if (changelog.added.isNotEmpty()) {
            changes += ChangelogItem(0, context.getString(R.string.changelog_added))
            changes += changelog.added.map { change -> ChangelogItem(1, "• $change") }
            changes += ChangelogItem(0, "")
        }
        if (changelog.improved.isNotEmpty()) {
            changes += ChangelogItem(0, context.getString(R.string.changelog_improved))
            changes += changelog.improved.map { change -> ChangelogItem(1, "• $change") }
            changes += ChangelogItem(0, "")
        }
        if (changelog.fixed.isNotEmpty()) {
            changes += ChangelogItem(0, context.getString(R.string.changelog_fixed))
            changes += changelog.fixed.map { change -> ChangelogItem(1, "• $change") }
            changes += ChangelogItem(0, "")
        }
        if (changelog.removed.isNotEmpty()) {
            changes += ChangelogItem(0, context.getString(R.string.changelog_removed))
            changes += changelog.removed.map { change -> ChangelogItem(1, "• $change") }
            changes += ChangelogItem(0, "")
        }
        data = changes
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): QkBindingViewHolder<ChangelogListItemBinding> {
        val binding =
            ChangelogListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QkBindingViewHolder(binding).apply {
            if (viewType == 0) {
                binding.changelogItem.setTypeface(binding.changelogItem.typeface, Typeface.BOLD)
            }
        }
    }

    override fun onBindViewHolder(
        holder: QkBindingViewHolder<ChangelogListItemBinding>,
        position: Int,
    ) {
        val item = getItem(position)
        holder.binding.changelogItem.text = item.label
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type
    }
}
