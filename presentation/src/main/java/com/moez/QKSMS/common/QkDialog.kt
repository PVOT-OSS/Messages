/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common

import android.app.Activity
import android.app.Dialog
import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import javax.inject.Inject
import org.prauga.messages.R
import org.prauga.messages.common.util.extensions.dpToPx
import org.prauga.messages.common.util.extensions.setPadding
import org.prauga.messages.injection.appComponent
import timber.log.Timber

/** Wrapper around AlertDialog which makes it easier to display lists that use our UI */
class QkDialog @Inject constructor(private val context: Context, val adapter: MenuItemAdapter) {

    var title: String? = null

    init {
        appComponent.inject(this)
    }

    fun show(activity: Activity) {
        val recyclerView = RecyclerView(activity)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        recyclerView.setPadding(top = 8.dpToPx(context), bottom = 8.dpToPx(context))

        val dialog =
            AlertDialog.Builder(activity!!, R.style.AppThemeDialog)
                .setTitle(title)
                .setView(recyclerView)
                .create()

        val clicks = adapter.menuItemClicks.dismissOnNext(dialog)

        dialog.setOnDismissListener { clicks.dispose() }

        dialog.show()
    }

    fun setTitle(@StringRes title: Int) {
        this.title = context.getString(title)
    }
}

fun Observable<*>.dismissOnNext(dialog: Dialog): Disposable =
    subscribe({ if (dialog.isShowing) dialog.dismiss() }, Timber::w)
