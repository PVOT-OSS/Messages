package org.prauga.messages.feature.blocking.manager

import io.reactivex.Observable
import io.reactivex.Single
import org.prauga.messages.common.base.QkViewContract

interface BlockingManagerView : QkViewContract<BlockingManagerState> {

    fun activityResumed(): Observable<*>
    fun qksmsClicked(): Observable<*>
    fun callBlockerClicked(): Observable<*>
    fun callControlClicked(): Observable<*>
    fun siaClicked(): Observable<*>

    fun showCopyDialog(manager: String): Single<Boolean>

}
