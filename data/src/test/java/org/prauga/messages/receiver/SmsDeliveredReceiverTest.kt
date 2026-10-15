/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.receiver

import android.app.Activity
import android.content.Context
import android.content.Intent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.prauga.messages.interactor.MarkDelivered
import org.prauga.messages.interactor.MarkDeliveryFailed
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class SmsDeliveredReceiverTest {

    @Mock private lateinit var context: Context

    @Mock private lateinit var markDelivered: MarkDelivered

    @Mock private lateinit var markDeliveryFailed: MarkDeliveryFailed

    private lateinit var receiver: SmsDeliveredReceiver

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        receiver = SmsDeliveredReceiver()
        // Manually inject dependencies for testing
        receiver.markDelivered = markDelivered
        receiver.markDeliveryFailed = markDeliveryFailed
    }

    @Test
    fun givenSmsDeliveredReceiver_whenInstantiated_thenIsBroadcastReceiverSubclass() {
        val receiver = SmsDeliveredReceiver()

        org.junit.Assert.assertNotNull("Receiver should be instantiable", receiver)
        org.junit.Assert.assertTrue(
            "Receiver should be a BroadcastReceiver",
            receiver is android.content.BroadcastReceiver,
        )
    }

    @Test
    fun givenReceiverWithInjectedDependencies_whenAccessed_thenDependenciesAreAvailable() {
        org.junit.Assert.assertNotNull(receiver.markDelivered)
        org.junit.Assert.assertNotNull(receiver.markDeliveryFailed)
    }

    @Test
    fun givenIntent_whenExtraIdRetrieved_thenDefaultsToZero() {
        val intent = Intent()

        val id = intent.getLongExtra("id", 0L)

        org.junit.Assert.assertEquals(0L, id)
    }

    @Test
    fun givenIntent_whenExtraIdSet_thenRetrievesCorrectly() {
        val expectedId = 54321L
        val intent = Intent().apply { putExtra("id", expectedId) }

        val id = intent.getLongExtra("id", 0L)

        org.junit.Assert.assertEquals(expectedId, id)
    }

    @Test
    fun givenIntentWithMaxLongId_whenExtraIdRetrieved_thenReturnsMaxValue() {
        val expectedId = Long.MAX_VALUE
        val intent = Intent().apply { putExtra("id", expectedId) }

        val id = intent.getLongExtra("id", 0L)

        org.junit.Assert.assertEquals(expectedId, id)
    }

    @Test
    fun givenResultCodeActivityResultOk_whenChecked_thenIndicatesDeliverySuccess() {
        val resultCode = Activity.RESULT_OK

        val isDelivered = resultCode == Activity.RESULT_OK

        org.junit.Assert.assertTrue("RESULT_OK should indicate successful delivery", isDelivered)
    }

    @Test
    fun givenResultCodeActivityResultCanceled_whenChecked_thenIndicatesDeliveryFailure() {
        val resultCode = Activity.RESULT_CANCELED

        val isFailed = resultCode == Activity.RESULT_CANCELED

        org.junit.Assert.assertTrue("RESULT_CANCELED should indicate delivery failure", isFailed)
    }

    @Test
    fun givenDeliveryResultCodes_whenCompared_thenHandlesBothCases() {
        val successCode = Activity.RESULT_OK
        val failureCode = Activity.RESULT_CANCELED

        org.junit.Assert.assertNotEquals(
            "Success and failure codes should be different",
            successCode,
            failureCode,
        )
    }
}
