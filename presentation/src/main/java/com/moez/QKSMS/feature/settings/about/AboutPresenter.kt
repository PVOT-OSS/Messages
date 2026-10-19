/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.settings.about

import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import javax.inject.Inject
import org.prauga.messages.R
import org.prauga.messages.common.Navigator
import org.prauga.messages.common.base.QkPresenter

class AboutPresenter @Inject constructor(private val navigator: Navigator) :
    QkPresenter<AboutView, Unit>(Unit) {

    override fun bindIntents(view: AboutView) {
        super.bindIntents(view)

        view.preferenceClicks().autoDispose(view.scope()).subscribe { preference ->
            when (preference.id) {
                R.id.developer -> navigator.showDeveloper()

                R.id.source -> navigator.showSourceCode()

                R.id.changelog -> navigator.showChangelog()

                R.id.contact -> navigator.showSupport()

                R.id.license -> navigator.showLicense()
            }
        }
    }
}
