/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.main

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.prauga.pvot.designsystem.components.PvotMenu
import com.prauga.pvot.designsystem.components.PvotMenuItem
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.messages.R

@Composable
fun InboxHeader(showMenu: Boolean, onNavigate: (NavItem) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier.fillMaxWidth().padding(horizontal = PvotSpacing.L, vertical = PvotSpacing.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (showMenu) {
            PvotMenu(
                entries =
                    listOf(
                        PvotMenuItem(
                            label = stringResource(R.string.backup_title),
                            onClick = { onNavigate(NavItem.BACKUP) },
                            icon = R.drawable.ic_import_export_black_24dp,
                        ),
                        PvotMenuItem(
                            label = stringResource(R.string.drawer_scheduled),
                            onClick = { onNavigate(NavItem.SCHEDULED) },
                            icon = R.drawable.ic_event_black_24dp,
                        ),
                        PvotMenuItem(
                            label = stringResource(R.string.drawer_blocking),
                            onClick = { onNavigate(NavItem.BLOCKING) },
                            icon = R.drawable.ic_block_white_24dp,
                        ),
                        PvotMenuItem(
                            label = stringResource(R.string.drawer_settings),
                            onClick = { onNavigate(NavItem.SETTINGS) },
                            icon = R.drawable.ic_settings_black_24dp,
                        ),
                    ),
                contentDescription = stringResource(R.string.main_more_cd),
                icon = R.drawable.ic_filter,
            )
        }
    }
}

@Preview
@Composable
private fun InboxHeaderPreview() {
    PvotAppTheme { InboxHeader(showMenu = true, onNavigate = {}) }
}
