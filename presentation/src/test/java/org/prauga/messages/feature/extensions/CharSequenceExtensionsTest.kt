/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.extensions

import android.annotation.SuppressLint
import androidx.emoji2.text.EmojiCompat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@SuppressLint("RestrictedApi")
@RunWith(RobolectricTestRunner::class)
class CharSequenceExtensionsTest {

    @After fun tearDown() = clearEmojiCompat()

    private fun clearEmojiCompat() {
        EmojiCompat::class.java.getDeclaredField("sInstance").apply {
            isAccessible = true
            set(null, null)
        }
    }

    @Test
    fun givenEmojiCompatNotInitialized_whenIsEmojiOnly_thenFalseInsteadOfCrash() {
        clearEmojiCompat()

        assertFalse("😀".isEmojiOnly())
    }

    @Test
    fun givenEmojiCompatStillLoading_whenIsEmojiOnly_thenFalseInsteadOfCrash() {
        val config = object : EmojiCompat.Config({ _ -> }) {}
        EmojiCompat.reset(config)
        assertEquals(EmojiCompat.LOAD_STATE_LOADING, EmojiCompat.get().loadState)

        assertFalse("😀".isEmojiOnly())
    }
}
