/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.content.Context
import android.content.Intent
import android.provider.Telephony
import io.reactivex.Flowable
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.prauga.messages.interactor.SyncMessages
import org.prauga.messages.util.Preferences
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class DefaultSmsChangedReceiverTest {

    @Mock private lateinit var context: Context

    @Mock private lateinit var intent: Intent

    @Mock private lateinit var prefs: Preferences

    @Mock private lateinit var syncMessages: SyncMessages

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun givenIsDefaultSmsAppTrue_whenOnReceive_thenSyncsMessages() {
        `when`(intent.getBooleanExtra(Telephony.Sms.Intents.EXTRA_IS_DEFAULT_SMS_APP, false))
            .thenReturn(true)
        `when`(syncMessages.buildObservable(Unit)).thenReturn(Flowable.just(Unit))

        // Simulate what the receiver does when it becomes default SMS app
        if (intent.getBooleanExtra(Telephony.Sms.Intents.EXTRA_IS_DEFAULT_SMS_APP, false)) {
            syncMessages.buildObservable(Unit).subscribe()
        }

        verify(syncMessages).buildObservable(Unit)
    }

    @Test
    fun givenIsDefaultSmsAppFalse_whenOnReceive_thenDoesNotSync() {
        `when`(intent.getBooleanExtra(Telephony.Sms.Intents.EXTRA_IS_DEFAULT_SMS_APP, false))
            .thenReturn(false)

        // Simulate what the receiver does when it's no longer default SMS app
        if (intent.getBooleanExtra(Telephony.Sms.Intents.EXTRA_IS_DEFAULT_SMS_APP, false)) {
            syncMessages.buildObservable(Unit).subscribe()
        }

        verify(syncMessages, never()).buildObservable(Unit)
    }

    @Test
    fun givenNoExtraProvided_whenGetBooleanExtra_thenDefaultsToFalse() {
        `when`(intent.getBooleanExtra(Telephony.Sms.Intents.EXTRA_IS_DEFAULT_SMS_APP, false))
            .thenReturn(false)

        val isDefaultSmsApp =
            intent.getBooleanExtra(Telephony.Sms.Intents.EXTRA_IS_DEFAULT_SMS_APP, false)
        assert(!isDefaultSmsApp)
    }
}
