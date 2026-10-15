/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.changehandler.AnimatorChangeHandler
import org.prauga.messages.common.util.extensions.dpToPx

class QkChangeHandler : AnimatorChangeHandler(250, true) {

    @NonNull
    override fun getAnimator(
        @NonNull container: ViewGroup,
        @Nullable from: View?,
        @Nullable to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean,
    ): Animator {
        val animatorSet = AnimatorSet()
        animatorSet.interpolator = DecelerateInterpolator()

        if (isPush) {
            if (from != null) {
                animatorSet.play(
                    ObjectAnimator.ofFloat(from, View.TRANSLATION_X, -from.width.toFloat() / 4)
                )
            }
            if (to != null) {
                to.translationZ = 8.dpToPx(to.context).toFloat()
                animatorSet.play(
                    ObjectAnimator.ofFloat(to, View.TRANSLATION_X, to.width.toFloat(), 0f)
                )
            }
        } else {
            if (from != null) {
                from.translationZ = 8.dpToPx(from.context).toFloat()
                animatorSet.play(
                    ObjectAnimator.ofFloat(from, View.TRANSLATION_X, from.width.toFloat())
                )
            }
            if (to != null) {
                // Allow this to have a nice transition when coming off an aborted push animation
                val fromLeft = from?.translationX ?: 0f
                animatorSet.play(
                    ObjectAnimator.ofFloat(to, View.TRANSLATION_X, fromLeft - to.width / 4, 0f)
                )
            }
        }

        return animatorSet
    }

    override fun resetFromView(@NonNull from: View) {
        from.translationX = 0f
        from.translationZ = 0f
    }

    @NonNull
    override fun copy(): ControllerChangeHandler {
        return QkChangeHandler()
    }
}
