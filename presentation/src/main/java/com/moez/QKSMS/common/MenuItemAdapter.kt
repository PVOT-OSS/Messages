/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ArrayRes
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import org.prauga.messages.common.base.QkAdapter
import org.prauga.messages.common.base.QkBindingViewHolder
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.extensions.resolveThemeColor
import org.prauga.messages.common.util.extensions.setVisible
import org.prauga.messages.databinding.MenuListItemBinding

data class MenuItem(val title: String, val actionId: Int)

class MenuItemAdapter
@Inject
constructor(private val context: Context, private val colors: Colors) :
    QkAdapter<MenuItem, QkBindingViewHolder<MenuListItemBinding>>() {

    val menuItemClicks: Subject<Int> = PublishSubject.create()

    private val disposables = CompositeDisposable()

    var selectedItem: Int? = null
        set(value) {
            val old = data.map { it.actionId }.indexOfFirst { it == field }
            val new = data.map { it.actionId }.indexOfFirst { it == value }

            field = value

            old.let { notifyItemChanged(it) }
            new.let { notifyItemChanged(it) }
        }

    fun setData(@ArrayRes titles: Int, @ArrayRes values: Int = -1) {
        val valueInts = if (values != -1) context.resources.getIntArray(values) else null

        data =
            context.resources.getStringArray(titles).mapIndexed { index, title ->
                MenuItem(title, valueInts?.getOrNull(index) ?: index)
            }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): QkBindingViewHolder<MenuListItemBinding> {
        val binding =
            MenuListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        val states =
            arrayOf(
                intArrayOf(android.R.attr.state_activated),
                intArrayOf(-android.R.attr.state_activated),
            )

        val text = parent.context.resolveThemeColor(android.R.attr.textColorTertiary)
        binding.check.imageTintList = ColorStateList(states, intArrayOf(colors.theme().theme, text))

        return QkBindingViewHolder(binding).apply {
            binding.root.setOnClickListener {
                val menuItem = getItem(adapterPosition)
                menuItemClicks.onNext(menuItem.actionId)
            }
        }
    }

    override fun onBindViewHolder(holder: QkBindingViewHolder<MenuListItemBinding>, position: Int) {
        val menuItem = getItem(position)

        holder.binding.title.text = menuItem.title
        holder.binding.check.isActivated = (menuItem.actionId == selectedItem)
        holder.binding.check.setVisible(selectedItem != null)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        disposables.clear()
    }
}
