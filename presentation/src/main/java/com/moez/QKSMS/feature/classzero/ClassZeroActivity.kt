/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.classzero

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.prauga.pvot.designsystem.components.PvotDialog
import com.prauga.pvot.designsystem.components.PvotDialogAction
import com.prauga.pvot.designsystem.components.PvotDialogActionStyle
import com.prauga.pvot.designsystem.motion.PvotDurations
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import dagger.android.AndroidInjection
import javax.inject.Inject
import kotlinx.coroutines.delay
import org.prauga.messages.R
import org.prauga.messages.interactor.DeleteMessages
import org.prauga.messages.interactor.MarkRead
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
        val sender = message.address
        val body = message.body

        setContent {
            PvotAppTheme {
                var visible by rememberSaveable { mutableStateOf(true) }
                ClassZeroDialog(
                    visible = visible,
                    sender = sender,
                    body = body,
                    // A second tap while the dialog leaves must not act again
                    onSave = {
                        if (visible) {
                            markRead.execute(listOf(threadId))
                            visible = false
                        }
                    },
                    onDiscard = {
                        if (visible) {
                            deleteMessages.execute(DeleteMessages.Params(listOf(id), threadId))
                            visible = false
                        }
                    },
                )
                if (!visible) {
                    LaunchedEffect(Unit) {
                        // Lets the dialog play its exit before the window goes
                        delay(PvotDurations.Short.toLong())
                        finish()
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_ID = "id"
    }
}

@Composable
private fun ClassZeroDialog(
    visible: Boolean,
    sender: String,
    body: String,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    PvotDialog(
        visible = visible,
        onDismissRequest = {},
        title = sender,
        message = body,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_message_black_24dp),
                contentDescription = null,
            )
        },
        dismissOnOutsideTap = false,
    ) {
        PvotDialogAction(
            label = stringResource(R.string.button_save),
            onClick = onSave,
            style = PvotDialogActionStyle.Primary,
        )
        PvotDialogAction(label = stringResource(R.string.button_discard), onClick = onDiscard)
    }
}

@Preview
@Composable
private fun ClassZeroDialogPreview() {
    PvotAppTheme {
        ClassZeroDialog(
            visible = true,
            sender = "5557002",
            body = "Your balance is 20.00",
            onSave = {},
            onDiscard = {},
        )
    }
}
