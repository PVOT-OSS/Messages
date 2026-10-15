/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.themepicker

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import javax.inject.Inject
import org.prauga.messages.R

class ThemePagerAdapter @Inject constructor(private val context: Context) : PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        return when (position) {
            1 -> container.findViewById(R.id.hsvPicker)
            else -> container.findViewById(R.id.materialColors)
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            1 -> context.getString(R.string.theme_plus)
            else -> context.getString(R.string.theme_material)
        }
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {}

    override fun getCount(): Int {
        return 2
    }
}
