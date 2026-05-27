package com.pdfapp.reader.domain.usecase

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.RectF
import com.pdfapp.reader.domain.model.EditAnnotation
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Per-type PdfBox renderers that flatten EditAnnotation subtypes into a
 * PDPageContentStream (APPEND mode). Each function transforms bitmap-pixel
 * coords to PDF-point coords using the supplied scale factors.
 */

// -- Coordinate transform helper --

/** Convert bitmap-pixel RectF to PDF-point RectF (Y-axis inverted). */
fun transformToPdfSpace(
    bounds: RectF,
    scaleX: Float, scaleY: Float,
    pdfPageHeight: Float
): RectF = RectF(
    bounds.left * scaleX,
    pdfPageHeight - bounds.top * scaleY,
    bounds.right * scaleX,
    pdfPageHeight - bounds.bottom * scaleY
)

/** Transform a single point from bitmap pixels to PDF points. */
fun transformPointToPdf(
    x: Float, y: Float,
    scaleX: Float, scaleY: Float,
    pdfPageHeight: Float
): Pair<Float, Float> = Pair(x * scaleX, pdfPageHeight - y * scaleY)

// -- Color helpers --

private fun extractRgb(argb: Int): Triple<Float, Float, Float> {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    return Triple(r, g, b)
}

private fun PDPageContentStream.setStrokingRgb(argb: Int) {
    val (r, g, b) = extractRgb(argb)
    setStrokingColor(r, g, b)
}

private fun PDPageContentStream.setNonStrokingRgb(argb: Int, alpha: Float = 1f) {
    val (r, g, b) = extractRgb(argb)
    setNonStrokingColor(r, g, b)
}

/** Apply PDF ExtGState transparency so highlight fills don't cover underlying text. */
private fun PDPageContentStream.setNonStrokingAlpha(
    doc: PDDocument, alpha: Float
) {
    if (alpha >= 1f) return
    val gs = com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState()
    gs.nonStrokingAlphaConstant = alpha.coerceIn(0f, 1f)
    setGraphicsStateParameters(gs)
}

// -- Renderers --

fun renderHighlight(
    cs: PDPageContentStream, doc: PDDocument,
    ann: EditAnnotation.Highlight, scaleX: Float, scaleY: Float, pdfH: Float
) {
    cs.saveGraphicsState()
    cs.setNonStrokingRgb(ann.color)
    cs.setNonStrokingAlpha(doc, ann.opacity)
    ann.textQuads.forEach { quad ->
        val pq = transformToPdfSpace(quad, scaleX, scaleY, pdfH)
        val x = minOf(pq.left, pq.right)
        val y = minOf(pq.top, pq.bottom)
        val w = Math.abs(pq.right - pq.left)
        val h = Math.abs(pq.top - pq.bottom)
        cs.addRect(x, y, w, h)
    }
    cs.fill()
    cs.restoreGraphicsState()
}

fun renderUnderline(cs: PDPageContentStream, ann: EditAnnotation.Underline, scaleX: Float, scaleY: Float, pdfH: Float) {
    cs.saveGraphicsState()
    cs.setStrokingRgb(ann.color)
    cs.setLineWidth(1f)
    ann.textQuads.forEach { quad ->
        val pq = transformToPdfSpace(quad, scaleX, scaleY, pdfH)
        // Draw line at bottom of quad
        val y = minOf(pq.top, pq.bottom)
        cs.moveTo(minOf(pq.left, pq.right), y)
        cs.lineTo(maxOf(pq.left, pq.right), y)
    }
    cs.stroke()
    cs.restoreGraphicsState()
}

fun renderStrikethrough(cs: PDPageContentStream, ann: EditAnnotation.Strikethrough, scaleX: Float, scaleY: Float, pdfH: Float) {
    cs.saveGraphicsState()
    cs.setStrokingRgb(ann.color)
    cs.setLineWidth(1f)
    ann.textQuads.forEach { quad ->
        val pq = transformToPdfSpace(quad, scaleX, scaleY, pdfH)
        // Draw line at vertical center of quad
        val midY = (pq.top + pq.bottom) / 2f
        cs.moveTo(minOf(pq.left, pq.right), midY)
        cs.lineTo(maxOf(pq.left, pq.right), midY)
    }
    cs.stroke()
    cs.restoreGraphicsState()
}

/** Render sticky note as a colored square with text on the PDF. */
fun renderStickyNote(cs: PDPageContentStream, ann: EditAnnotation.StickyNote, scaleX: Float, scaleY: Float, pdfH: Float) {
    val pq = transformToPdfSpace(ann.bounds, scaleX, scaleY, pdfH)
    val x = minOf(pq.left, pq.right)
    val y = minOf(pq.top, pq.bottom)
    val w = Math.abs(pq.right - pq.left)
    val h = Math.abs(pq.top - pq.bottom)
    cs.saveGraphicsState()
    // Filled note body
    cs.setNonStrokingRgb(ann.color)
    cs.addRect(x, y, w, h)
    cs.fill()
    // Border outline
    cs.setStrokingRgb(0xFF333333.toInt())
    cs.setLineWidth(0.5f)
    cs.addRect(x, y, w, h)
    cs.stroke()
    cs.restoreGraphicsState()
}

fun renderFreehandStroke(cs: PDPageContentStream, ann: EditAnnotation.FreehandStroke, scaleX: Float, scaleY: Float, pdfH: Float) {
    if (ann.points.size < 2) return
    cs.saveGraphicsState()
    cs.setStrokingRgb(ann.color)
    cs.setLineWidth(ann.strokeWidth * scaleX)
    val first = transformPointToPdf(ann.points[0].x, ann.points[0].y, scaleX, scaleY, pdfH)
    cs.moveTo(first.first, first.second)
    for (i in 1 until ann.points.size) {
        val pt = transformPointToPdf(ann.points[i].x, ann.points[i].y, scaleX, scaleY, pdfH)
        cs.lineTo(pt.first, pt.second)
    }
    cs.stroke()
    cs.restoreGraphicsState()
}

fun renderOvalShape(cs: PDPageContentStream, ann: EditAnnotation.OvalShape, scaleX: Float, scaleY: Float, pdfH: Float) {
    val pq = transformToPdfSpace(ann.bounds, scaleX, scaleY, pdfH)
    val cx = (minOf(pq.left, pq.right) + maxOf(pq.left, pq.right)) / 2f
    val cy = (minOf(pq.top, pq.bottom) + maxOf(pq.top, pq.bottom)) / 2f
    val rx = Math.abs(pq.right - pq.left) / 2f
    val ry = Math.abs(pq.top - pq.bottom) / 2f
    // Bézier approximation of ellipse (kappa = 0.5522847498)
    val k = 0.5522847498f
    cs.saveGraphicsState()
    cs.moveTo(cx + rx, cy)
    cs.curveTo(cx + rx, cy + ry * k, cx + rx * k, cy + ry, cx, cy + ry)
    cs.curveTo(cx - rx * k, cy + ry, cx - rx, cy + ry * k, cx - rx, cy)
    cs.curveTo(cx - rx, cy - ry * k, cx - rx * k, cy - ry, cx, cy - ry)
    cs.curveTo(cx + rx * k, cy - ry, cx + rx, cy - ry * k, cx + rx, cy)
    cs.closePath()
    if (ann.fillColor != null) {
        cs.setNonStrokingRgb(ann.fillColor, ann.fillOpacity)
        cs.setStrokingRgb(ann.color)
        cs.setLineWidth(ann.strokeWidth * scaleX)
        cs.fillAndStroke()
    } else {
        cs.setStrokingRgb(ann.color)
        cs.setLineWidth(ann.strokeWidth * scaleX)
        cs.stroke()
    }
    cs.restoreGraphicsState()
}

fun renderRectShape(cs: PDPageContentStream, ann: EditAnnotation.RectShape, scaleX: Float, scaleY: Float, pdfH: Float) {
    val pq = transformToPdfSpace(ann.bounds, scaleX, scaleY, pdfH)
    val x = minOf(pq.left, pq.right)
    val y = minOf(pq.top, pq.bottom)
    val w = Math.abs(pq.right - pq.left)
    val h = Math.abs(pq.top - pq.bottom)
    cs.saveGraphicsState()
    cs.addRect(x, y, w, h)
    if (ann.fillColor != null) {
        cs.setNonStrokingRgb(ann.fillColor, ann.fillOpacity)
        cs.setStrokingRgb(ann.color)
        cs.setLineWidth(ann.strokeWidth * scaleX)
        cs.fillAndStroke()
    } else {
        cs.setStrokingRgb(ann.color)
        cs.setLineWidth(ann.strokeWidth * scaleX)
        cs.stroke()
    }
    cs.restoreGraphicsState()
}

fun renderSignatureOrImage(
    cs: PDPageContentStream, doc: PDDocument, context: Context,
    imagePath: String, bounds: RectF, rotation: Float,
    scaleX: Float, scaleY: Float, pdfH: Float
) {
    val file = File(imagePath)
    if (!file.exists()) return
    val bitmap = BitmapFactory.decodeFile(imagePath) ?: return
    val baos = ByteArrayOutputStream()
    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos)
    bitmap.recycle()
    val imageBytes = baos.toByteArray()
    val pdImage = PDImageXObject.createFromByteArray(doc, imageBytes, "img")

    val pq = transformToPdfSpace(bounds, scaleX, scaleY, pdfH)
    val x = minOf(pq.left, pq.right)
    val y = minOf(pq.top, pq.bottom)
    val w = Math.abs(pq.right - pq.left)
    val h = Math.abs(pq.top - pq.bottom)

    cs.saveGraphicsState()
    if (rotation != 0f) {
        // Rotate around center of the image rect
        val centerX = x + w / 2f
        val centerY = y + h / 2f
        val rad = Math.toRadians(rotation.toDouble())
        val cos = Math.cos(rad).toFloat()
        val sin = Math.sin(rad).toFloat()
        val matrix = com.tom_roush.pdfbox.util.Matrix(cos, sin, -sin, cos, centerX - cos * centerX + sin * centerY, centerY - sin * centerX - cos * centerY)
        cs.transform(matrix)
    }
    cs.drawImage(pdImage, x, y, w, h)
    cs.restoreGraphicsState()
}

fun renderTextElement(cs: PDPageContentStream, ann: EditAnnotation.TextElement, scaleX: Float, scaleY: Float, pdfH: Float) {
    val pq = transformToPdfSpace(ann.bounds, scaleX, scaleY, pdfH)
    val x = minOf(pq.left, pq.right)
    val y = maxOf(pq.top, pq.bottom) // PDF baseline from top of rect
    val fontSize = ann.fontSize * scaleX

    // Select PDF font variant based on bold/italic and font family
    val font = when (ann.fontFamily) {
        "Courier" -> when {
            ann.isBold && ann.isItalic -> PDType1Font.COURIER_BOLD_OBLIQUE
            ann.isBold -> PDType1Font.COURIER_BOLD
            ann.isItalic -> PDType1Font.COURIER_OBLIQUE
            else -> PDType1Font.COURIER
        }
        "Times New Roman" -> when {
            ann.isBold && ann.isItalic -> PDType1Font.TIMES_BOLD_ITALIC
            ann.isBold -> PDType1Font.TIMES_BOLD
            ann.isItalic -> PDType1Font.TIMES_ITALIC
            else -> PDType1Font.TIMES_ROMAN
        }
        else -> when { // Helvetica / Arial
            ann.isBold && ann.isItalic -> PDType1Font.HELVETICA_BOLD_OBLIQUE
            ann.isBold -> PDType1Font.HELVETICA_BOLD
            ann.isItalic -> PDType1Font.HELVETICA_OBLIQUE
            else -> PDType1Font.HELVETICA
        }
    }

    // Preserve newlines (code 10) for multiline split, filter other control chars
    val safeText = ann.text.filter { it.code == 10 || it.code in 32..255 }
    if (safeText.isBlank()) return

    cs.saveGraphicsState()
    if (ann.rotation != 0f) {
        val cx = x + Math.abs(pq.right - pq.left) / 2f
        val cy = minOf(pq.top, pq.bottom) + Math.abs(pq.top - pq.bottom) / 2f
        val rad = Math.toRadians(ann.rotation.toDouble())
        val cos = Math.cos(rad).toFloat()
        val sin = Math.sin(rad).toFloat()
        val matrix = com.tom_roush.pdfbox.util.Matrix(
            cos, sin, -sin, cos,
            cx - cos * cx + sin * cy, cy - sin * cx - cos * cy
        )
        cs.transform(matrix)
    }
    cs.setNonStrokingRgb(ann.color)
    cs.beginText()
    cs.setFont(font, fontSize)
    // Split text by newlines for multiline rendering
    val lines = safeText.split("\n")
    cs.newLineAtOffset(x, y - fontSize)
    cs.showText(lines.first())
    for (i in 1 until lines.size) {
        cs.newLineAtOffset(0f, -fontSize * 1.2f)
        cs.showText(lines[i])
    }
    cs.endText()
    cs.restoreGraphicsState()
}

fun renderDateStamp(cs: PDPageContentStream, ann: EditAnnotation.DateStamp, scaleX: Float, scaleY: Float, pdfH: Float) {
    val pq = transformToPdfSpace(ann.bounds, scaleX, scaleY, pdfH)
    val x = minOf(pq.left, pq.right)
    val y = minOf(pq.top, pq.bottom)
    val fontSize = ann.fontSize * scaleX
    // Filter to WinAnsiEncoding-safe chars to avoid crash with PDType1Font.HELVETICA
    val safeText = ann.text.filter { it.code in 32..255 }
    if (safeText.isEmpty()) return
    cs.saveGraphicsState()
    cs.setNonStrokingRgb(ann.color)
    cs.beginText()
    cs.setFont(PDType1Font.HELVETICA, fontSize)
    cs.newLineAtOffset(x, y)
    cs.showText(safeText)
    cs.endText()
    cs.restoreGraphicsState()
}

fun renderLineShape(cs: PDPageContentStream, ann: EditAnnotation.LineShape, scaleX: Float, scaleY: Float, pdfH: Float) {
    val s = transformPointToPdf(ann.startPoint.x, ann.startPoint.y, scaleX, scaleY, pdfH)
    val e = transformPointToPdf(ann.endPoint.x, ann.endPoint.y, scaleX, scaleY, pdfH)
    cs.saveGraphicsState()
    cs.setStrokingRgb(ann.color)
    cs.setLineWidth(ann.strokeWidth * scaleX)
    cs.moveTo(s.first, s.second)
    cs.lineTo(e.first, e.second)
    cs.stroke()
    cs.restoreGraphicsState()
}

fun renderArrowShape(cs: PDPageContentStream, ann: EditAnnotation.ArrowShape, scaleX: Float, scaleY: Float, pdfH: Float) {
    val s = transformPointToPdf(ann.startPoint.x, ann.startPoint.y, scaleX, scaleY, pdfH)
    val e = transformPointToPdf(ann.endPoint.x, ann.endPoint.y, scaleX, scaleY, pdfH)
    cs.saveGraphicsState()
    cs.setStrokingRgb(ann.color)
    cs.setLineWidth(ann.strokeWidth * scaleX)
    // Main line
    cs.moveTo(s.first, s.second)
    cs.lineTo(e.first, e.second)
    cs.stroke()
    // Arrowhead
    val headLen = maxOf(ann.strokeWidth * 5f * scaleX, 8f)
    val angle = Math.atan2((e.second - s.second).toDouble(), (e.first - s.first).toDouble())
    val a1 = angle + Math.toRadians(150.0)
    val a2 = angle - Math.toRadians(150.0)
    cs.moveTo(e.first, e.second)
    cs.lineTo(e.first + (headLen * Math.cos(a1)).toFloat(), e.second + (headLen * Math.sin(a1)).toFloat())
    cs.stroke()
    cs.moveTo(e.first, e.second)
    cs.lineTo(e.first + (headLen * Math.cos(a2)).toFloat(), e.second + (headLen * Math.sin(a2)).toFloat())
    cs.stroke()
    cs.restoreGraphicsState()
}

fun renderZigzagShape(cs: PDPageContentStream, ann: EditAnnotation.ZigzagShape, scaleX: Float, scaleY: Float, pdfH: Float) {
    val s = transformPointToPdf(ann.startPoint.x, ann.startPoint.y, scaleX, scaleY, pdfH)
    val e = transformPointToPdf(ann.endPoint.x, ann.endPoint.y, scaleX, scaleY, pdfH)
    val dx = e.first - s.first; val dy = e.second - s.second
    val len = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    if (len < 1f) return
    val ux = dx / len; val uy = dy / len; val px = -uy; val py = ux
    val amplitude = maxOf(len * 0.06f, 4f)
    val step = len / ann.segments
    cs.saveGraphicsState()
    cs.setStrokingRgb(ann.color)
    cs.setLineWidth(ann.strokeWidth * scaleX)
    cs.moveTo(s.first, s.second)
    for (i in 1..ann.segments) {
        val t = i * step
        val bx = s.first + ux * t; val by = s.second + uy * t
        val side = if (i % 2 == 1) amplitude else -amplitude
        if (i < ann.segments) cs.lineTo(bx + px * side, by + py * side)
        else cs.lineTo(e.first, e.second)
    }
    cs.stroke()
    cs.restoreGraphicsState()
}

fun renderTriangleShape(cs: PDPageContentStream, ann: EditAnnotation.TriangleShape, scaleX: Float, scaleY: Float, pdfH: Float) {
    val pq = transformToPdfSpace(ann.bounds, scaleX, scaleY, pdfH)
    val l = minOf(pq.left, pq.right); val r = maxOf(pq.left, pq.right)
    val b = minOf(pq.top, pq.bottom); val t = maxOf(pq.top, pq.bottom)
    val midX = (l + r) / 2f
    cs.saveGraphicsState()
    ann.fillColor?.let { fc ->
        cs.setNonStrokingRgb(fc, ann.fillOpacity)
        cs.moveTo(midX, t); cs.lineTo(l, b); cs.lineTo(r, b); cs.closePath(); cs.fill()
    }
    cs.setStrokingRgb(ann.color)
    cs.setLineWidth(ann.strokeWidth * scaleX)
    cs.moveTo(midX, t); cs.lineTo(l, b); cs.lineTo(r, b); cs.closePath(); cs.stroke()
    cs.restoreGraphicsState()
}

fun renderPolygonShape(cs: PDPageContentStream, ann: EditAnnotation.PolygonShape, scaleX: Float, scaleY: Float, pdfH: Float) {
    if (ann.points.size < 3) return
    cs.saveGraphicsState()
    val first = transformPointToPdf(ann.points[0].x, ann.points[0].y, scaleX, scaleY, pdfH)
    // Fill if enabled
    ann.fillColor?.let { fc ->
        cs.setNonStrokingRgb(fc, ann.fillOpacity)
        cs.moveTo(first.first, first.second)
        for (i in 1 until ann.points.size) {
            val pt = transformPointToPdf(ann.points[i].x, ann.points[i].y, scaleX, scaleY, pdfH)
            cs.lineTo(pt.first, pt.second)
        }
        cs.closePath(); cs.fill()
    }
    // Stroke
    cs.setStrokingRgb(ann.color)
    cs.setLineWidth(ann.strokeWidth * scaleX)
    cs.moveTo(first.first, first.second)
    for (i in 1 until ann.points.size) {
        val pt = transformPointToPdf(ann.points[i].x, ann.points[i].y, scaleX, scaleY, pdfH)
        cs.lineTo(pt.first, pt.second)
    }
    cs.closePath(); cs.stroke()
    cs.restoreGraphicsState()
}
