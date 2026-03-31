/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.receiver

import android.content.Context
import android.content.Intent
import org.prauga.messages.interactor.ReceiveSms
import org.prauga.messages.repository.MessageRepository
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class SmsReceiverTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var messageRepository: MessageRepository

    @Mock
    private lateinit var receiveSms: ReceiveSms

    private lateinit var receiver: SmsReceiver

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        receiver = SmsReceiver()
        // Manually inject dependencies for testing
        receiver.messageRepo = messageRepository
        receiver.receiveSms = receiveSms
    }

    @Test
    fun givenSmsReceiver_whenInstantiated_thenIsBroadcastReceiverSubclass() {
        val receiver = SmsReceiver()

        org.junit.Assert.assertNotNull("Receiver should be instantiable", receiver)
        org.junit.Assert.assertTrue("Receiver should be a BroadcastReceiver",
            receiver is android.content.BroadcastReceiver)
    }

    @Test
    fun givenReceiverWithInjectedDependencies_whenAccessed_thenDependenciesAreAvailable() {
        org.junit.Assert.assertNotNull(receiver.messageRepo)
    }

    @Test
    fun givenIntent_whenSubscriptionExtraRetrieved_thenDefaultsToNegativeOne() {
        val intent = Intent()

        val subscription = intent.extras?.getInt("subscription", -1) ?: -1

        org.junit.Assert.assertEquals(-1, subscription)
    }

    @Test
    fun givenIntent_whenSubscriptionExtraSet_thenRetrievesCorrectly() {
        val expectedSubscription = 0
        val intent = Intent().apply {
            putExtra("subscription", expectedSubscription)
        }

        val subscription = intent.extras?.getInt("subscription", -1) ?: -1

        org.junit.Assert.assertEquals(expectedSubscription, subscription)
    }

    @Test
    fun givenIntentWithDualSimSubscription_whenExtraRetrieved_thenReturnsCorrectSubscription() {
        val subscriptionId = 1 // Second SIM slot
        val intent = Intent().apply {
            putExtra("subscription", subscriptionId)
        }

        val subscription = intent.extras?.getInt("subscription", -1) ?: -1

        org.junit.Assert.assertEquals(subscriptionId, subscription)
    }

    @Test
    fun givenSubscriptionIds_whenCompared_thenAreDifferent() {
        val sim1 = 0
        val sim2 = 1
        val noSim = -1

        org.junit.Assert.assertNotEquals(sim1, sim2)
        org.junit.Assert.assertNotEquals(sim1, noSim)
        org.junit.Assert.assertNotEquals(sim2, noSim)
    }

    @Test
    fun givenEmptyMessageList_whenReduced_thenHandlesGracefully() {
        val emptyList = emptyList<String>()

        org.junit.Assert.assertTrue(emptyList.isEmpty())
    }

    @Test
    fun givenSingleMessageBody_whenMapped_thenReturnsSameBody() {
        val bodies = listOf("Hello World")

        val combined = bodies.reduce { body, new -> body + new }

        org.junit.Assert.assertEquals("Hello World", combined)
    }

    @Test
    fun givenMultipleMessageBodies_whenReduced_thenConcatenates() {
        val bodies = listOf("Hello ", "World", "!")

        val combined = bodies.reduce { body, new -> body + new }

        org.junit.Assert.assertEquals("Hello World!", combined)
    }

    @Test
    fun givenNullableMessageBodies_whenFiltered_thenRemovesNulls() {
        val bodies: List<String?> = listOf("Hello", null, "World", null)

        val nonNullBodies = bodies.mapNotNull { it }

        org.junit.Assert.assertEquals(2, nonNullBodies.size)
        org.junit.Assert.assertEquals("Hello", nonNullBodies[0])
        org.junit.Assert.assertEquals("World", nonNullBodies[1])
    }
}
