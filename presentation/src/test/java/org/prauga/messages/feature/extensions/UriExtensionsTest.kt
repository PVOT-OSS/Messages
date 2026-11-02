/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.extensions

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowMediaMetadataRetriever
import org.robolectric.shadows.util.DataSource

@RunWith(RobolectricTestRunner::class)
class UriExtensionsTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun givenAudioTheRetrieverCannotOpen_whenEmbeddedPicture_thenNullInsteadOfCrash() {
        val uri = Uri.parse("content://org.prauga.messages.test/broken.mp3")
        ShadowMediaMetadataRetriever.addException(
            DataSource.toDataSource(context, uri),
            RuntimeException("setDataSource failed: status = 0xFFFFFFEA"),
        )

        assertNull(uri.embeddedPicture(context))
    }
}
