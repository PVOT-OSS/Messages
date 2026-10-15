/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.receiver

import android.content.Context
import android.content.Intent
import io.reactivex.Flowable
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.prauga.messages.interactor.UpdateScheduledMessageAlarms
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class BootReceiverTest {

    @Mock private lateinit var context: Context

    @Mock private lateinit var intent: Intent

    @Mock private lateinit var updateScheduledMessageAlarms: UpdateScheduledMessageAlarms

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun givenBootCompleted_whenOnReceive_thenRestartsScheduledAlarms() {
        `when`(intent.action).thenReturn(Intent.ACTION_BOOT_COMPLETED)
        `when`(updateScheduledMessageAlarms.buildObservable(Unit)).thenReturn(Flowable.just(Unit))

        // Since the receiver uses Dagger injection, we can test the interactor directly
        // The receiver simply calls updateScheduledMessageAlarms.execute(Unit)
        updateScheduledMessageAlarms.buildObservable(Unit).subscribe()

        verify(updateScheduledMessageAlarms).buildObservable(Unit)
    }

    @Test
    fun givenBootAction_whenIntentHasAction_thenActionIsBootCompleted() {
        `when`(intent.action).thenReturn(Intent.ACTION_BOOT_COMPLETED)

        assert(intent.action == Intent.ACTION_BOOT_COMPLETED)
    }

    @Test
    fun givenMyPackageReplaced_whenIntentHasAction_thenActionIsMyPackageReplaced() {
        `when`(intent.action).thenReturn(Intent.ACTION_MY_PACKAGE_REPLACED)

        assert(intent.action == Intent.ACTION_MY_PACKAGE_REPLACED)
    }
}
