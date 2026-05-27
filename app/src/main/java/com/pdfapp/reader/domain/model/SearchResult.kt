package com.pdfapp.reader.domain.model

import android.graphics.RectF

/** A single text search match within a PDF page. */
data class SearchResult(
    val pageIndex: Int,
    val matchText: String,
    val lineBounds: List<RectF>
)
