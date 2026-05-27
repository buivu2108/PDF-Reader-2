package com.pdfapp.reader.ui.editmode.fillsign

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Approval
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.data.local.entity.SignatureEntity

/** Predefined stamp labels available in the stamp picker. */
val STAMP_OPTIONS = listOf("APPROVED", "DRAFT", "CONFIDENTIAL", "COPY", "VOID", "ORIGINAL")

/**
 * Fill & Sign toolbar with horizontally scrollable tool buttons and collapsible panels.
 * Signature gallery shows when Signature tool is active; stamp picker shows when Stamp is active.
 */
@Composable
fun FillSignToolbar(
    activeTool: FillSignTool,
    savedSignatures: List<SignatureEntity>,
    onToolSelected: (FillSignTool) -> Unit,
    onAddSignature: () -> Unit,
    onPlaceSignature: (SignatureEntity) -> Unit,
    onDeleteSignature: (String) -> Unit,
    onPickImage: () -> Unit,
    onPlaceDateStamp: () -> Unit,
    onAddPage: () -> Unit,
    onPlaceStamp: (String) -> Unit,
    onLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // -- Signature gallery panel (visible when Signature tool active) --
        AnimatedVisibility(
            visible = activeTool == FillSignTool.SIGNATURE,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            SignatureGalleryPanel(
                signatures = savedSignatures,
                onAdd = onAddSignature,
                onPlace = onPlaceSignature,
                onDelete = onDeleteSignature
            )
        }

        // -- Stamp picker panel (visible when Stamp tool active) --
        AnimatedVisibility(
            visible = activeTool == FillSignTool.STAMP,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            StampPickerPanel(onPlaceStamp = onPlaceStamp)
        }

        // -- Horizontally scrollable tool buttons row --
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FillSignToolButton(
                    icon = { Icon(painterResource(R.drawable.ic_signature), contentDescription = null, modifier = Modifier.size(24.dp)) },
                    label = stringResource(R.string.fillsign_tool_signature),
                    isActive = activeTool == FillSignTool.SIGNATURE,
                    onClick = { onToolSelected(FillSignTool.SIGNATURE) }
                )
                FillSignToolButton(
                    icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(24.dp)) },
                    label = stringResource(R.string.fillsign_tool_image),
                    isActive = activeTool == FillSignTool.IMAGE,
                    onClick = onPickImage
                )
                FillSignToolButton(
                    icon = { Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(24.dp)) },
                    label = stringResource(R.string.fillsign_tool_text),
                    isActive = activeTool == FillSignTool.TEXT,
                    onClick = { onToolSelected(FillSignTool.TEXT) }
                )
                FillSignToolButton(
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(24.dp)) },
                    label = stringResource(R.string.fillsign_tool_date),
                    isActive = activeTool == FillSignTool.DATE_STAMP,
                    onClick = onPlaceDateStamp
                )
                FillSignToolButton(
                    icon = { Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, modifier = Modifier.size(24.dp)) },
                    label = stringResource(R.string.fillsign_tool_add_page),
                    isActive = activeTool == FillSignTool.ADD_PAGE,
                    onClick = onAddPage
                )
                FillSignToolButton(
                    icon = { Icon(Icons.Default.Approval, contentDescription = null, modifier = Modifier.size(24.dp)) },
                    label = stringResource(R.string.fillsign_tool_stamp),
                    isActive = activeTool == FillSignTool.STAMP,
                    onClick = { onToolSelected(FillSignTool.STAMP) }
                )
                FillSignToolButton(
                    icon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(24.dp)) },
                    label = stringResource(R.string.fillsign_tool_link),
                    isActive = activeTool == FillSignTool.LINK,
                    onClick = onLink
                )
            }
        }
    }
}

/** Horizontal row of predefined stamp chips. Tap a stamp to place it on the page. */
@Composable
private fun StampPickerPanel(onPlaceStamp: (String) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            STAMP_OPTIONS.forEach { stamp ->
                OutlinedButton(
                    onClick = { onPlaceStamp(stamp) },
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stamp,
                        fontSize = 13.sp,
                        color = Color(0xFFD32F2F)
                    )
                }
            }
        }
    }
}

/** Single tool button with icon and label. Matches Annotate tab styling. */
@Composable
private fun FillSignToolButton(
    icon: @Composable () -> Unit,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            icon()
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/** Horizontal scrollable gallery of saved signatures with Add button. */
@Composable
private fun SignatureGalleryPanel(
    signatures: List<SignatureEntity>,
    onAdd: () -> Unit,
    onPlace: (SignatureEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            // Add button
            item {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.fillsign_add_signature),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(text = stringResource(R.string.fillsign_add_signature),
                            fontSize = 9.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            // Saved signature thumbnails
            items(signatures, key = { it.id }) { entity ->
                SignatureThumbnail(
                    entity = entity,
                    onTap = { onPlace(entity) },
                    onDelete = { onDelete(entity.id) }
                )
            }
        }
    }
}

/** Single signature thumbnail with delete button. */
@Composable
private fun SignatureThumbnail(
    entity: SignatureEntity,
    onTap: () -> Unit,
    onDelete: () -> Unit
) {
    val bitmap = remember(entity.imagePath) {
        BitmapFactory.decodeFile(entity.imagePath)
    }

    Box(modifier = Modifier.size(64.dp)) {
        // Thumbnail image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .clickable(onClick = onTap),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = entity.name.ifEmpty { "Signature" },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Delete button (top-right corner)
        FilledIconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp)
                .size(20.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = Color.White
            )
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.cd_delete),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
