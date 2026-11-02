/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class InteractorTest {

    private class Echo(private val source: (Int) -> Flowable<Int>) : Interactor<Int>() {
        override fun buildObservable(params: Int) = source(params)
    }

    @Before
    fun setUp() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    @After
    fun tearDown() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun givenManyRunsThatFinish_whenExecuted_thenNoneAreKept() {
        val interactor = Echo { Flowable.just(it) }
        var completed = 0

        repeat(10_000) { interactor.execute(it) { completed++ } }

        assertEquals(10_000, completed)
        assertEquals(0, interactor.pendingRuns)
    }

    @Test
    fun givenRunsThatFail_whenExecuted_thenNoneAreKept() {
        val interactor = Echo { Flowable.error(IllegalStateException("boom $it")) }

        repeat(100) { interactor.execute(it) }

        assertEquals(0, interactor.pendingRuns)
    }

    @Test
    fun givenARunStillGoing_whenDisposed_thenItIsCancelled() {
        val upstream = PublishSubject.create<Int>()
        val interactor = Echo { upstream.toFlowable(io.reactivex.BackpressureStrategy.LATEST) }

        interactor.execute(1)
        assertEquals(1, interactor.pendingRuns)

        interactor.dispose()

        assertFalse(upstream.hasObservers())
    }
}
