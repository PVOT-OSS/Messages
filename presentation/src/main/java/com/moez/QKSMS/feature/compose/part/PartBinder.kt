/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.compose.part

import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.prauga.messages.common.base.QkViewHolder
import org.prauga.messages.common.util.Colors
import org.prauga.messages.model.Message
import org.prauga.messages.model.MmsPart

abstract class PartBinder {

    val clicks: Subject<Long> = PublishSubject.create()

    abstract val partLayout: Int

    abstract var theme: Colors.Theme

    abstract fun canBindPart(part: MmsPart): Boolean

    abstract fun bindPart(
        holder: QkViewHolder,
        part: MmsPart,
        message: Message,
        canGroupWithPrevious: Boolean,
        canGroupWithNext: Boolean,
    )
}
