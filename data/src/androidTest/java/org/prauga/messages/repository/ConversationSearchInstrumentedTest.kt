/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import android.content.Context
import android.os.Debug
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.realm.Realm
import io.realm.RealmList
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.prauga.messages.filter.ContactFilter
import org.prauga.messages.filter.ConversationFilter
import org.prauga.messages.filter.PhoneNumberFilter
import org.prauga.messages.filter.RecipientFilter
import org.prauga.messages.mapper.CursorToConversation
import org.prauga.messages.mapper.CursorToRecipient
import org.prauga.messages.model.Conversation
import org.prauga.messages.model.Message
import org.prauga.messages.model.MmsPart
import org.prauga.messages.model.Recipient
import org.prauga.messages.model.SearchItem
import org.prauga.messages.testutil.RealmTestHelper
import org.prauga.messages.util.PhoneNumberUtils

@RunWith(AndroidJUnit4::class)
class ConversationSearchInstrumentedTest {

    private lateinit var realm: Realm
    private lateinit var repository: ConversationRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        RealmTestHelper.init(context)
        realm = RealmTestHelper.getRealm()
        val phoneNumberFilter = PhoneNumberFilter(PhoneNumberUtils(context))
        repository =
            ConversationRepositoryImpl(
                context,
                ConversationFilter(
                    RecipientFilter(ContactFilter(phoneNumberFilter), phoneNumberFilter)
                ),
                mock(CursorToConversation::class.java),
                mock(CursorToRecipient::class.java),
                PhoneNumberUtils(context),
            )
    }

    @After
    fun tearDown() {
        realm.close()
        RealmTestHelper.tearDown()
    }

    private fun sms(id: Long, threadId: Long, body: String) =
        Message().apply {
            this.id = id
            this.threadId = threadId
            type = "sms"
            this.body = body
            date = id
        }

    /** A layout part first, as carriers send them, then the text */
    private fun mms(id: Long, threadId: Long, text: String) =
        Message().apply {
            this.id = id
            this.threadId = threadId
            type = "mms"
            date = id
            parts =
                RealmList(
                    MmsPart().apply {
                        this.id = id * 10
                        messageId = id
                        type = "application/smil"
                        this.text = LAYOUT
                    },
                    MmsPart().apply {
                        this.id = id * 10 + 1
                        messageId = id
                        type = "text/plain"
                        this.text = text
                    },
                )
        }

    private fun Realm.conversation(
        id: Long,
        address: String,
        last: Message,
        blocked: Boolean = false,
    ) =
        insert(
            Conversation(
                id = id,
                recipients = RealmList(Recipient(id = id, address = address)),
                lastMessage = where(Message::class.java).equalTo("id", last.id).findFirst(),
                blocked = blocked,
            )
        )

    private fun bytesAllocated() = Debug.getRuntimeStat("art.gc.bytes-allocated").toLong()

    @Test
    fun givenHugeInboxMatchingTheQuery_whenSearching_thenResultsAndMemoryStayBounded() {
        val count = 50_000
        realm.executeTransaction { r ->
            r.insert((1..count).map { sms(it.toLong(), 1, "hello number $it") })
            r.conversation(1, "5550001", sms(count.toLong(), 1, ""))
        }
        val openInstances = Realm.getLocalInstanceCount(realm.configuration)

        val before = bytesAllocated()
        val results = repository.searchConversationsGrouped("hello")
        val allocated = bytesAllocated() - before

        val messages = results.filterIsInstance<SearchItem.Message>()
        assertThat(messages).hasSize(ConversationRepositoryImpl.MAX_SEARCH_MESSAGES.toInt())
        assertThat(messages.first().messageId).isEqualTo(count.toLong())
        assertThat(messages.map { it.timestamp }).isInOrder(Comparator.reverseOrder<Long>())
        assertThat(Realm.getLocalInstanceCount(realm.configuration)).isEqualTo(openInstances)
        assertThat(allocated).isLessThan(ALLOCATION_LIMIT)
    }

    @Test
    fun givenMatchesInSeveralConversations_whenSearching_thenGroupedWithBlockedOnesLeftOut() {
        realm.executeTransaction { r ->
            r.insert(listOf(sms(1, 1, "lunch?"), sms(2, 1, "lunch at 1"), sms(3, 2, "lunch!")))
            r.insert(sms(4, 3, "lunch from a blocked number"))
            r.conversation(1, "5550001", sms(2, 1, ""))
            r.conversation(2, "5550002", sms(3, 2, ""))
            r.conversation(3, "5550003", sms(4, 3, ""), blocked = true)
        }

        val results = repository.searchConversationsGrouped("LUNCH")

        assertThat(results.filterIsInstance<SearchItem.Header>().map { it.conversationId })
            .containsExactly(1L, 2L)
            .inOrder()
        assertThat(results.filterIsInstance<SearchItem.Message>().map { it.messageId })
            .containsExactly(2L, 1L, 3L)
            .inOrder()
    }

    @Test
    fun givenMms_whenSearchingAWordOnlyInItsLayout_thenNothingMatches() {
        realm.executeTransaction { r ->
            r.insert(mms(1, 1, "see you at noon"))
            r.conversation(1, "5550001", mms(1, 1, ""))
        }

        assertThat(repository.searchConversationsGrouped("layout")).isEmpty()
    }

    @Test
    fun givenMms_whenSearchingItsText_thenTheTextIsShownNotTheLayout() {
        realm.executeTransaction { r ->
            r.insert(mms(1, 1, "see you at noon"))
            r.conversation(1, "5550001", mms(1, 1, ""))
        }

        val messages =
            repository.searchConversationsGrouped("NOON").filterIsInstance<SearchItem.Message>()

        assertThat(messages.map { it.body }).containsExactly("see you at noon")
    }

    @Test
    fun givenAQueryWithQuotesAndBackslashes_whenSearching_thenItIsMatchedLiterally() {
        val text = """it's a \\ path"""
        realm.executeTransaction { r ->
            r.insert(mms(1, 1, text))
            r.conversation(1, "5550001", mms(1, 1, ""))
        }

        val results = repository.searchConversationsGrouped("""it's a \\""")

        val messages = results.filterIsInstance<SearchItem.Message>()

        assertThat(messages.map { it.body }).containsExactly(text)
    }

    @Test
    fun givenManyMmsWhoseLayoutHasTheWord_whenSearching_thenAnOlderTextMatchIsStillFound() {
        val count = ConversationRepositoryImpl.MAX_SEARCH_MESSAGES + 100
        realm.executeTransaction { r ->
            r.insert(sms(1, 1, "an image of the cat"))
            r.insert((2..count).map { mms(it, 1, "photo $it") })
            r.conversation(1, "5550001", sms(count, 1, ""))
        }

        val messages =
            repository.searchConversationsGrouped("image").filterIsInstance<SearchItem.Message>()

        assertThat(messages.map { it.messageId }).containsExactly(1L)
    }

    private companion object {
        const val ALLOCATION_LIMIT = 4_000_000L
        const val LAYOUT =
            "<smil><head><layout><root-layout/><region id=\"Image\" top=\"0%\" left=\"0%\" " +
                "height=\"80%\"/></layout></head><body><par dur=\"5000ms\">" +
                "<img src=\"image.jpg\" region=\"Image\"/></par></body></smil>"
    }
}
