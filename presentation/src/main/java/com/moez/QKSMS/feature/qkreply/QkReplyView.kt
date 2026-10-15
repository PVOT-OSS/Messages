/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.qkreply

import io.reactivex.Observable
import org.prauga.messages.common.base.QkView

interface QkReplyView : QkView<QkReplyState> {

    val menuItemIntent: Observable<Int>
    val textChangedIntent: Observable<CharSequence>
    val changeSimIntent: Observable<*>
    val sendIntent: Observable<Unit>

    fun setDraft(draft: String)

    fun finish()
}
