/*
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.carrier

import org.junit.Assert.assertEquals
import org.junit.Test

class MmsConfigOverridesTest {

    @Test
    fun givenCarrierUserAgent_whenUserAgent_thenUsesCarrierValue() {
        assertEquals("CarrierUA/1.0", MmsConfigOverrides.userAgent("CarrierUA/1.0", "1.0.8"))
    }

    @Test
    fun givenNoCarrierUserAgent_whenUserAgent_thenFallsBackToAppVersion() {
        assertEquals("Messages 1.0.8", MmsConfigOverrides.userAgent(null, "1.0.8"))
        assertEquals("Messages 1.0.8", MmsConfigOverrides.userAgent("  ", "1.0.8"))
    }

    @Test
    fun givenNoVersion_whenUserAgent_thenUsesAppNameOnly() {
        assertEquals("Messages", MmsConfigOverrides.userAgent(null, null))
    }

    @Test
    fun givenDeviceDetails_whenCustomUserAgent_thenJoinsThemWithoutSpaces() {
        assertEquals(
            "Google/Pixel_9/Android-17/Messages_1.0.8",
            MmsConfigOverrides.customUserAgent("Google", "Pixel 9", "17", "1.0.8"),
        )
    }
}
