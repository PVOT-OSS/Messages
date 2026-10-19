/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.bluelinelabs.conductor.archlifecycle.LifecycleController
import org.prauga.messages.R

abstract class QkController<
    ViewContract : QkViewContract<State>,
    State : Any,
    Presenter : QkPresenter<ViewContract, State>,
> : LifecycleController() {

    abstract var presenter: Presenter

    private val appCompatActivity: AppCompatActivity?
        get() = activity as? AppCompatActivity

    protected val themedActivity: QkThemedActivity<*>?
        get() = activity as? QkThemedActivity<*>

    protected var containerView: View? = null

    @LayoutRes var layoutRes: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?,
    ): View {
        return inflater.inflate(layoutRes, container, false).also {
            containerView = it
            onViewCreated()
        }
    }

    open fun onViewCreated() {}

    fun setTitle(@StringRes titleId: Int) {
        setTitle(activity?.getString(titleId))
    }

    fun setTitle(title: CharSequence?) {
        activity?.title = title
        containerView?.findViewById<android.widget.TextView>(R.id.toolbarTitle)?.text = title
    }

    fun showBackButton(show: Boolean) {
        appCompatActivity?.supportActionBar?.setDisplayHomeAsUpEnabled(show)
    }

    override fun onDestroyView(view: View) {
        containerView = null
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onCleared()
    }
}
