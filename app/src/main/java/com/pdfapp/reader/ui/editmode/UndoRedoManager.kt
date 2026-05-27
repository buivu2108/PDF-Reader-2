package com.pdfapp.reader.ui.editmode

import com.pdfapp.reader.domain.model.EditAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Manages undo/redo stacks for edit actions. Supports 50+ actions. */
class UndoRedoManager {

    private val undoStack = ArrayDeque<EditAction>()
    private val redoStack = ArrayDeque<EditAction>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    fun pushAction(action: EditAction) {
        undoStack.addLast(action)
        redoStack.clear()
        updateState()
    }

    fun undo(): EditAction? {
        val action = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(action)
        updateState()
        return action
    }

    fun redo(): EditAction? {
        val action = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(action)
        updateState()
        return action
    }

    fun hasChanges(): Boolean = undoStack.isNotEmpty()

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        updateState()
    }

    private fun updateState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }
}
