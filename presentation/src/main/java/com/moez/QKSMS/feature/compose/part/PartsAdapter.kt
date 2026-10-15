/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.compose.part

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable
import javax.inject.Inject
import org.prauga.messages.R
import org.prauga.messages.common.base.QkViewHolder
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.widget.QkContextMenuRecyclerView
import org.prauga.messages.extensions.isSmil
import org.prauga.messages.extensions.isText
import org.prauga.messages.feature.compose.BubbleUtils.canGroup
import org.prauga.messages.feature.compose.MessagesAdapter
import org.prauga.messages.model.Message
import org.prauga.messages.model.MmsPart

class PartsAdapter
@Inject
constructor(
    colors: Colors,
    fileBinder: FileBinder,
    imageBinder: ImageBinder,
    audioBinder: AudioBinder,
    vCardBinder: VCardBinder,
) :
    QkContextMenuRecyclerView.Adapter<
        Long,
        MmsPart,
        QkContextMenuRecyclerView.ViewHolder<MmsPart>,
    >() {

    private val partBinders = listOf(audioBinder, imageBinder, vCardBinder, fileBinder)

    var theme: Colors.Theme = colors.theme()
        set(value) {
            field = value
            partBinders.forEach { binder -> binder.theme = value }
        }

    val clicks: Observable<Long> = Observable.merge(partBinders.map { it.clicks })

    private lateinit var message: Message
    private var previous: Message? = null
    private var next: Message? = null
    private var bodyVisible: Boolean = true
    private var audioState: MessagesAdapter.AudioState? = null

    fun setData(
        message: Message,
        previous: Message?,
        next: Message?,
        holder: QkViewHolder,
        audioState: MessagesAdapter.AudioState?,
    ) {
        this.message = message
        this.previous = previous
        this.next = next
        this.bodyVisible =
            holder.containerView.findViewById<View>(R.id.body)?.visibility == View.VISIBLE
        this.data = message.parts.filter { !it.isSmil() && !it.isText() }
        this.audioState = audioState
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): QkContextMenuRecyclerView.ViewHolder<MmsPart> {
        val layout = partBinders.getOrNull(viewType)?.partLayout ?: 0
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return QkContextMenuRecyclerView.ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: QkContextMenuRecyclerView.ViewHolder<MmsPart>,
        position: Int,
    ) {
        val part = data[position]

        holder.contextMenuValue = part

        val canGroupWithPrevious = canGroup(message, previous) || position > 0
        val canGroupWithNext = canGroup(message, next) || position < itemCount - 1 || bodyVisible

        val binder = partBinders.firstOrNull { it.canBindPart(part) }
        if (binder == null) return

        // if audioState is set and binder is audio type, set it's audioState ref
        if ((audioState != null) && (binder is AudioBinder)) binder.audioState = audioState!!

        binder.bindPart(holder, part, message, canGroupWithPrevious, canGroupWithNext)
    }

    override fun getItemViewType(position: Int): Int {
        return partBinders.indexOfFirst { it.canBindPart(data[position]) }
    }
}
