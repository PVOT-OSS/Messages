/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.realm.Realm
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.messages.testutil.RealmTestHelper
import org.prauga.messages.util.PhoneNumberUtils

/**
 * Receivers and workers read on threads without a looper, where an instance left open elsewhere on
 * the same thread keeps every later read on its old snapshot
 */
@RunWith(AndroidJUnit4::class)
class BlockingRepositoryInstrumentedTest {

    private lateinit var repository: BlockingRepositoryImpl
    private lateinit var reader: ExecutorService
    private lateinit var leftOpen: Realm

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        RealmTestHelper.init(context)
        repository = BlockingRepositoryImpl(PhoneNumberUtils(context))
        reader = Executors.newSingleThreadExecutor()
        leftOpen = reader.submit<Realm> { Realm.getDefaultInstance() }.get()
    }

    @After
    fun tearDown() {
        reader.submit { leftOpen.close() }.get()
        reader.shutdown()
        RealmTestHelper.tearDown()
    }

    @Test
    fun givenAnInstanceLeftOpenOnTheReader_whenANumberIsBlockedElsewhere_thenTheReaderSeesIt() {
        assertThat(onReader { repository.isBlocked(NUMBER) }).isFalse()

        repository.blockNumber(NUMBER)

        assertThat(onReader { repository.isBlocked(NUMBER) }).isTrue()
    }

    @Test
    fun givenAnInstanceLeftOpenOnTheReader_whenItUnblocksANumberBlockedElsewhere_thenItIsUnblocked() {
        repository.blockNumber(NUMBER)

        onReader { repository.unblockNumbers(NUMBER) }

        assertThat(repository.isBlocked(NUMBER)).isFalse()
    }

    private fun <T> onReader(block: () -> T): T = reader.submit<T> { block() }.get()

    private companion object {
        const val NUMBER = "5557003"
    }
}
