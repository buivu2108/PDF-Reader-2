package com.pdfapp.reader.ui.editmode.fillsign

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vtsoft.pdfapp.reader.R

/** Stroke data: path points + color used when drawing. */
private data class StrokeData(val points: List<Offset>, val color: Int)

/** Available signature ink colors: black, blue, red. */
private val SIGNATURE_COLORS = listOf(
    0xFF000000.toInt(), // Black
    0xFF2196F3.toInt(), // Blue
    0xFFF44336.toInt()  // Red
)

/**
 * Full-screen dialog for drawing a new signature.
 * Features: freehand canvas, 3 color options, single undo, name field, save/clear.
 */
@Composable
fun SignatureCreationDialog(
    onDismiss: () -> Unit,
    onSave: (bitmap: Bitmap, name: String, color: Int) -> Unit
) {
    val strokes = remember { mutableStateListOf<StrokeData>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var selectedColor by remember { mutableIntStateOf(SIGNATURE_COLORS[0]) }
    var signatureName by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // -- Header: Cancel + Title --
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Text(
                        text = stringResource(R.string.fillsign_add_signature),
                        style = MaterialTheme.typography.titleMedium
                    )
                    // Spacer to balance layout
                    Spacer(modifier = Modifier.width(64.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // -- Drawing Canvas --
                var canvasWidth by remember { mutableIntStateOf(0) }
                var canvasHeight by remember { mutableIntStateOf(0) }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .pointerInput(selectedColor) {
                            canvasWidth = size.width
                            canvasHeight = size.height
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPoints = listOf(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentPoints = currentPoints + change.position
                                },
                                onDragEnd = {
                                    if (currentPoints.size > 1) {
                                        strokes.add(StrokeData(currentPoints, selectedColor))
                                    }
                                    currentPoints = emptyList()
                                },
                                onDragCancel = {
                                    currentPoints = emptyList()
                                }
                            )
                        }
                ) {
                    // Draw committed strokes
                    strokes.forEach { stroke ->
                        drawStrokeOnCompose(stroke.points, stroke.color)
                    }
                    // Draw current active stroke
                    if (currentPoints.isNotEmpty()) {
                        drawStrokeOnCompose(currentPoints, selectedColor)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // -- Color picker + Undo --
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.fillsign_color),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SIGNATURE_COLORS.forEach { color ->
                        val isSelected = color == selectedColor
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .then(
                                    if (isSelected) Modifier.border(3.dp, Color.Gray, CircleShape)
                                    else Modifier.border(1.dp, Color.LightGray, CircleShape)
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // Undo button
                    IconButton(
                        onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) },
                        enabled = strokes.isNotEmpty()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = stringResource(R.string.cd_undo)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // -- Signature name field --
                OutlinedTextField(
                    value = signatureName,
                    onValueChange = { signatureName = it },
                    label = { Text(stringResource(R.string.fillsign_signature_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // -- Clear / Save buttons --
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            strokes.clear()
                            currentPoints = emptyList()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.fillsign_clear))
                    }
                    Button(
                        onClick = {
                            if (strokes.isNotEmpty() && canvasWidth > 0 && canvasHeight > 0) {
                                val bitmap = captureSignatureBitmap(
                                    strokes, canvasWidth, canvasHeight
                                )
                                onSave(bitmap, signatureName, selectedColor)
                            }
                        },
                        enabled = strokes.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.fillsign_save))
                    }
                }
            }
        }
    }
}

/** Draw a stroke (list of points) on a Compose DrawScope. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStrokeOnCompose(
    points: List<Offset>,
    color: Int
) {
    if (points.size < 2) return
    val composeColor = Color(color)
    for (i in 0 until points.size - 1) {
        drawLine(
            color = composeColor,
            start = points[i],
            end = points[i + 1],
            strokeWidth = 4f
        )
    }
}

/**
 * Captures drawn strokes as a cropped Bitmap.
 * Renders all strokes onto an Android Canvas, then trims whitespace.
 */
private fun captureSignatureBitmap(
    strokes: List<StrokeData>,
    width: Int,
    height: Int
): Bitmap {
    // Render full canvas
    val fullBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(fullBitmap)
    val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    strokes.forEach { stroke ->
        paint.color = stroke.color
        if (stroke.points.size >= 2) {
            val path = Path()
            path.moveTo(stroke.points[0].x, stroke.points[0].y)
            for (i in 1 until stroke.points.size) {
                path.lineTo(stroke.points[i].x, stroke.points[i].y)
            }
            canvas.drawPath(path, paint)
        }
    }

    // Crop to stroke bounds with padding, recycle the full-size bitmap
    val cropped = cropToContent(fullBitmap, padding = 16)
    if (cropped !== fullBitmap) fullBitmap.recycle()
    return cropped
}

/** Crop bitmap to non-transparent content with padding. */
private fun cropToContent(bitmap: Bitmap, padding: Int): Bitmap {
    var minX = bitmap.width; var minY = bitmap.height
    var maxX = 0; var maxY = 0

    for (y in 0 until bitmap.height) {
        for (x in 0 until bitmap.width) {
            if (bitmap.getPixel(x, y) != 0) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
    }

    if (maxX <= minX || maxY <= minY) return bitmap

    val left = (minX - padding).coerceAtLeast(0)
    val top = (minY - padding).coerceAtLeast(0)
    val right = (maxX + padding).coerceAtMost(bitmap.width)
    val bottom = (maxY + padding).coerceAtMost(bitmap.height)

    return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
}
