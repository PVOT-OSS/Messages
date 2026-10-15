/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.util.extensions

import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import io.reactivex.subjects.Subject

fun AlertDialog.Builder.setPositiveButton(
    @StringRes textId: Int,
    subject: Subject<Unit>,
): AlertDialog.Builder {
    return setPositiveButton(textId) { _, _ -> subject.onNext(Unit) }
}

fun AlertDialog.Builder.setNegativeButton(
    @StringRes textId: Int,
    subject: Subject<Unit>,
): AlertDialog.Builder {
    return setNegativeButton(textId) { _, _ -> subject.onNext(Unit) }
}

fun AlertDialog.Builder.setNeutralButton(
    @StringRes textId: Int,
    subject: Subject<Unit>,
): AlertDialog.Builder {
    return setNeutralButton(textId) { _, _ -> subject.onNext(Unit) }
}

fun AlertDialog.setShowing(show: Boolean) {
    if (isShowing && !show) {
        dismiss()
    } else if (!isShowing && show) {
        show()
    }
}
