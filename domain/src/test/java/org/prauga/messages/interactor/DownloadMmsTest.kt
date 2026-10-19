/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.prauga.messages.model.Message
import org.prauga.messages.repository.MessageRepository

class DownloadMmsTest {

    @Mock private lateinit var messageRepository: MessageRepository

    @Mock private lateinit var message: Message

    private lateinit var downloadMms: DownloadMms

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        downloadMms = DownloadMms(messageRepository)
    }

    @Test
    fun givenPendingMmsDownload_whenBuildObservable_thenStartsDownload() {
        `when`(messageRepository.getMessage(1L)).thenReturn(message)
        `when`(message.isPendingMmsDownload()).thenReturn(true)

        val testSubscriber = TestSubscriber<Message>()
        downloadMms.buildObservable(1L).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(messageRepository).downloadMms(message)
    }

    @Test
    fun givenAlreadyDownloadedMms_whenBuildObservable_thenDoesNothing() {
        `when`(messageRepository.getMessage(1L)).thenReturn(message)
        `when`(message.isPendingMmsDownload()).thenReturn(false)

        val testSubscriber = TestSubscriber<Message>()
        downloadMms.buildObservable(1L).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepository, never()).downloadMms(message)
    }

    @Test
    fun givenExpiredMms_whenBuildObservable_thenDoesNotRetry() {
        `when`(messageRepository.getMessage(1L)).thenReturn(message)
        `when`(message.isPendingMmsDownload()).thenReturn(true)
        `when`(message.isExpiredMmsDownload()).thenReturn(true)

        val testSubscriber = TestSubscriber<Message>()
        downloadMms.buildObservable(1L).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepository, never()).downloadMms(message)
    }

    @Test
    fun givenMissingMessage_whenBuildObservable_thenCompletesWithoutDownloading() {
        `when`(messageRepository.getMessage(1L)).thenReturn(null)

        val testSubscriber = TestSubscriber<Message>()
        downloadMms.buildObservable(1L).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoValues()
    }
}
