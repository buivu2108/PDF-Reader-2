package com.pdfapp.reader.ui.editmode.annotate

import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import com.pdfapp.reader.domain.model.AnnotateTool
import com.pdfapp.reader.domain.model.EditAnnotation
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure rendering functions for annotate-tab annotations on Android Canvas.
 * Draws committed annotations and in-progress previews with Bezier smoothing.
 */
object AnnotateCanvasRenderer {

    private val reusablePath = Path()

    /** Draw committed annotate annotations. */
    fun drawCommittedAnnotations(
        canvas: android.graphics.Canvas,
        annotations: List<EditAnnotation>,
        strokePaint: Paint,
        fillPaint: Paint
    ) {
        annotations.forEach { ann ->
            when (ann) {
                is EditAnnotation.FreehandStroke -> drawFreehandStroke(canvas, ann, strokePaint)
                is EditAnnotation.OvalShape -> drawOvalShape(canvas, ann, strokePaint, fillPaint)
                is EditAnnotation.RectShape -> drawRectShape(canvas, ann, strokePaint, fillPaint)
                is EditAnnotation.LineShape -> drawLineShape(canvas, ann, strokePaint)
                is EditAnnotation.ArrowShape -> drawArrowShape(canvas, ann, strokePaint)
                is EditAnnotation.ZigzagShape -> drawZigzagShape(canvas, ann, strokePaint)
                is EditAnnotation.TriangleShape -> drawTriangleShape(canvas, ann, strokePaint, fillPaint)
                is EditAnnotation.PolygonShape -> drawPolygonShape(canvas, ann, strokePaint, fillPaint)
                else -> {} // Mark/FillSign annotations handled elsewhere
            }
        }
    }

    /** Draw in-progress freehand stroke from point list with Bezier smoothing. */
    fun drawFreehandPreview(
        canvas: android.graphics.Canvas, points: List<PointF>,
        color: Int, strokeWidth: Float, paint: Paint
    ) {
        if (points.size < 2) return
        configureFreehandPaint(paint, color, strokeWidth)
        buildBezierPath(points, reusablePath)
        canvas.drawPath(reusablePath, paint)
    }

    /** Draw in-progress polygon preview: freehand path + dashed closing line back to start. */
    fun drawPolygonPreview(
        canvas: android.graphics.Canvas, points: List<PointF>,
        color: Int, strokeWidth: Float, paint: Paint
    ) {
        if (points.size < 2) return
        configureFreehandPaint(paint, color, strokeWidth)
        buildBezierPath(points, reusablePath)
        canvas.drawPath(reusablePath, paint)
        // Dashed closing line from last point to first
        if (points.size >= 3) {
            paint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f, 4f), 0f)
            canvas.drawLine(points.last().x, points.last().y, points.first().x, points.first().y, paint)
            paint.pathEffect = null
        }
    }

    /** Draw in-progress shape preview (outline only). */
    fun drawShapePreview(
        canvas: android.graphics.Canvas, tool: AnnotateTool,
        startPoint: PointF, currentPoint: PointF,
        color: Int, strokeWidth: Float, paint: Paint
    ) {
        paint.apply {
            this.color = color; this.strokeWidth = strokeWidth
            style = Paint.Style.STROKE; isAntiAlias = true
        }
        val rect = RectF(
            minOf(startPoint.x, currentPoint.x), minOf(startPoint.y, currentPoint.y),
            maxOf(startPoint.x, currentPoint.x), maxOf(startPoint.y, currentPoint.y)
        )
        when (tool) {
            AnnotateTool.CIRCLE -> canvas.drawOval(rect, paint)
            AnnotateTool.RECTANGLE -> canvas.drawRect(rect, paint)
            AnnotateTool.LINE -> canvas.drawLine(startPoint.x, startPoint.y, currentPoint.x, currentPoint.y, paint)
            AnnotateTool.ARROW -> {
                canvas.drawLine(startPoint.x, startPoint.y, currentPoint.x, currentPoint.y, paint)
                drawArrowhead(canvas, startPoint, currentPoint, strokeWidth, paint)
            }
            AnnotateTool.ZIGZAG -> {
                buildZigzagPath(startPoint, currentPoint, 8, reusablePath)
                canvas.drawPath(reusablePath, paint)
            }
            AnnotateTool.TRIANGLE -> {
                buildTrianglePath(rect, reusablePath)
                canvas.drawPath(reusablePath, paint)
            }
            else -> {}
        }
    }

    // -- Private: committed shape drawers --

    private fun drawFreehandStroke(canvas: android.graphics.Canvas, stroke: EditAnnotation.FreehandStroke, paint: Paint) {
        if (stroke.points.size < 2) return
        configureFreehandPaint(paint, stroke.color, stroke.strokeWidth)
        buildBezierPath(stroke.points, reusablePath)
        canvas.drawPath(reusablePath, paint)
    }

    private fun drawOvalShape(canvas: android.graphics.Canvas, oval: EditAnnotation.OvalShape, strokePaint: Paint, fillPaint: Paint) {
        oval.fillColor?.let { fc ->
            fillPaint.apply { color = fc; alpha = (oval.fillOpacity * 255).toInt(); style = Paint.Style.FILL; isAntiAlias = true }
            canvas.drawOval(oval.bounds, fillPaint)
        }
        strokePaint.apply { color = oval.color; this.strokeWidth = oval.strokeWidth; style = Paint.Style.STROKE; isAntiAlias = true }
        canvas.drawOval(oval.bounds, strokePaint)
    }

    private fun drawRectShape(canvas: android.graphics.Canvas, rect: EditAnnotation.RectShape, strokePaint: Paint, fillPaint: Paint) {
        rect.fillColor?.let { fc ->
            fillPaint.apply { color = fc; alpha = (rect.fillOpacity * 255).toInt(); style = Paint.Style.FILL; isAntiAlias = true }
            canvas.drawRect(rect.bounds, fillPaint)
        }
        strokePaint.apply { color = rect.color; this.strokeWidth = rect.strokeWidth; style = Paint.Style.STROKE; isAntiAlias = true }
        canvas.drawRect(rect.bounds, strokePaint)
    }

    private fun drawLineShape(canvas: android.graphics.Canvas, line: EditAnnotation.LineShape, paint: Paint) {
        paint.apply { color = line.color; this.strokeWidth = line.strokeWidth; style = Paint.Style.STROKE; isAntiAlias = true; strokeCap = Paint.Cap.ROUND }
        canvas.drawLine(line.startPoint.x, line.startPoint.y, line.endPoint.x, line.endPoint.y, paint)
    }

    private fun drawArrowShape(canvas: android.graphics.Canvas, arrow: EditAnnotation.ArrowShape, paint: Paint) {
        paint.apply { color = arrow.color; this.strokeWidth = arrow.strokeWidth; style = Paint.Style.STROKE; isAntiAlias = true; strokeCap = Paint.Cap.ROUND }
        canvas.drawLine(arrow.startPoint.x, arrow.startPoint.y, arrow.endPoint.x, arrow.endPoint.y, paint)
        drawArrowhead(canvas, arrow.startPoint, arrow.endPoint, arrow.strokeWidth, paint)
    }

    private fun drawZigzagShape(canvas: android.graphics.Canvas, zigzag: EditAnnotation.ZigzagShape, paint: Paint) {
        paint.apply { color = zigzag.color; this.strokeWidth = zigzag.strokeWidth; style = Paint.Style.STROKE; isAntiAlias = true; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        buildZigzagPath(zigzag.startPoint, zigzag.endPoint, zigzag.segments, reusablePath)
        canvas.drawPath(reusablePath, paint)
    }

    private fun drawTriangleShape(canvas: android.graphics.Canvas, tri: EditAnnotation.TriangleShape, strokePaint: Paint, fillPaint: Paint) {
        buildTrianglePath(tri.bounds, reusablePath)
        tri.fillColor?.let { fc ->
            fillPaint.apply { color = fc; alpha = (tri.fillOpacity * 255).toInt(); style = Paint.Style.FILL; isAntiAlias = true }
            canvas.drawPath(reusablePath, fillPaint)
        }
        strokePaint.apply { color = tri.color; this.strokeWidth = tri.strokeWidth; style = Paint.Style.STROKE; isAntiAlias = true }
        canvas.drawPath(reusablePath, strokePaint)
    }

    private fun drawPolygonShape(canvas: android.graphics.Canvas, poly: EditAnnotation.PolygonShape, strokePaint: Paint, fillPaint: Paint) {
        if (poly.points.size < 3) return
        buildBezierPath(poly.points, reusablePath)
        reusablePath.close() // Close the path back to first point
        poly.fillColor?.let { fc ->
            fillPaint.apply { color = fc; alpha = (poly.fillOpacity * 255).toInt(); style = Paint.Style.FILL; isAntiAlias = true }
            canvas.drawPath(reusablePath, fillPaint)
        }
        strokePaint.apply { color = poly.color; this.strokeWidth = poly.strokeWidth; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; isAntiAlias = true }
        canvas.drawPath(reusablePath, strokePaint)
    }

    // -- Private: geometry helpers --

    private fun drawArrowhead(canvas: android.graphics.Canvas, start: PointF, end: PointF, strokeWidth: Float, paint: Paint) {
        val headLen = maxOf(strokeWidth * 5f, 12f)
        val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
        val a1 = angle + Math.toRadians(150.0)
        val a2 = angle - Math.toRadians(150.0)
        val path = reusablePath.apply { reset() }
        path.moveTo(end.x, end.y)
        path.lineTo(end.x + headLen * cos(a1).toFloat(), end.y + headLen * sin(a1).toFloat())
        path.moveTo(end.x, end.y)
        path.lineTo(end.x + headLen * cos(a2).toFloat(), end.y + headLen * sin(a2).toFloat())
        canvas.drawPath(path, paint)
    }

    /** Build zigzag path perpendicular to the line between start and end. */
    private fun buildZigzagPath(start: PointF, end: PointF, segments: Int, path: Path) {
        path.reset()
        val dx = end.x - start.x; val dy = end.y - start.y
        val len = sqrt(dx * dx + dy * dy)
        if (len < 1f || segments < 1) { path.moveTo(start.x, start.y); path.lineTo(end.x, end.y); return }
        // Unit vectors: along line and perpendicular
        val ux = dx / len; val uy = dy / len
        val px = -uy; val py = ux
        val amplitude = maxOf(len * 0.06f, 6f)
        val step = len / segments
        path.moveTo(start.x, start.y)
        for (i in 1..segments) {
            val t = i * step
            val baseX = start.x + ux * t; val baseY = start.y + uy * t
            val side = if (i % 2 == 1) amplitude else -amplitude
            if (i < segments) {
                path.lineTo(baseX + px * side, baseY + py * side)
            } else {
                path.lineTo(end.x, end.y)
            }
        }
    }

    /** Build equilateral-ish triangle: top-center, bottom-left, bottom-right. */
    private fun buildTrianglePath(bounds: RectF, path: Path) {
        path.reset()
        path.moveTo((bounds.left + bounds.right) / 2f, bounds.top)
        path.lineTo(bounds.left, bounds.bottom)
        path.lineTo(bounds.right, bounds.bottom)
        path.close()
    }

    private fun configureFreehandPaint(paint: Paint, color: Int, strokeWidth: Float) {
        paint.apply {
            this.color = color; this.strokeWidth = strokeWidth
            style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; isAntiAlias = true
        }
    }

    private fun buildBezierPath(points: List<PointF>, path: Path) {
        path.reset()
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]; val curr = points[i]
            path.quadTo(prev.x, prev.y, (prev.x + curr.x) / 2f, (prev.y + curr.y) / 2f)
        }
        path.lineTo(points.last().x, points.last().y)
    }
}
