/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.prauga.messages.feature.settings.about

import android.view.View
import com.jakewharton.rxbinding4.view.clicks
import org.prauga.messages.BuildConfig
import org.prauga.messages.R
import org.prauga.messages.common.base.QkController
import org.prauga.messages.common.widget.PreferenceView
import org.prauga.messages.databinding.AboutControllerBinding
import org.prauga.messages.injection.appComponent
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject

class AboutController : QkController<AboutView, Unit, AboutPresenter>(), AboutView {

    @Inject override lateinit var presenter: AboutPresenter

    private var _binding: AboutControllerBinding? = null
    private val binding get() = _binding!!

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

    override fun preferenceClicks(): Observable<PreferenceView> = (0 until binding.preferences.childCount)
            .map { index -> binding.preferences.getChildAt(index) }
            .mapNotNull { view -> view as? PreferenceView }
            .map { preference -> preference.clicks().map { preference } }
            .let { preferences -> Observable.merge(preferences) }

    override fun render(state: Unit) {
        // No special rendering required
    }

}