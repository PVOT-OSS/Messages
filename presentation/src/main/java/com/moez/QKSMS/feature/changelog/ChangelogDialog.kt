/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.changelog

import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.prauga.messages.BuildConfig
import org.prauga.messages.R
import org.prauga.messages.databinding.ChangelogDialogBinding
import org.prauga.messages.feature.main.MainActivity
import org.prauga.messages.manager.ChangelogManager

class ChangelogDialog(activity: MainActivity) {

    val moreClicks: Subject<Unit> = PublishSubject.create()

    private val dialog: AlertDialog
    private val adapter = ChangelogAdapter(activity)

    init {
        val layout = ChangelogDialogBinding.inflate(LayoutInflater.from(activity))

        dialog =
            AlertDialog.Builder(activity!!, R.style.AppThemeDialog)
                .setCancelable(true)
                .setView(layout.root)
                .create()

        layout.version.text =
            activity.getString(R.string.changelog_version, BuildConfig.VERSION_NAME)
        layout.changelog.adapter = adapter
        layout.more.setOnClickListener {
            dialog.dismiss()
            moreClicks.onNext(Unit)
        }
        layout.dismiss.setOnClickListener { dialog.dismiss() }
    }

    fun show(changelog: ChangelogManager.CumulativeChangelog) {
        adapter.setChangelog(changelog)
        dialog.show()
    }
}
