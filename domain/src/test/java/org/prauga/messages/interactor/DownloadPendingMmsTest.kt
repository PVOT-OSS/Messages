/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.subscribers.TestSubscriber
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.prauga.messages.manager.MmsDownloadPolicy
import org.prauga.messages.manager.PermissionManager
import org.prauga.messages.model.Message
import org.prauga.messages.repository.MessageRepository

class DownloadPendingMmsTest {

    @Mock private lateinit var messageRepository: MessageRepository

    @Mock private lateinit var permissionManager: PermissionManager

    @Mock private lateinit var mmsDownloadPolicy: MmsDownloadPolicy

    @Mock private lateinit var first: Message

    @Mock private lateinit var second: Message

    private lateinit var downloadPendingMms: DownloadPendingMms

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        downloadPendingMms =
            DownloadPendingMms(messageRepository, mmsDownloadPolicy, permissionManager)
        `when`(mmsDownloadPolicy.shouldAutoDownload(anyInt(), any())).thenReturn(true)
    }

    @Test
    fun givenPendingDownloads_whenBuildObservable_thenDownloadsEachOfThem() {
        `when`(permissionManager.isDefaultSms()).thenReturn(true)
        `when`(messageRepository.getPendingMmsDownloads(anyLong()))
            .thenReturn(listOf(first, second))

        val testSubscriber = TestSubscriber<Any>()
        downloadPendingMms.buildObservable(Unit).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(messageRepository).downloadMms(first, true)
        verify(messageRepository).downloadMms(second, true)
    }

    @Test
    fun givenRecentWindow_whenBuildObservable_thenOnlyLooksAtTheLastWeek() {
        var since = 0L
        `when`(permissionManager.isDefaultSms()).thenReturn(true)
        `when`(messageRepository.getPendingMmsDownloads(anyLong())).thenAnswer {
            since = it.getArgument(0)
            emptyList<Message>()
        }
        val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L

        downloadPendingMms.buildObservable(Unit).subscribe(TestSubscriber<Any>())

        assertTrue(since in (weekAgo - 1000)..(weekAgo + 1000))
    }

    @Test
    fun givenPolicyAgainstAutoDownload_whenBuildObservable_thenLeavesThemForTheUser() {
        `when`(permissionManager.isDefaultSms()).thenReturn(true)
        `when`(messageRepository.getPendingMmsDownloads(anyLong())).thenReturn(listOf(first))
        `when`(mmsDownloadPolicy.shouldAutoDownload(anyInt(), any())).thenReturn(false)

        downloadPendingMms.buildObservable(Unit).subscribe(TestSubscriber<Any>())

        verify(messageRepository, never()).downloadMms(first, true)
    }

    @Test
    fun givenNotDefaultSmsApp_whenBuildObservable_thenDoesNotDownload() {
        `when`(permissionManager.isDefaultSms()).thenReturn(false)

        val testSubscriber = TestSubscriber<Any>()
        downloadPendingMms.buildObservable(Unit).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepository, never()).getPendingMmsDownloads(anyLong())
        verifyNoInteractions(first, second)
    }
}
