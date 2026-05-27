package com.pdfapp.reader.ui.viewer.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CropPortrait
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.domain.model.PageMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerTopBar(
    visible: Boolean,
    fileName: String,
    isNightMode: Boolean,
    pageMode: PageMode = PageMode.CONTINUOUS,
    isBookmarked: Boolean = false,
    isSearchActive: Boolean = false,
    searchQuery: String = "",
    searchResultCount: Int = 0,
    searchCurrentIndex: Int = 0,
    onSearchQueryChange: (String) -> Unit = {},
    onSearchNext: () -> Unit = {},
    onSearchPrev: () -> Unit = {},
    onSearchClose: () -> Unit = {},
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onNightModeToggle: () -> Unit,
    onPageModeToggle: () -> Unit,
    onToggleBookmark: () -> Unit = {},
    onShowBookmarks: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it })
    ) {
        TopAppBar(
            windowInsets = WindowInsets(0, 0, 0, 0),
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    AnimatedVisibility(
                        visible = !isSearchActive,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Text(
                            text = fileName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    AnimatedVisibility(
                        visible = isSearchActive,
                        enter = expandHorizontally(
                            expandFrom = Alignment.End,
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(200)),
                        exit = shrinkHorizontally(
                            shrinkTowards = Alignment.End,
                            animationSpec = tween(300, easing = FastOutLinearInEasing)
                        ) + fadeOut(animationSpec = tween(150)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearchNext() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            decorationBox = { innerTextField ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = stringResource(R.string.viewer_search_hint),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            }
                        )
                    }
                }
            },
            navigationIcon = {
                AnimatedVisibility(
                    visible = !isSearchActive,
                    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.cd_back), modifier = Modifier.size(32.dp))
                    }
                }
            },
            actions = {
                AnimatedVisibility(
                    visible = !isSearchActive,
                    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onSearch) {
                            Icon(Icons.Default.Search, tint = MaterialTheme.colorScheme.primary, contentDescription = stringResource(R.string.cd_search))
                        }
                        IconButton(onClick = onNightModeToggle) {
                            if (isNightMode) {
                                Icon(Icons.Default.Star, tint = Color(0xFFFFB300), contentDescription = stringResource(R.string.cd_light_mode))
                            } else {
                                Icon(Icons.Default.Done, tint = MaterialTheme.colorScheme.primary, contentDescription = stringResource(R.string.cd_night_mode))
                            }
                        }
                        IconButton(onClick = onToggleBookmark) {
                            if (isBookmarked) {
                                Icon(Icons.Default.Bookmark, tint = MaterialTheme.colorScheme.primary, contentDescription = "Remove bookmark")
                            } else {
                                Icon(Icons.Outlined.BookmarkBorder, tint = MaterialTheme.colorScheme.onSurfaceVariant, contentDescription = "Add bookmark")
                            }
                        }
                        IconButton(onClick = onShowBookmarks) {
                            Icon(Icons.AutoMirrored.Filled.List, tint = MaterialTheme.colorScheme.tertiary, contentDescription = "Show bookmarks")
                        }
                        IconButton(onClick = onPageModeToggle) {
                            if (pageMode == PageMode.CONTINUOUS) {
                                Icon(Icons.Outlined.CropPortrait, tint = MaterialTheme.colorScheme.secondary, contentDescription = stringResource(R.string.cd_page_mode))
                            } else {
                                Icon(Icons.Default.ViewDay, tint = MaterialTheme.colorScheme.secondary, contentDescription = stringResource(R.string.cd_page_mode))
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchResultCount > 0) {
                            Text(
                                text = "${searchCurrentIndex + 1}/$searchResultCount",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = onSearchPrev,
                            modifier = Modifier.width(36.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.cd_previous_result))
                        }
                        IconButton(
                            onClick = onSearchNext,
                            modifier = Modifier.width(36.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.cd_next_result))
                        }
                        IconButton(onClick = onSearchClose) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close_search))
                        }
                    }
                }
            }
        )
    }
}
