/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.conversationinfo

import io.realm.RealmList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.prauga.messages.feature.conversationinfo.ConversationInfoItem.ConversationInfoMedia
import org.prauga.messages.feature.conversationinfo.ConversationInfoItem.ConversationInfoSettings
import org.prauga.messages.model.MmsPart

class ConversationInfoItemsTest {

    private class Media(override val size: Int) : AbstractList<MmsPart>() {
        val read = mutableListOf<Int>()
        private val part = mock(MmsPart::class.java)

        override fun get(index: Int): MmsPart {
            read += index
            return part
        }
    }

    private val settings =
        ConversationInfoSettings("", RealmList(), archived = false, blocked = false)

    @Test
    fun givenAHugeMediaList_whenShowingInfo_thenOnlyTheItemsAskedForAreRead() {
        val media = Media(1_000_000_000)
        val items = ConversationInfoItems(listOf(settings), media)

        assertEquals(1_000_000_001, items.size)
        assertSame(settings, items[0])
        assertTrue(items[5] is ConversationInfoMedia)
        assertEquals(listOf(4), media.read)
    }

    @Test
    fun givenTwoListsOfTheSameItems_whenCompared_thenTheyDifferWithoutReadingThem() {
        val media = Media(1_000_000_000)

        assertNotEquals(
            ConversationInfoItems(listOf(settings), media),
            ConversationInfoItems(listOf(settings), media),
        )
        assertTrue(media.read.isEmpty())
    }
}
