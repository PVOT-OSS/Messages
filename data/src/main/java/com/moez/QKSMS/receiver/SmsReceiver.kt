/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.prauga.messages.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms
import dagger.android.AndroidInjection
import org.prauga.messages.interactor.ReceiveSms
import org.prauga.messages.repository.MessageRepository
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class SmsReceiver : BroadcastReceiver() {
    @Inject lateinit var messageRepo: MessageRepository
    @Inject lateinit var receiveSms: ReceiveSms

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        val pendingResult = goAsync()

        Sms.Intents.getMessagesFromIntent(intent)?.let { messages ->
            // reduce list of messages to single message and save in db
            val messageId = Single.just(messages)
                .observeOn(Schedulers.io())
                .map {
                    Timber.v("onReceive() new sms")

                    messageRepo.insertReceivedSms(
                        intent.extras?.getInt("subscription", -1) ?: -1,
                        messages[0].displayOriginatingAddress,
                        messages.mapNotNull { it.displayMessageBody }.reduce { body, new -> body + new },
                        messages[0].timestampMillis
                    ).id
                }
                .blockingGet()

            receiveSms.execute(messageId) { pendingResult.finish() }
        } ?: pendingResult.finish()
    }

}
