package com.pdfapp.reader.ui.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.ui.components.AdBanner

data class ToolItem(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val route: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(onToolClick: (String) -> Unit) {
    val tools = listOf(
        ToolItem(R.string.tool_scan, R.string.tool_scan_desc, Icons.Default.CameraAlt, "scan", Color(0xFF32ADE6)),
        ToolItem(R.string.tool_image_to_pdf, R.string.tool_image_to_pdf_desc, Icons.Default.Image, "image_to_pdf", Color(0xFFFF9500)),
        ToolItem(R.string.tool_split, R.string.tool_split_desc, Icons.Default.ContentCut, "split", Color(0xFFFF3B30)),
        ToolItem(R.string.tool_lock, R.string.tool_lock_desc, Icons.Default.Lock, "lock", Color(0xFF5856D6)),
        ToolItem(R.string.tool_unlock, R.string.tool_unlock_desc, Icons.Default.LockOpen, "unlock", Color(0xFF34C759)),
        ToolItem(R.string.tool_pdf_to_image, R.string.tool_pdf_to_image_desc, Icons.Default.PhotoLibrary, "pdf_to_image", Color(0xFFFF2D55)),
        ToolItem(R.string.tool_extract_text, R.string.tool_extract_text_desc, Icons.Default.TextFields, "extract_text", Color(0xFF007AFF)),
        ToolItem(R.string.tool_merge, R.string.tool_merge_desc, Icons.Default.MergeType, "merge", Color(0xFF5E5CE6)),
        ToolItem(R.string.tool_compress, R.string.tool_compress_desc, Icons.Default.Compress, "compress", Color(0xFFFFCC00)),
        ToolItem(R.string.tool_page_manager, R.string.tool_page_manager_desc, Icons.Default.ViewModule, "page_manager", Color(0xFF30B0C7)),
    )

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
                TopAppBar(
                    title = { Text(stringResource(R.string.tools_title)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            },
            bottomBar = { AdBanner(Modifier.fillMaxWidth().padding(bottom = 110.dp)) }
        ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(tools) { tool ->
                ToolCard(
                    tool = tool,
                    onClick = { onToolClick(tool.route) }
                )
            }
        }
    }
    }
}

@Composable
private fun ToolCard(tool: ToolItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(tool.color.copy(alpha = 0.15f), shape = androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = tool.color
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(tool.titleRes),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(tool.descRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
