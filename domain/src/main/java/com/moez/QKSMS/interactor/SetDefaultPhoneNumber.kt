/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.repository.ContactRepository

class SetDefaultPhoneNumber @Inject constructor(private val contactRepo: ContactRepository) :
    Interactor<SetDefaultPhoneNumber.Params>() {

    data class Params(val lookupKey: String, val phoneNumberId: Long)

    override fun buildObservable(params: Params): Flowable<*> {
        return Flowable.just(params).doOnNext { (lookupKey, phoneNumberId) ->
            contactRepo.setDefaultPhoneNumber(lookupKey, phoneNumberId)
        }
    }
}
