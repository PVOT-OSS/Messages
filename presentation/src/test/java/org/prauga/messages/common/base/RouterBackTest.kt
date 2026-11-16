/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.base

import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class RouterBackTest {

    class Screen : Controller() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup,
            savedViewState: android.os.Bundle?,
        ): View = View(container.context)
    }

    private lateinit var activity: ComponentActivity
    private lateinit var router: Router

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val container = FrameLayout(activity).also(activity::setContentView)
        router = Conductor.attachRouter(activity, container, null)
        router.setRoot(RouterTransaction.with(Screen()))
        activity.handleBackWith(router)
    }

    @Test
    fun givenOnlyTheRootScreen_whenChecked_thenBackIsLeftToTheSystem() {
        assertFalse(activity.onBackPressedDispatcher.hasEnabledCallbacks())
    }

    @Test
    fun givenAPushedScreen_whenBackIsPressed_thenItPopsAndTheRootIsLeftToTheSystem() {
        router.pushController(RouterTransaction.with(Screen()))
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(activity.onBackPressedDispatcher.hasEnabledCallbacks())

        activity.onBackPressedDispatcher.onBackPressed()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, router.backstackSize)
        assertFalse(activity.isFinishing)
        assertFalse(activity.onBackPressedDispatcher.hasEnabledCallbacks())
    }

    @Test
    @Suppress("DEPRECATION")
    fun givenAndroidBeforeThirteen_whenTheBackKeyIsPressed_thenTheSameCallbacksRun() {
        router.pushController(RouterTransaction.with(Screen()))
        shadowOf(Looper.getMainLooper()).idle()

        activity.onBackPressed()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, router.backstackSize)
        assertFalse(activity.isFinishing)

        activity.onBackPressed()
        assertTrue(activity.isFinishing)
    }
}
