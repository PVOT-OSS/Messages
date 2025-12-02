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
package dev.danascape.messages.feature.themepicker

import android.animation.ObjectAnimator
import android.view.View
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding2.view.clicks
import dev.danascape.messages.R
import dev.danascape.messages.common.base.QkController
import dev.danascape.messages.common.util.Colors
import dev.danascape.messages.common.util.extensions.dpToPx
import dev.danascape.messages.common.util.extensions.setBackgroundTint
import dev.danascape.messages.common.util.extensions.setVisible
import dev.danascape.messages.feature.themepicker.injection.ThemePickerModule
import dev.danascape.messages.injection.appComponent
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import dev.danascape.messages.common.widget.PagerTitleView
import dev.danascape.messages.common.widget.QkEditText
import dev.danascape.messages.common.widget.QkTextView
import javax.inject.Inject

class ThemePickerController(
    val recipientId: Long = 0L
) : QkController<ThemePickerView, ThemePickerState, ThemePickerPresenter>(), ThemePickerView {

    @Inject override lateinit var presenter: ThemePickerPresenter

    @Inject lateinit var colors: Colors
    @Inject lateinit var themeAdapter: ThemeAdapter
    @Inject lateinit var themePagerAdapter: ThemePagerAdapter

    private lateinit var contentView: View
    private lateinit var pager: ViewPager
    private lateinit var tabs: PagerTitleView
    private lateinit var materialColors: RecyclerView
    private lateinit var picker: HSVPickerView
    private lateinit var hex: QkEditText
    private lateinit var applyGroup: Group
    private lateinit var clear: ImageView
    private lateinit var apply: QkTextView

    private val viewQksmsPlusSubject: Subject<Unit> = PublishSubject.create()

    init {
        appComponent
                .themePickerBuilder()
                .themePickerModule(ThemePickerModule(this))
                .build()
                .inject(this)

        layoutRes = R.layout.theme_picker_controller
    }

    override fun onViewCreated() {
        super.onViewCreated()

        val view = containerView ?: return

        contentView = view.findViewById(R.id.contentView)
        pager = view.findViewById(R.id.pager)
        tabs = view.findViewById(R.id.tabs)
        materialColors = view.findViewById(R.id.materialColors)
        picker = view.findViewById(R.id.picker)
        hex = view.findViewById(R.id.hex)
        applyGroup = view.findViewById(R.id.applyGroup)
        clear = view.findViewById(R.id.clear)
        apply = view.findViewById(R.id.apply)

        pager.offscreenPageLimit = 1
        pager.adapter = themePagerAdapter
        tabs.pager = pager

        themeAdapter.data = colors.materialColors

        materialColors.layoutManager = LinearLayoutManager(activity)
        materialColors.adapter = themeAdapter
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.title_theme)
        showBackButton(true)

        themedActivity?.supportActionBar?.let { toolbar ->
            ObjectAnimator.ofFloat(toolbar, "elevation", toolbar.elevation, 0f).start()
        }
    }

    override fun onDetach(view: View) {
        super.onDetach(view)

        themedActivity?.supportActionBar?.let { toolbar ->
            ObjectAnimator.ofFloat(toolbar, "elevation", toolbar.elevation, 8.dpToPx(toolbar.themedContext).toFloat()).start()
        }
    }

    override fun showQksmsPlusSnackbar() {
        Snackbar.make(contentView, R.string.toast_qksms_plus, Snackbar.LENGTH_LONG).run {
            setAction(R.string.button_more) { viewQksmsPlusSubject.onNext(Unit) }
            setActionTextColor(colors.theme().theme)
            show()
        }
    }

    override fun themeSelected(): Observable<Int> = themeAdapter.colorSelected

    override fun hsvThemeSelected(): Observable<Int> = picker.selectedColor

    override fun clearHsvThemeClicks(): Observable<*> = clear.clicks()

    override fun applyHsvThemeClicks(): Observable<*> = apply.clicks()

    override fun viewQksmsPlusClicks(): Observable<*> = viewQksmsPlusSubject

    override fun render(state: ThemePickerState) {
        tabs.setRecipientId(state.recipientId)

        hex.setText(Integer.toHexString(state.newColor).takeLast(6))

        applyGroup.setVisible(state.applyThemeVisible)
        apply.setBackgroundTint(state.newColor)
        apply.setTextColor(state.newTextColor)
    }

    override fun setCurrentTheme(color: Int) {
        picker.setColor(color)
        themeAdapter.selectedColor = color
    }

}
