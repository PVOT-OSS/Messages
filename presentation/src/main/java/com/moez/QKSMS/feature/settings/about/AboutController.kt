/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.settings.about

import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import javax.inject.Inject
import org.prauga.messages.BuildConfig
import org.prauga.messages.R
import org.prauga.messages.common.base.QkController
import org.prauga.messages.common.widget.PreferenceView
import org.prauga.messages.databinding.AboutControllerBinding
import org.prauga.messages.injection.appComponent

class AboutController : QkController<AboutView, Unit, AboutPresenter>(), AboutView {

    @Inject override lateinit var presenter: AboutPresenter

    private var _binding: AboutControllerBinding? = null
    private val binding
        get() = _binding!!

    init {
        appComponent.inject(this)
        layoutRes = R.layout.about_controller
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        _binding = AboutControllerBinding.bind(view)
        binding.version.summary = BuildConfig.VERSION_NAME
        presenter.bindIntents(this)
        setTitle(R.string.about_title)
        showBackButton(true)
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        _binding = null
    }

    override fun preferenceClicks(): Observable<PreferenceView> =
        (0 until binding.preferences.childCount)
            .map { index -> binding.preferences.getChildAt(index) }
            .mapNotNull { view -> view as? PreferenceView }
            .map { preference -> preference.clicks().map { preference } }
            .let { preferences -> Observable.merge(preferences) }

    override fun render(state: Unit) {
        // No special rendering required
    }
}
