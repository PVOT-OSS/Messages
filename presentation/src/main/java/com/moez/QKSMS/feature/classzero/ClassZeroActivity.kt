/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.classzero

import android.app.AlertDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.work.WorkManager
import dagger.android.AndroidInjection
import javax.inject.Inject
import org.prauga.messages.R
import org.prauga.messages.interactor.DeleteMessages
import org.prauga.messages.interactor.MarkRead
import org.prauga.messages.receiver.SmsReceiver
import org.prauga.messages.repository.MessageRepository

class ClassZeroActivity : AppCompatActivity() {

    @Inject lateinit var deleteMessages: DeleteMessages
    @Inject lateinit var markRead: MarkRead
    @Inject lateinit var messageRepo: MessageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        val id = intent.getLongExtra(EXTRA_ID, -1)
        val message = messageRepo.getMessage(id)
        if (message == null) {
            finish()
            return
        }
        val threadId = message.threadId

        AlertDialog.Builder(this)
            .setTitle(message.address)
            .setMessage(message.body)
            .setCancelable(false)
            .setPositiveButton(R.string.button_save) { _, _ ->
                decided(id)
                markRead.execute(listOf(threadId))
                finish()
            }
            .setNegativeButton(R.string.button_cancel) { _, _ ->
                decided(id)
                deleteMessages.execute(DeleteMessages.Params(listOf(id), threadId))
                finish()
            }
            .show()
    }

    private fun decided(id: Long) {
        WorkManager.getInstance(this).cancelUniqueWork(SmsReceiver.classZeroWork(id))
    }

    companion object {
        const val EXTRA_ID = "id"
    }
}
