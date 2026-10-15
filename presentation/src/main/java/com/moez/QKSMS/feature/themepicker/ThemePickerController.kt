/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.themepicker

import android.animation.ObjectAnimator
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import org.prauga.messages.R
import org.prauga.messages.common.base.QkController
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.extensions.dpToPx
import org.prauga.messages.common.util.extensions.setBackgroundTint
import org.prauga.messages.common.util.extensions.setVisible
import org.prauga.messages.common.widget.PagerTitleView
import org.prauga.messages.common.widget.QkEditText
import org.prauga.messages.common.widget.QkTextView
import org.prauga.messages.feature.themepicker.injection.ThemePickerModule
import org.prauga.messages.injection.appComponent

class ThemePickerController(val recipientId: Long = 0L) :
    QkController<ThemePickerView, ThemePickerState, ThemePickerPresenter>(), ThemePickerView {

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
            ObjectAnimator.ofFloat(
                    toolbar,
                    "elevation",
                    toolbar.elevation,
                    8.dpToPx(toolbar.themedContext).toFloat(),
                )
                .start()
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
