/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.prauga.pvot.designsystem.components.button.PvotIconButton
import com.prauga.pvot.designsystem.components.button.PvotIconButtonDefaults
import com.prauga.pvot.designsystem.components.textfield.PvotTextField
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import com.prauga.pvot.designsystem.theme.PvotShapes
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.messages.R

@Composable
fun InboxSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    showSearch: Boolean,
    showCompose: Boolean,
    onCompose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(PvotSpacing.M),
        horizontalArrangement = Arrangement.spacedBy(PvotSpacing.M),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showSearch) {
            PvotTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = stringResource(R.string.title_conversations),
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_search), contentDescription = null)
                },
                trailingIcon =
                    if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = onClear) {
                                Icon(
                                    painterResource(R.drawable.ic_close_black_24dp),
                                    contentDescription = stringResource(R.string.button_clear),
                                )
                            }
                        }
                    } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                shape = PvotShapes.Pill,
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (showCompose) {
            PvotIconButton(
                icon = painterResource(R.drawable.ic_compose),
                contentDescription = stringResource(R.string.main_fab_cd),
                onClick = onCompose,
                size = PvotIconButtonDefaults.LargeSize,
            )
        }
    }
}

@Preview
@Composable
private fun InboxSearchBarPreview() {
    PvotAppTheme {
        InboxSearchBar(
            query = "",
            onQueryChange = {},
            onClear = {},
            onSearch = {},
            showSearch = true,
            showCompose = true,
            onCompose = {},
        )
    }
}
