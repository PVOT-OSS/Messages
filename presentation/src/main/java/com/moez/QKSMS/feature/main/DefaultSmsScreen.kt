/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prauga.pvot.coreui.PvotEmptyContent
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import org.prauga.messages.R

@Composable
fun DefaultSmsScreen(onChangeDefaultApp: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        PvotEmptyContent(
            message = stringResource(R.string.main_not_default_sms_message),
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_message_black_24dp),
                    contentDescription = null,
                )
            },
            actionLabel = stringResource(R.string.main_change_default_app),
            onAction = onChangeDefaultApp,
        )
    }
}

@Preview
@Composable
private fun DefaultSmsScreenPreview() {
    PvotAppTheme { DefaultSmsScreen(onChangeDefaultApp = {}) }
}
