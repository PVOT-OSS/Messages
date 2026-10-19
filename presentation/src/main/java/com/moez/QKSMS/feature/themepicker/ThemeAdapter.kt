/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.themepicker

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import org.prauga.messages.common.base.QkAdapter
import org.prauga.messages.common.base.QkViewHolder
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.extensions.dpToPx
import org.prauga.messages.common.util.extensions.setBackgroundTint
import org.prauga.messages.common.util.extensions.setTint
import org.prauga.messages.common.util.extensions.setVisible
import org.prauga.messages.databinding.ThemeListItemBinding
import org.prauga.messages.databinding.ThemePaletteListItemBinding

class ThemeAdapter @Inject constructor(private val context: Context, private val colors: Colors) :
    QkAdapter<List<Int>, QkViewHolder>() {

    val colorSelected: Subject<Int> = PublishSubject.create()

    var selectedColor: Int = -1
        set(value) {
            val oldPosition = data.indexOfFirst { it.contains(field) }
            val newPosition = data.indexOfFirst { it.contains(value) }

            field = value
            iconTint = colors.textPrimaryOnThemeForColor(value)

            oldPosition.takeIf { it != -1 }?.let { position -> notifyItemChanged(position) }
            newPosition.takeIf { it != -1 }?.let { position -> notifyItemChanged(position) }
        }

    private var iconTint = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val binding =
            ThemePaletteListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.palette.flexWrap = FlexWrap.WRAP
        binding.palette.flexDirection = FlexDirection.ROW

        return QkViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val palette = getItem(position)
        val binding = ThemePaletteListItemBinding.bind(holder.containerView)

        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val minPadding = (16 * 6).dpToPx(context)
        val size =
            if (screenWidth - minPadding > (56 * 5).dpToPx(context)) {
                56.dpToPx(context)
            } else {
                (screenWidth - minPadding) / 5
            }
        val swatchPadding = (screenWidth - size * 5) / 12

        binding.palette.removeAllViews()
        binding.palette.setPadding(swatchPadding, swatchPadding, swatchPadding, swatchPadding)

        (palette.subList(0, 5) + palette.subList(5, 10).reversed())
            .mapIndexed { index, color ->
                val itemBinding =
                    ThemeListItemBinding.inflate(
                        LayoutInflater.from(context),
                        binding.palette,
                        false,
                    )
                itemBinding.root.apply {

                    // Send clicks to the selected subject
                    setOnClickListener { colorSelected.onNext(color) }

                    // Apply the color to the view
                    itemBinding.theme.setBackgroundTint(color)

                    // Control the check visibility and tint
                    itemBinding.check.setVisible(color == selectedColor)
                    itemBinding.check.setTint(iconTint)

                    // Update the size so that the spacing is perfectly even
                    layoutParams =
                        (layoutParams as FlexboxLayout.LayoutParams).apply {
                            height = size
                            width = size
                            isWrapBefore = index % 5 == 0
                            setMargins(swatchPadding, swatchPadding, swatchPadding, swatchPadding)
                        }
                }
            }
            .forEach { theme -> binding.palette.addView(theme) }
    }
}
