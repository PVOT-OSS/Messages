/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.model

import android.content.Context
import android.net.Uri
import java.io.File
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AttachmentTest {

    private lateinit var context: Context
    private val created = mutableListOf<File>()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @After
    fun tearDown() {
        created.forEach { it.deleteRecursively() }
    }

    private fun newFile(dir: File, name: String): File =
        File(dir, name).apply {
            parentFile?.mkdirs()
            writeText("data")
            created += this
        }

    @Test
    fun givenFileInCacheDir_whenRemoveCacheFile_thenDeletesFile() {
        val file = newFile(context.cacheDir, "recorded-test.3ga")

        assertTrue(Attachment(context, Uri.fromFile(file)).removeCacheFile(context))
        assertFalse(file.exists())
    }

    @Test
    fun givenFileInFilesDir_whenRemoveCacheFile_thenKeepsFile() {
        val file = newFile(context.filesDir, "scheduled-1/abc/photo.jpg")

        assertFalse(Attachment(context, Uri.fromFile(file)).removeCacheFile(context))
        assertTrue(file.exists())
    }

    @Test
    fun givenPathEscapingCacheDir_whenRemoveCacheFile_thenKeepsFile() {
        val file = newFile(context.filesDir, "default.realm")
        val escaping = Uri.parse("file://${context.cacheDir.absolutePath}/../files/default.realm")

        assertFalse(Attachment(context, escaping).removeCacheFile(context))
        assertTrue(file.exists())
    }

    @Test
    fun givenCacheDirItself_whenRemoveCacheFile_thenKeepsDir() {
        context.cacheDir.mkdirs()

        assertFalse(Attachment(context, Uri.fromFile(context.cacheDir)).removeCacheFile(context))
        assertTrue(context.cacheDir.exists())
    }

    @Test
    fun givenContentUri_whenRemoveCacheFile_thenReturnsFalse() {
        val uri = Uri.parse("content://media/external/images/media/1")

        assertFalse(Attachment(context, uri).removeCacheFile(context))
    }
}
