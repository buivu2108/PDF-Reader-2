package com.pdfapp.reader.ui.editmode.fillsign

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vtsoft.pdfapp.reader.R

/** Extended color palette for text elements. */
private val textColors = listOf(
    0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF9E9E9E.toInt(),
    0xFF8B0000.toInt(), 0xFFF44336.toInt(), 0xFFFF9800.toInt(),
    0xFF4CAF50.toInt(), 0xFF009688.toInt(), 0xFF00BCD4.toInt(),
    0xFF2196F3.toInt(), 0xFF673AB7.toInt(), 0xFFE91E63.toInt(),
    0xFFFF4081.toInt(), 0xFFFFC107.toInt()
)

/** System-safe font families with Android Typeface mapping. */
private val availableFonts = listOf("Helvetica", "Arial", "Times New Roman", "Courier")

/** Map font name to Compose FontFamily. */
private fun fontFamilyFor(name: String): FontFamily = when (name) {
    "Courier" -> FontFamily.Monospace
    "Times New Roman" -> FontFamily.Serif
    else -> FontFamily.SansSerif
}

/**
 * Bottom sheet for creating or editing text elements with rich formatting.
 * Header: Cancel / "Add Text" / Done. Body: B/I/U/S toggles, font picker,
 * size controls, color swatches, multiline text input with live preview.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextCreationBottomSheet(
    initialText: String = "",
    initialFontSize: Float = 16f,
    initialFontFamily: String = "Helvetica",
    initialColor: Int = 0xFF000000.toInt(),
    initialBold: Boolean = false,
    initialItalic: Boolean = false,
    initialUnderline: Boolean = false,
    initialStrikethrough: Boolean = false,
    onDismiss: () -> Unit,
    onDone: (text: String, fontSize: Float, fontFamily: String, color: Int,
             isBold: Boolean, isItalic: Boolean, isUnderline: Boolean, isStrikethrough: Boolean) -> Unit,
    /** Optional: fires on every formatting change for real-time preview on the PDF sticker. */
    onLiveUpdate: ((text: String, fontSize: Float, fontFamily: String, color: Int,
                    isBold: Boolean, isItalic: Boolean, isUnderline: Boolean, isStrikethrough: Boolean) -> Unit)? = null
) {
    var text by remember { mutableStateOf(initialText) }
    var fontSize by remember { mutableFloatStateOf(initialFontSize) }
    var fontFamily by remember { mutableStateOf(initialFontFamily) }
    var selectedColor by remember { mutableIntStateOf(initialColor) }
    var isBold by remember { mutableStateOf(initialBold) }
    var isItalic by remember { mutableStateOf(initialItalic) }
    var isUnderline by remember { mutableStateOf(initialUnderline) }
    var isStrikethrough by remember { mutableStateOf(initialStrikethrough) }

    // Emit live updates whenever any formatting property changes
    LaunchedEffect(text, fontSize, fontFamily, selectedColor, isBold, isItalic, isUnderline, isStrikethrough) {
        onLiveUpdate?.invoke(text, fontSize, fontFamily, selectedColor, isBold, isItalic, isUnderline, isStrikethrough)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // -- Header: Cancel / Title / Done --
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Text(
                    text = stringResource(R.string.fillsign_add_text_title),
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(
                    onClick = {
                        onDone(text, fontSize, fontFamily, selectedColor,
                            isBold, isItalic, isUnderline, isStrikethrough)
                    },
                    enabled = text.isNotBlank()
                ) {
                    Text(stringResource(R.string.fillsign_text_done))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // -- Formatting toggles: B / I / U / S --
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                FormatToggle("B", isBold, fontWeight = FontWeight.Bold) { isBold = it }
                FormatToggle("I", isItalic, fontStyle = FontStyle.Italic) { isItalic = it }
                FormatToggle("U", isUnderline, textDecoration = TextDecoration.Underline) { isUnderline = it }
                FormatToggle("S", isStrikethrough, textDecoration = TextDecoration.LineThrough) { isStrikethrough = it }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // -- Font family + Size controls --
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Font family dropdown
                FontFamilyDropdown(
                    selected = fontFamily,
                    onSelected = { fontFamily = it },
                    modifier = Modifier.weight(1f)
                )
                // Font size controls
                FontSizeControls(
                    size = fontSize,
                    onSizeChange = { fontSize = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // -- Color swatches --
            TextColorPickerRow(
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // -- Text input with live formatting preview --
            val textStyle = TextStyle(
                fontSize = fontSize.coerceIn(12f, 28f).sp, // clamp preview size for readability
                fontFamily = fontFamilyFor(fontFamily),
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = buildTextDecoration(isUnderline, isStrikethrough),
                color = Color(selectedColor)
            )

            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 200.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                textStyle = textStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.fillsign_text_hint),
                                style = textStyle.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Single formatting toggle button (B/I/U/S). */
@Composable
private fun FormatToggle(
    label: String,
    isActive: Boolean,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal,
    textDecoration: TextDecoration = TextDecoration.None,
    onToggle: (Boolean) -> Unit
) {
    val bg = if (isActive) MaterialTheme.colorScheme.primaryContainer
             else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
             else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable { onToggle(!isActive) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                textDecoration = textDecoration,
                color = fg
            )
        )
    }
}

/** Font family dropdown using ExposedDropdownMenuBox. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontFamilyDropdown(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            textStyle = TextStyle(fontSize = 14.sp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableFonts.forEach { font ->
                DropdownMenuItem(
                    text = { Text(font, fontFamily = fontFamilyFor(font)) },
                    onClick = { onSelected(font); expanded = false }
                )
            }
        }
    }
}

/** Font size +/- controls with numeric display. */
@Composable
private fun FontSizeControls(size: Float, onSizeChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(
            onClick = { if (size > 8f) onSizeChange(size - 1f) },
            modifier = Modifier.size(32.dp)
        ) {
            Text("-", fontSize = 16.sp)
        }
        Text(
            text = "${size.toInt()}",
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        FilledTonalIconButton(
            onClick = { if (size < 72f) onSizeChange(size + 1f) },
            modifier = Modifier.size(32.dp)
        ) {
            Text("+", fontSize = 16.sp)
        }
    }
}

/** Horizontally scrollable color swatch row for text colors. */
@Composable
private fun TextColorPickerRow(selectedColor: Int, onColorSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        textColors.forEach { color ->
            val isSelected = color == selectedColor
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .then(
                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        else if (color == 0xFFFFFFFF.toInt()) Modifier.border(1.dp, Color.LightGray, CircleShape)
                        else Modifier
                    )
                    .clickable { onColorSelected(color) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = if (color == 0xFF000000.toInt()) Color.White else Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/** Build TextDecoration from underline + strikethrough flags. */
private fun buildTextDecoration(underline: Boolean, strikethrough: Boolean): TextDecoration {
    val decorations = mutableListOf<TextDecoration>()
    if (underline) decorations.add(TextDecoration.Underline)
    if (strikethrough) decorations.add(TextDecoration.LineThrough)
    return if (decorations.isEmpty()) TextDecoration.None
    else decorations.reduce { a, b -> a + b }
}
