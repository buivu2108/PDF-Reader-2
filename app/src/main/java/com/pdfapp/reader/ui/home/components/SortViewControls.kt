package com.pdfapp.reader.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.domain.model.SortOption
import com.pdfapp.reader.domain.model.ViewMode

/**
 * Control row for the Home screen toolbar:
 * - Left side: sort dropdown (6 options)
 * - Right side: toggle between list and grid view
 */
@Composable
fun SortViewControls(
    sortOption: SortOption,
    viewMode: ViewMode,
    onSortChange: (SortOption) -> Unit,
    onViewModeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort dropdown trigger
        Box {
            TextButton(
                onClick = { dropdownExpanded = true }
            ) {
                Text(
                    text = sortOption.toDisplayLabel(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.cd_sort_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                SortOption.entries.forEach { option ->
                    val (icon, iconTint) = option.toIconData()
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        text = {
                            Text(
                                text = option.toDisplayLabel(),
                                color = if (option == sortOption)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onSortChange(option)
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

        // View mode toggle
        IconButton(onClick = onViewModeToggle) {
            Icon(
                imageVector = if (viewMode == ViewMode.LIST) Icons.Default.Menu else Icons.AutoMirrored.Filled.List,
                contentDescription = stringResource(if (viewMode == ViewMode.LIST) R.string.cd_switch_to_grid else R.string.cd_switch_to_list),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Maps a SortOption to its localized display label via string resources. */
@Composable
private fun SortOption.toDisplayLabel(): String = when (this) {
    SortOption.NAME_ASC -> stringResource(R.string.home_sort_name_asc)
    SortOption.NAME_DESC -> stringResource(R.string.home_sort_name_desc)
    SortOption.DATE_NEWEST -> stringResource(R.string.home_sort_date_newest)
    SortOption.DATE_OLDEST -> stringResource(R.string.home_sort_date_oldest)
    SortOption.SIZE_LARGEST -> stringResource(R.string.home_sort_size_largest)
    SortOption.SIZE_SMALLEST -> stringResource(R.string.home_sort_size_smallest)
}

/** Returns the icon and color pair for each sort option */
@Composable
private fun SortOption.toIconData(): Pair<androidx.compose.ui.graphics.vector.ImageVector, androidx.compose.ui.graphics.Color> = when (this) {
    SortOption.NAME_ASC -> Icons.Default.SortByAlpha to androidx.compose.ui.graphics.Color(0xFF2196F3) // Blue
    SortOption.NAME_DESC -> Icons.Default.SortByAlpha to androidx.compose.ui.graphics.Color(0xFF03A9F4) // Light Blue
    SortOption.DATE_NEWEST -> Icons.Default.DateRange to androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
    SortOption.DATE_OLDEST -> Icons.Default.DateRange to androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
    SortOption.SIZE_LARGEST -> Icons.Default.Menu to androidx.compose.ui.graphics.Color(0xFF9C27B0) // Purple
    SortOption.SIZE_SMALLEST -> Icons.Default.Menu to androidx.compose.ui.graphics.Color(0xFFE91E63) // Pink
}
