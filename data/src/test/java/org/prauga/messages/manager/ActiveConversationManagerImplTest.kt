/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.manager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ActiveConversationManagerImplTest {

    private lateinit var activeConversationManager: ActiveConversationManagerImpl

    @Before
    fun setUp() {
        activeConversationManager = ActiveConversationManagerImpl()
    }

    @Test
    fun givenNoActiveConversation_whenGetActiveConversation_thenReturnsNull() {
        val result = activeConversationManager.getActiveConversation()

        assertNull(result)
    }

    @Test
    fun givenThreadId_whenSetActiveConversation_thenReturnsThreadId() {
        val threadId = 100L

        activeConversationManager.setActiveConversation(threadId)

        assertEquals(threadId, activeConversationManager.getActiveConversation())
    }

    @Test
    fun givenNullThreadId_whenSetActiveConversation_thenReturnsNull() {
        activeConversationManager.setActiveConversation(100L)
        activeConversationManager.setActiveConversation(null)

        assertNull(activeConversationManager.getActiveConversation())
    }

    @Test
    fun givenZeroThreadId_whenSetActiveConversation_thenReturnsZero() {
        activeConversationManager.setActiveConversation(0L)

        assertEquals(0L, activeConversationManager.getActiveConversation())
    }

    @Test
    fun givenMultipleChanges_whenSetActiveConversation_thenReturnsLastValue() {
        activeConversationManager.setActiveConversation(100L)
        activeConversationManager.setActiveConversation(200L)
        activeConversationManager.setActiveConversation(300L)

        assertEquals(300L, activeConversationManager.getActiveConversation())
    }

    @Test
    fun givenLargeThreadId_whenSetActiveConversation_thenHandlesCorrectly() {
        val threadId = Long.MAX_VALUE

        activeConversationManager.setActiveConversation(threadId)

        assertEquals(Long.MAX_VALUE, activeConversationManager.getActiveConversation())
    }

    @Test
    fun givenNegativeThreadId_whenSetActiveConversation_thenHandlesCorrectly() {
        val threadId = -1L

        activeConversationManager.setActiveConversation(threadId)

        assertEquals(-1L, activeConversationManager.getActiveConversation())
    }

    @Test
    fun givenActiveConversation_whenSetToNull_thenClearsActiveConversation() {
        activeConversationManager.setActiveConversation(100L)
        assertEquals(100L, activeConversationManager.getActiveConversation())

        activeConversationManager.setActiveConversation(null)
        assertNull(activeConversationManager.getActiveConversation())
    }

    @Test
    fun givenMultipleNullSets_whenGetActiveConversation_thenReturnsNull() {
        activeConversationManager.setActiveConversation(null)
        activeConversationManager.setActiveConversation(null)
        activeConversationManager.setActiveConversation(null)

        assertNull(activeConversationManager.getActiveConversation())
    }

    @Test
    fun givenAlternatingValues_whenGetActiveConversation_thenReturnsLastValue() {
        activeConversationManager.setActiveConversation(100L)
        activeConversationManager.setActiveConversation(null)
        activeConversationManager.setActiveConversation(200L)
        activeConversationManager.setActiveConversation(null)
        activeConversationManager.setActiveConversation(300L)

        assertEquals(300L, activeConversationManager.getActiveConversation())
    }
}
