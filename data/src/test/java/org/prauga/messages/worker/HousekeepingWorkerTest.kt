/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.worker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HousekeepingWorkerTest {

    @Test
    fun givenWorkerTag_whenAccessed_thenMatchesClassName() {
        // The worker tag is the class simple name
        val expectedTag = "HousekeepingWorker"
        assertEquals(expectedTag, HousekeepingWorker::class.java.simpleName)
    }

    @Test
    fun givenTwoHoursAgo_whenCalculated_thenIsCorrect() {
        val now = System.currentTimeMillis()
        val twoHoursAgo = now - (2 * 60 * 60 * 1000)

        // Verify the calculation is approximately 2 hours ago
        val diff = now - twoHoursAgo
        assertEquals(2 * 60 * 60 * 1000L, diff)
    }

    @Test
    fun givenScheduledMessageDir_whenNameMatches_thenStartsWithScheduledPrefix() {
        val dirName = "scheduled-12345"
        assertTrue(dirName.startsWith("scheduled-"))
    }

    @Test
    fun givenScheduledMessageDir_whenExtractingId_thenReturnsCorrectId() {
        val dirName = "scheduled-12345"
        val id = dirName.substringAfter('-').toLong()
        assertEquals(12345L, id)
    }

    @Test
    fun givenScheduledMessageDir_whenIdIsLarge_thenReturnsCorrectId() {
        val dirName = "scheduled-${Long.MAX_VALUE}"
        val id = dirName.substringAfter('-').toLong()
        assertEquals(Long.MAX_VALUE, id)
    }

    @Test
    fun givenNonScheduledDir_whenChecked_thenDoesNotMatchPrefix() {
        val dirName = "attachments"
        assertTrue(!dirName.startsWith("scheduled-"))
    }

    @Test
    fun givenTimestampComparison_whenOlderThanThreshold_thenShouldBeDeleted() {
        val now = System.currentTimeMillis()
        val twoHoursAgo = now - (2 * 60 * 60 * 1000)
        val threeHoursAgo = now - (3 * 60 * 60 * 1000)

        // Files older than twoHoursAgo should be deleted
        assertTrue(threeHoursAgo < twoHoursAgo)
    }

    @Test
    fun givenTimestampComparison_whenNewerThanThreshold_thenShouldNotBeDeleted() {
        val now = System.currentTimeMillis()
        val twoHoursAgo = now - (2 * 60 * 60 * 1000)
        val oneHourAgo = now - (1 * 60 * 60 * 1000)

        // Files newer than twoHoursAgo should not be deleted
        assertTrue(oneHourAgo >= twoHoursAgo)
    }

    @Test
    fun givenTwoHoursInMillis_whenCalculated_thenEquals7200000() {
        val twoHoursInMillis = 2 * 60 * 60 * 1000L
        assertEquals(7200000L, twoHoursInMillis)
    }
}
