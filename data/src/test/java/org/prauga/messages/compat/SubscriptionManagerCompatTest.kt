/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.compat

import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class SubscriptionManagerCompatTest {

    private val sim1 = subscription(1)
    private val sim2 = subscription(2)
    private val subs = listOf(sim1, sim2)

    private fun subscription(id: Int) =
        SubscriptionInfoCompat(mock<SubscriptionInfo> { on { subscriptionId } doReturn id })

    @Test
    fun givenNoConversationHistory_whenPreferredSubscription_thenUsesSystemDefaultSmsSim() {
        val result = SubscriptionManagerCompat.preferredSubscription(subs, -1, 2)

        assertEquals(sim2, result)
    }

    @Test
    fun givenSimUsedInConversation_whenPreferredSubscription_thenUsesConversationSim() {
        val result = SubscriptionManagerCompat.preferredSubscription(subs, 1, 2)

        assertEquals(sim1, result)
    }

    @Test
    fun givenAskEveryTime_whenPreferredSubscription_thenFallsBackToFirstSim() {
        val result =
            SubscriptionManagerCompat.preferredSubscription(
                subs,
                -1,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID,
            )

        assertEquals(sim1, result)
    }

    @Test
    fun givenConversationSimNoLongerActive_whenPreferredSubscription_thenUsesSystemDefaultSmsSim() {
        val result = SubscriptionManagerCompat.preferredSubscription(subs, 3, 2)

        assertEquals(sim2, result)
    }

    @Test
    fun givenNoActiveSims_whenPreferredSubscription_thenReturnsNull() {
        assertNull(SubscriptionManagerCompat.preferredSubscription(emptyList(), 1, 2))
    }
}
