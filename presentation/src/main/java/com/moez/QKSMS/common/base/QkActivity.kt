/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.base

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.viewbinding.ViewBinding
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import org.prauga.messages.R
import org.prauga.messages.app.BaseViewBindingActivity
import org.prauga.messages.util.Preferences

abstract class QkActivity<Vb : ViewBinding>(inflate: (LayoutInflater) -> Vb) :
    BaseViewBindingActivity<Vb>(inflate) {
    @Inject lateinit var prefs: Preferences

    protected val menu: Subject<Menu> = BehaviorSubject.create()

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onNewIntent(intent)
        disableScreenshots(prefs.disableScreenshots.get())
    }

    override fun onResume() {
        super.onResume()
        disableScreenshots(prefs.disableScreenshots.get())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        setSupportActionBar(findToolbar())
        title = title // The title may have been set before layout inflation
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        setSupportActionBar(findToolbar())
        title = title // The title may have been set before layout inflation
    }

    override fun setTitle(titleId: Int) {
        title = getString(titleId)
    }

    override fun setTitle(title: CharSequence?) {
        super.setTitle(title)
        findToolbarTitle()?.text = title
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        if (menu != null) {
            this.menu.onNext(menu)
        }
        return result
    }

    protected open fun showBackButton(show: Boolean) {
        supportActionBar?.setDisplayHomeAsUpEnabled(show)
    }

    private fun disableScreenshots(disableScreenshots: Boolean) {
        if (disableScreenshots) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    protected fun findToolbar(): Toolbar? = findViewById(R.id.toolbar)

    protected fun findToolbarTitle(): TextView? = findViewById(R.id.toolbarTitle)
}
