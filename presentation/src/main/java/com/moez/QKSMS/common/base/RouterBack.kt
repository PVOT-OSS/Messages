/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.base

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.Router

/** Leaves the root screen's Back to the system, so Android 16+ plays its predictive animation */
fun ComponentActivity.handleBackWith(router: Router) {
    val callback =
        onBackPressedDispatcher.addCallback(this, router.backstackSize > 1) { router.handleBack() }
    router.addChangeListener(
        object : ControllerChangeHandler.ControllerChangeListener {
            override fun onChangeStarted(
                to: Controller?,
                from: Controller?,
                isPush: Boolean,
                container: ViewGroup,
                handler: ControllerChangeHandler,
            ) {
                callback.isEnabled = router.backstackSize > 1
            }

            override fun onChangeCompleted(
                to: Controller?,
                from: Controller?,
                isPush: Boolean,
                container: ViewGroup,
                handler: ControllerChangeHandler,
            ) {
                callback.isEnabled = router.backstackSize > 1
            }
        }
    )
}
