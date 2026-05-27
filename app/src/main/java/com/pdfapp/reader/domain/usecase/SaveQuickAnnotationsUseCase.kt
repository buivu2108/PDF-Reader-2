package com.pdfapp.reader.domain.usecase

import android.content.Context
import android.net.Uri
import com.pdfapp.reader.domain.model.AnnotationType
import com.pdfapp.reader.domain.model.QuickAnnotation
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Saves QuickAnnotations (highlight, underline, strikethrough) to a new PDF file
 * using PdfBox PDAnnotationTextMarkup with QuadPoints per line.
 */
class SaveQuickAnnotationsUseCase(private val context: Context) {

    /**
     * Save annotations to a new PDF. Returns output file path on success, null on failure.
     */
    suspend fun save(sourceUri: Uri, annotations: List<QuickAnnotation>): String? =
        withContext(Dispatchers.IO) {
            var document: PDDocument? = null
            try {
                val inputStream = context.contentResolver.openInputStream(sourceUri)
                    ?: return@withContext null
                document = inputStream.use { PDDocument.load(it) }

                for (pageIndex in 0 until document.numberOfPages) {
                    val page = document.getPage(pageIndex)
                    val pageAnns = annotations.filter { it.pageIndex == pageIndex }
                    if (pageAnns.isEmpty()) continue

                    val mediaBox = page.mediaBox
                    val pdfPageHeight = mediaBox.height

                    pageAnns.forEach { ann ->
                        val subType = when (ann.type) {
                            AnnotationType.HIGHLIGHT -> PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT
                            AnnotationType.UNDERLINE -> PDAnnotationTextMarkup.SUB_TYPE_UNDERLINE
                            AnnotationType.STRIKETHROUGH -> PDAnnotationTextMarkup.SUB_TYPE_STRIKEOUT
                        }

                        val pdfAnn = PDAnnotationTextMarkup(subType)
                        pdfAnn.setColor(toPDColor(ann.color))

                        // Build QuadPoints from all line bounds
                        // Each line produces 8 float values (4 corners x 2 coords)
                        val allQuads = mutableListOf<Float>()
                        var unionRect = PDRectangle()
                        var first = true

                        ann.lineBounds.forEach { lineBound ->
                            // Convert from PDFTextStripper coords (top-left origin Y)
                            // to PDF annotation coords (bottom-left origin Y)
                            val pdfLeft = lineBound.left
                            val pdfRight = lineBound.right
                            val pdfBottom = pdfPageHeight - lineBound.bottom
                            val pdfTop = pdfPageHeight - lineBound.top

                            // QuadPoints order: top-left, top-right, bottom-left, bottom-right
                            allQuads.addAll(listOf(
                                pdfLeft, pdfTop,
                                pdfRight, pdfTop,
                                pdfLeft, pdfBottom,
                                pdfRight, pdfBottom
                            ))

                            val lineRect = PDRectangle(pdfLeft, pdfBottom, pdfRight - pdfLeft, pdfTop - pdfBottom)
                            if (first) {
                                unionRect = lineRect
                                first = false
                            } else {
                                val minX = minOf(unionRect.lowerLeftX, lineRect.lowerLeftX)
                                val minY = minOf(unionRect.lowerLeftY, lineRect.lowerLeftY)
                                val maxX = maxOf(unionRect.upperRightX, lineRect.upperRightX)
                                val maxY = maxOf(unionRect.upperRightY, lineRect.upperRightY)
                                unionRect = PDRectangle(minX, minY, maxX - minX, maxY - minY)
                            }
                        }

                        pdfAnn.rectangle = unionRect
                        pdfAnn.setQuadPoints(allQuads.toFloatArray())
                        page.annotations.add(pdfAnn)
                    }
                }

                val outputDir = File(context.filesDir, "edited").apply { mkdirs() }
                val outputFile = File(outputDir, "annotated_${System.currentTimeMillis()}.pdf")
                FileOutputStream(outputFile).use { fos -> document.save(fos) }
                outputFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                document?.close()
            }
        }

    private fun toPDColor(argb: Int): PDColor {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        return PDColor(floatArrayOf(r, g, b), PDDeviceRGB.INSTANCE)
    }
}
