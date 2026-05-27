package com.pdfapp.reader.domain.model

import android.graphics.RectF

/** Represents an undoable/redoable edit action across all edit mode tabs. */
sealed class EditAction {
    data class Add(val annotation: EditAnnotation) : EditAction()
    data class Remove(val annotation: EditAnnotation) : EditAction()
    data class Move(val id: String, val oldBounds: RectF, val newBounds: RectF) : EditAction()
    data class Resize(val id: String, val oldBounds: RectF, val newBounds: RectF) : EditAction()
    data class ChangeColor(val id: String, val oldColor: Int, val newColor: Int) : EditAction()
    data class ChangeWidth(val id: String, val oldWidth: Float, val newWidth: Float) : EditAction()
    data class ChangeOpacity(val id: String, val oldOpacity: Float, val newOpacity: Float) : EditAction()
    data class ChangeFontSize(val id: String, val oldSize: Float, val newSize: Float) : EditAction()
    /** Batch move of multiple annotations (region drag). */
    data class GroupMove(val ids: List<String>, val deltaX: Float, val deltaY: Float) : EditAction()
}
