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

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import com.google.android.mms.MmsException
import com.google.android.mms.util_alt.SqliteWrapper
import com.klinker.android.send_message.Transaction
import dagger.android.AndroidInjection
import io.realm.Realm
import org.prauga.messages.interactor.SyncMessage
import org.prauga.messages.model.Message
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class MmsSentReceiver : BroadcastReceiver() {

    @Inject lateinit var syncMessage: SyncMessage

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        Timber.v("MMS sending result: $resultCode")
        val uri = Uri.parse(intent.getStringExtra(Transaction.EXTRA_CONTENT_URI))
        Timber.v(uri.toString())

        when (resultCode) {
            Activity.RESULT_OK -> {
                Timber.v("MMS has finished sending, marking it as so in the database")
                val values = ContentValues(1)
                values.put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_SENT)
                SqliteWrapper.update(context, context.contentResolver, uri, values, null, null)
            }

            else -> {
                Timber.v("MMS has failed to send, marking it as so in the database")
                try {
                    val messageId = ContentUris.parseId(uri)

                    val values = ContentValues(1)
                    values.put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_FAILED)
                    SqliteWrapper.update(context, context.contentResolver, Telephony.Mms.CONTENT_URI, values,
                            "${Telephony.Mms._ID} = ?", arrayOf(messageId.toString()))

                    // Update the error type directly in Realm since the PendingMessages table
                    // query doesn't work reliably (the message may not be in PendingMessages
                    // by the time this receiver runs)
                    updateErrorTypeInRealm(messageId, Telephony.MmsSms.ERR_TYPE_GENERIC_PERMANENT)

                } catch (e: MmsException) {
                    Timber.e(e, "Failed to mark MMS as failed")
                }
            }
        }

        val filePath = intent.getStringExtra(Transaction.EXTRA_FILE_PATH)
        Timber.v(filePath)
        filePath?.let { File(it).delete() }

        Uri.parse(intent.getStringExtra("content_uri"))?.let { contentUri ->
            val pendingResult = goAsync()
            syncMessage.execute(contentUri) { pendingResult.finish() }
        }
    }

    /**
     * Updates the error type for a failed MMS message directly in Realm.
     * This is more reliable than trying to update the PendingMessages table,
     * which may not contain the message by the time this receiver runs.
     */
    private fun updateErrorTypeInRealm(contentId: Long, errorType: Int) {
        try {
            Realm.getDefaultInstance().use { realm ->
                realm.executeTransaction { r ->
                    // Find the message by contentId (the original MMS ID)
                    val message = r.where(Message::class.java)
                        .equalTo("contentId", contentId)
                        .findFirst()

                    if (message != null) {
                        message.errorType = errorType
                        Timber.v("Updated errorType to $errorType for message contentId=$contentId")
                    } else {
                        Timber.w("Could not find message with contentId=$contentId to update errorType")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update errorType in Realm for contentId=$contentId")
        }
    }

}