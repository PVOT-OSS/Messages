/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */

package org.prauga.messages.app

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.viewbinding.ViewBinding
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for BaseViewBindingActivity.
 *
 * These tests verify that the base activity correctly initializes ViewBinding,
 * sets the content view during onCreate, and maintains the binding throughout
 * the activity lifecycle.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class BaseViewBindingActivityTest {

    private lateinit var activity: TestActivity

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(TestActivity::class.java)
            .create()
            .get()
    }

    @Test
    fun givenActivity_whenOnCreateCalled_thenBindingIsNonNull() {
        val binding = activity.getBinding()

        assertNotNull("Binding should be non-null after onCreate", binding)
    }

    @Test
    fun givenActivity_whenBindingAccessedMultipleTimes_thenSameInstance() {
        val firstBindingReference = activity.getBinding()
        val secondBindingReference = activity.getBinding()

        assertSame(
            "Binding should be the same instance across multiple accesses",
            firstBindingReference,
            secondBindingReference
        )
    }

    @Test
    fun givenActivity_whenOnCreateCalled_thenBindingRootIsSetAsContentView() {
        val binding = activity.getBinding()

        val content = activity.findViewById<FrameLayout>(android.R.id.content)
        assertNotNull("android.R.id.content should exist", content)

        val rootView = content.getChildAt(0)
        assertSame("Binding root should be set as content view", binding.root, rootView)
    }

    @Test
    fun givenActivityLifecycle_whenResumed_thenBindingStaysNonNull() {
        val testActivity = Robolectric.buildActivity(TestActivity::class.java)
            .create()
            .start()
            .resume()
            .get()

        val binding = testActivity.getBinding()
        assertNotNull("Binding should remain non-null through lifecycle", binding)
        assertTrue("Binding root should still be a FrameLayout", binding.root is FrameLayout)
    }

    // Test ViewBinding implementation
    class TestViewBinding(private val rootView: FrameLayout) : ViewBinding {
        override fun getRoot(): View = rootView
    }

    // Test Activity implementation
    class TestActivity : BaseViewBindingActivity<TestViewBinding>(
        { layoutInflater ->
            TestViewBinding(FrameLayout(layoutInflater.context))
        }
    ) {
        fun getBinding(): TestViewBinding = binding
    }
}
