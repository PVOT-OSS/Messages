/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.widget

import android.content.Context
import android.util.AttributeSet

class TightTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    QkTextView(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // Get a non-null copy of the layout, if available. Then ensure we've got multiple lines
        val layout = layout ?: return
        if (layout.lineCount <= 1) {
            return
        }

        val maxLineWidth = (0 until layout.lineCount).map(layout::getLineWidth).max() ?: 0f

        val width =
            Math.ceil(maxLineWidth.toDouble()).toInt() + compoundPaddingLeft + compoundPaddingRight
        if (width < measuredWidth) {
            val widthSpec =
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.getMode(widthMeasureSpec))
            super.onMeasure(widthSpec, heightMeasureSpec)
        }
    }
}
