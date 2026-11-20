/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.main

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import com.prauga.pvot.designsystem.theme.PvotShapes
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.messages.R

private val filters =
    listOf(
        MessageCategory.ALL to R.string.main_filter_all,
        MessageCategory.UNREAD to R.string.main_filter_unread,
        MessageCategory.ARCHIVED to R.string.drawer_archived,
    )

@Composable
fun InboxFilters(
    active: MessageCategory,
    onSelect: (MessageCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = PvotSpacing.L),
        horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S),
    ) {
        filters.forEach { (category, label) ->
            InboxFilter(category == active, label) { onSelect(category) }
        }
    }
}

@Composable
private fun InboxFilter(selected: Boolean, @StringRes label: Int, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(label), fontWeight = FontWeight.SemiBold) },
        shape = PvotShapes.Pill,
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = colors.surfaceVariant,
                labelColor = colors.onSurface,
                selectedContainerColor = colors.primary,
                selectedLabelColor = colors.onPrimary,
            ),
        border =
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = colors.outlineVariant,
                selectedBorderColor = colors.primary,
            ),
    )
}

@Preview
@Composable
private fun InboxFiltersPreview() {
    PvotAppTheme { InboxFilters(active = MessageCategory.ALL, onSelect = {}) }
}
