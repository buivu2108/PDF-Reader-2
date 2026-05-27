package com.pdfapp.reader.ui.editmode

import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.domain.model.AnnotateTool
import com.pdfapp.reader.domain.model.EditTab
import com.pdfapp.reader.ui.editmode.annotate.AnnotateTabViewModel
import com.pdfapp.reader.ui.editmode.annotate.FreehandStrokeSettingsSheet
import com.pdfapp.reader.ui.editmode.annotate.ShapeFillAndStrokeSettingsSheet
import com.pdfapp.reader.ui.editmode.mark.HighlightSettingsSheet
import com.pdfapp.reader.ui.editmode.mark.MarkTabViewModel

/** Tab selector row for Mark / Annotate / Fill & Sign. */
@Composable
fun EditModeTabSelector(
    selectedTab: EditTab,
    onTabSelected: (EditTab) -> Unit
) {
    val tabs = EditTab.entries
    TabRow(selectedTabIndex = tabs.indexOf(selectedTab)) {
        tabs.forEach { tab ->
            Tab(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = when (tab) {
                            EditTab.MARK -> stringResource(R.string.edit_tab_mark)
                            EditTab.ANNOTATE -> stringResource(R.string.edit_tab_annotate)
                            EditTab.FILL_SIGN -> stringResource(R.string.edit_tab_fill_sign)
                        }
                    )
                }
            )
        }
    }
}

/** Renders mark settings sheet with shared color picker + opacity slider for all tools. */
@Composable
fun MarkToolSettingsSheet(
    showSettings: Boolean,
    markColor: Int,
    markOpacity: Float,
    markViewModel: MarkTabViewModel,
    onDismiss: () -> Unit
) {
    if (!showSettings) return
    HighlightSettingsSheet(
        highlightColor = markColor,
        highlightOpacity = markOpacity,
        onColorSelected = markViewModel::setMarkColor,
        onOpacityChange = markViewModel::setMarkOpacity,
        onDismiss = onDismiss
    )
}

/** Renders the appropriate settings bottom sheet for the active Annotate tool. */
@Composable
fun AnnotateToolSettingsSheets(
    showAnnotateSettings: Boolean,
    annotateActiveTool: AnnotateTool,
    annotateStrokeColor: Int,
    annotateStrokeWidth: Float,
    annotateFillEnabled: Boolean,
    annotateFillColor: Int,
    annotateViewModel: AnnotateTabViewModel,
    onDismiss: () -> Unit
) {
    if (!showAnnotateSettings) return
    when (annotateActiveTool) {
        AnnotateTool.DRAW -> FreehandStrokeSettingsSheet(
            strokeColor = annotateStrokeColor,
            strokeWidth = annotateStrokeWidth,
            onColorSelected = annotateViewModel::setStrokeColor,
            onStrokeWidthChange = annotateViewModel::setStrokeWidth,
            onDismiss = onDismiss
        )
        AnnotateTool.CIRCLE, AnnotateTool.RECTANGLE, AnnotateTool.TRIANGLE, AnnotateTool.POLYGON -> ShapeFillAndStrokeSettingsSheet(
            strokeColor = annotateStrokeColor,
            strokeWidth = annotateStrokeWidth,
            fillEnabled = annotateFillEnabled,
            fillColor = annotateFillColor,
            onColorSelected = annotateViewModel::setStrokeColor,
            onStrokeWidthChange = annotateViewModel::setStrokeWidth,
            onFillEnabledChange = annotateViewModel::setFillEnabled,
            onFillColorSelected = annotateViewModel::setFillColor,
            onDismiss = onDismiss
        )
        AnnotateTool.LINE, AnnotateTool.ARROW, AnnotateTool.ZIGZAG -> FreehandStrokeSettingsSheet(
            strokeColor = annotateStrokeColor,
            strokeWidth = annotateStrokeWidth,
            onColorSelected = annotateViewModel::setStrokeColor,
            onStrokeWidthChange = annotateViewModel::setStrokeWidth,
            onDismiss = onDismiss
        )
        AnnotateTool.SELECT, AnnotateTool.ERASER, AnnotateTool.NONE -> onDismiss()
    }
}
