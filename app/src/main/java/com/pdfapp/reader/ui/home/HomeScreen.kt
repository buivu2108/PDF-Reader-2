package com.pdfapp.reader.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.domain.model.PdfFileInfo
import com.pdfapp.reader.domain.model.ViewMode
import com.pdfapp.reader.ui.home.components.EmptyStateView
import com.pdfapp.reader.ui.home.components.FileGridItem
import com.pdfapp.reader.ui.home.components.FileInfoDialog
import com.pdfapp.reader.ui.home.components.FileListItem
import com.pdfapp.reader.ui.home.components.FileOptionsSheet
import com.pdfapp.reader.ui.home.components.FolderGroupHeader
import com.pdfapp.reader.ui.home.components.MultiSelectTopBar
import com.pdfapp.reader.ui.home.components.SortViewControls
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.isSystemInDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onFileClick: (PdfFileInfo) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Re-scan files when returning to home (e.g. after Save As creates a new PDF)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.scanFiles()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var sheetFile by remember { mutableStateOf<PdfFileInfo?>(null) }
    var infoFile by remember { mutableStateOf<PdfFileInfo?>(null) }
    val expandedFolders = remember { mutableStateMapOf<String, Boolean>() }

    val isDark = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isDark) {
                    SolidColor(Color(0xFF40226D))
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFDFCAFF), // Vibrant pastel purple
                            Color(0xFFF2EAFC), // Mid light purple
                            MaterialTheme.colorScheme.surface
                        )
                    )
                }
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (state.isMultiSelectMode) {
                    MultiSelectTopBar(
                        selectedCount = state.selectedFiles.size,
                        onClose = viewModel::clearSelection,
                        onSelectAll = viewModel::selectAll,
                        onShare = { /* TODO */ },
                        onDelete = viewModel::deleteSelectedFiles
                    )
                } else {
                    TopAppBar(
                        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent
                        ),
                        title = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = !state.isSearchActive,
                                    enter = androidx.compose.animation.fadeIn(),
                                    exit = androidx.compose.animation.fadeOut(),
                                    modifier = Modifier.align(Alignment.CenterStart)
                                ) {
                                    Text(stringResource(R.string.home_title))
                                }

                                androidx.compose.animation.AnimatedVisibility(
                                    visible = state.isSearchActive,
                                    enter = androidx.compose.animation.expandHorizontally(
                                        expandFrom = Alignment.End,
                                        animationSpec = androidx.compose.animation.core.tween(350, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                    ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200)),
                                    exit = androidx.compose.animation.shrinkHorizontally(
                                        shrinkTowards = Alignment.End,
                                        animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutLinearInEasing)
                                    ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(150))
                                ) {
                                    BasicTextField(
                                        value = state.searchQuery,
                                        onValueChange = viewModel::setSearchQuery,
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(41.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant,
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                                            )
                                            .padding(horizontal = 16.dp),
                                        decorationBox = { innerTextField ->
                                            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
                                                if (state.searchQuery.isEmpty()) {
                                                    Text(
                                                        text = stringResource(R.string.home_search_hint),
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        }
                                    )
                                }
                            }
                        },
                        actions = {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = !state.isSearchActive,
                                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(expandFrom = Alignment.Start),
                                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally(shrinkTowards = Alignment.Start)
                            ) {
                                SortViewControls(
                                    sortOption = state.sortOption,
                                    viewMode = state.viewMode,
                                    onSortChange = viewModel::setSortOption,
                                    onViewModeToggle = viewModel::toggleViewMode
                                )
                            }
                            IconButton(onClick = viewModel::toggleSearchActive) {
                                Icon(
                                    imageVector = if (state.isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = stringResource(R.string.cd_search)
                                )
                            }
                        }
                    )
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                if (!state.isMultiSelectMode) {
                // Vibrant Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val allSelected = !state.showFavoritesOnly
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                            .background(
                                if (allSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .clickable { if (!allSelected) viewModel.toggleFavoritesFilter() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All Files",
                            color = if (allSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    val favSelected = state.showFavoritesOnly
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                            .background(
                                if (favSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .clickable { if (!favSelected) viewModel.toggleFavoritesFilter() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Favorites",
                            color = if (favSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = viewModel::scanFiles,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isLoading && state.files.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.filteredFiles.isEmpty() -> EmptyStateView()
                    state.viewMode == ViewMode.GRID -> FileGridContent(
                        state, expandedFolders, viewModel, onFileClick
                    ) { sheetFile = it }
                    else -> FileListContent(
                        state, expandedFolders, viewModel, onFileClick
                    ) { sheetFile = it }
                }
            }
        }
    }

    sheetFile?.let { file ->
        FileOptionsSheet(
            file = file,
            onDismiss = { sheetFile = null },
            onOpen = { onFileClick(file); sheetFile = null },
            onShare = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(file.uri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, null))
                sheetFile = null
            },
            onRename = { sheetFile = null },
            onDelete = { viewModel.deleteFile(file.uri); sheetFile = null },
            onInfo = { infoFile = file; sheetFile = null },
            onFavorite = { viewModel.toggleFavorite(file.uri) }
        )
    }

    infoFile?.let { file ->
        FileInfoDialog(
            file = file,
            onDismiss = { infoFile = null }
        )
    }
    }
}

@Composable
private fun FileListContent(
    state: HomeUiState,
    expandedFolders: MutableMap<String, Boolean>,
    viewModel: HomeViewModel,
    onFileClick: (PdfFileInfo) -> Unit,
    onMoreClick: (PdfFileInfo) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 130.dp)) {
        state.filteredFiles.forEach { (folder, files) ->
            val expanded = expandedFolders.getOrDefault(folder, true)
            item(key = "folder_$folder") {
                FolderGroupHeader(
                    folderName = folder,
                    fileCount = files.size,
                    isExpanded = expanded,
                    onToggle = { expandedFolders[folder] = !expanded }
                )
            }
            if (expanded) {
                items(files, key = { it.uri }) { file ->
                    FileListItem(
                        file = file,
                        isSelected = file.uri in state.selectedFiles,
                        onItemClick = {
                            if (state.isMultiSelectMode) viewModel.toggleFileSelection(file.uri)
                            else onFileClick(file)
                        },
                        onMoreClick = { onMoreClick(file) },
                        onLongPress = { viewModel.toggleFileSelection(file.uri) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileGridContent(
    state: HomeUiState,
    expandedFolders: MutableMap<String, Boolean>,
    viewModel: HomeViewModel,
    onFileClick: (PdfFileInfo) -> Unit,
    onMoreClick: (PdfFileInfo) -> Unit
) {
    val allFiles = state.filteredFiles.values.flatten()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 130.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(allFiles, key = { it.uri }) { file ->
            FileGridItem(
                file = file,
                isSelected = file.uri in state.selectedFiles,
                onItemClick = {
                    if (state.isMultiSelectMode) viewModel.toggleFileSelection(file.uri)
                    else onFileClick(file)
                },
                onLongPress = { viewModel.toggleFileSelection(file.uri) },
                onFavoriteClick = { viewModel.toggleFavorite(file.uri) }
            )
        }
    }
}
