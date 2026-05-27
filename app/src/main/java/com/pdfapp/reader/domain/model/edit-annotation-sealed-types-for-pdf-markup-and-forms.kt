package com.pdfapp.reader.domain.model

import android.graphics.PointF
import android.graphics.RectF
import java.util.UUID

/**
 * Sealed class representing all annotation types across the 3 edit mode tabs.
 * Every annotation has an id, pageIndex, and mutable bounds (screen-space during editing,
 * converted to PDF-space on save).
 */
sealed class EditAnnotation {
    abstract val id: String
    abstract val pageIndex: Int
    abstract val bounds: RectF

    // -- Mark tab (text-selection-based markup) --

    /** Semi-transparent highlight over selected text quads. */
    data class Highlight(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val bounds: RectF,
        val textQuads: List<RectF>,
        val color: Int = 0xFFFFFF00.toInt(),
        val opacity: Float = 0.4f,
        val note: String? = null
    ) : EditAnnotation()

    /** Colored underline beneath selected text quads. */
    data class Underline(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val bounds: RectF,
        val textQuads: List<RectF>,
        val color: Int = 0xFF0000FF.toInt(),
        val opacity: Float = 1.0f,
        val note: String? = null
    ) : EditAnnotation()

    /** Strikethrough line through selected text quads. */
    data class Strikethrough(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val bounds: RectF,
        val textQuads: List<RectF>,
        val color: Int = 0xFFFF0000.toInt(),
        val opacity: Float = 1.0f,
        val note: String? = null
    ) : EditAnnotation()

    /** Sticky note icon placed at a tap position with editable text. */
    data class StickyNote(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val bounds: RectF,
        val text: String = "",
        val color: Int = 0xFFFFEB3B.toInt()
    ) : EditAnnotation()

    // -- Annotate tab (freehand + shapes) --

    /** Freehand drawing stroke with Bezier-smoothed points. */
    data class FreehandStroke(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val bounds: RectF,
        val points: List<PointF>,
        val color: Int = 0xFF000000.toInt(),
        val strokeWidth: Float = 3f
    ) : EditAnnotation()

    /** Oval/ellipse shape annotation. */
    data class OvalShape(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val bounds: RectF,
        val color: Int = 0xFF000000.toInt(),
        val strokeWidth: Float = 2f,
        val fillColor: Int? = null,
        val fillOpacity: Float = 0.3f
    ) : EditAnnotation()

    /** Rectangle shape annotation. */
    data class RectShape(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val bounds: RectF,
        val color: Int = 0xFF000000.toInt(),
        val strokeWidth: Float = 2f,
        val fillColor: Int? = null,
        val fillOpacity: Float = 0.3f
    ) : EditAnnotation()

    /** Straight line between two points. */
    data class LineShape(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val bounds: RectF,
        val startPoint: PointF,
        val endPoint: PointF,
        val color: Int = 0xFF000000.toInt(),
        val strokeWidth: Float = 2f
    ) : EditAnnotation()

    /** Arrow (line with arrowhead at end point). */
    data class ArrowShape(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val bounds: RectF,
        val startPoint: PointF,
        val endPoint: PointF,
        val color: Int = 0xFF000000.toInt(),
        val strokeWidth: Float = 2f
    ) : EditAnnotation()

    /** Zigzag line between two points. */
    data class ZigzagShape(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val bounds: RectF,
        val startPoint: PointF,
        val endPoint: PointF,
        val color: Int = 0xFF000000.toInt(),
        val strokeWidth: Float = 2f,
        val segments: Int = 8
    ) : EditAnnotation()

    /** Triangle shape annotation. */
    data class TriangleShape(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val bounds: RectF,
        val color: Int = 0xFF000000.toInt(),
        val strokeWidth: Float = 2f,
        val fillColor: Int? = null,
        val fillOpacity: Float = 0.3f
    ) : EditAnnotation()

    /** Closed polygon drawn freehand (auto-closes path from last point to first). */
    data class PolygonShape(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val bounds: RectF,
        val points: List<PointF>,
        val color: Int = 0xFF000000.toInt(),
        val strokeWidth: Float = 2f,
        val fillColor: Int? = null,
        val fillOpacity: Float = 0.3f
    ) : EditAnnotation()

    // -- Fill & Sign tab --

    /** Placed signature from gallery or drawn. */
    data class SignatureElement(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val bounds: RectF,
        val imagePath: String,
        val color: Int? = null,
        val strokeWidth: Float? = null,
        val rotation: Float = 0f
    ) : EditAnnotation()

    /** Placed image from device gallery, stored as internal file. */
    data class ImageElement(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val bounds: RectF,
        val imagePath: String,
        val rotation: Float = 0f
    ) : EditAnnotation()

    /** Auto-formatted date stamp. */
    data class DateStamp(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val bounds: RectF,
        val text: String,
        val fontSize: Float = 14f,
        val color: Int = 0xFF000000.toInt()
    ) : EditAnnotation()

    /** Placed text element with rich formatting (bold/italic/underline/strikethrough). */
    data class TextElement(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val bounds: RectF,
        val text: String,
        val fontSize: Float = 16f,
        val fontFamily: String = "Helvetica",
        val color: Int = 0xFF000000.toInt(),
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val isUnderline: Boolean = false,
        val isStrikethrough: Boolean = false,
        val rotation: Float = 0f
    ) : EditAnnotation()
}
