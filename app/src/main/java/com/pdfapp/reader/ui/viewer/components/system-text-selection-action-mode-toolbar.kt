package com.pdfapp.reader.ui.viewer.components

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.vtsoft.pdfapp.reader.R

private const val MENU_COPY = 1
private const val MENU_SELECT_ALL = 2
private const val MENU_SHARE = 3
private const val PROCESS_TEXT_BASE = 100

/**
 * Bridges Compose text selection state to Android's native floating ActionMode toolbar.
 * Displays system-standard actions (Copy, Select All, Share) plus PROCESS_TEXT activities
 * (Search, Translate, etc.) discovered from installed apps on the device.
 *
 * Hides automatically during handle drag and reappears when drag ends.
 * Clears selection only when the user explicitly dismisses the toolbar (not during drag hide).
 */
@Composable
fun SystemTextSelectionToolbar(
    isVisible: Boolean,
    selectedText: String,
    anchorX: Int,
    anchorY: Int,
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    val processTextApps = remember { queryProcessTextActivities(context) }

    // Mutable holder keeps callbacks/coordinates fresh without recomposition
    val holder = remember { ActionModeHolder() }
    holder.onCopy = onCopy
    holder.onSelectAll = onSelectAll
    holder.onShare = onShare
    holder.onDismiss = onDismiss
    holder.anchorX = anchorX
    holder.anchorY = anchorY
    holder.selectedText = selectedText
    holder.isVisible = isVisible

    // Start or finish ActionMode when visibility changes
    LaunchedEffect(isVisible) {
        if (isVisible && selectedText.isNotBlank()) {
            holder.start(view, context, processTextApps)
        } else {
            holder.finish()
        }
    }

    // Reposition floating toolbar when anchor moves
    LaunchedEffect(anchorX, anchorY) {
        holder.actionMode?.invalidateContentRect()
    }

    DisposableEffect(Unit) {
        onDispose { holder.finish() }
    }
}

/**
 * Holds the latest callback and coordinate values for ActionMode callbacks.
 * Avoids stale closures by updating fields directly from the composable body.
 */
private class ActionModeHolder {
    var onCopy: () -> Unit = {}
    var onSelectAll: () -> Unit = {}
    var onShare: () -> Unit = {}
    var onDismiss: () -> Unit = {}
    var anchorX: Int = 0
    var anchorY: Int = 0
    var selectedText: String = ""
    var isVisible: Boolean = false
    var actionMode: ActionMode? = null

    fun start(view: View, context: Context, processTextApps: List<ProcessTextApp>) {
        if (actionMode != null) return
        actionMode = view.startActionMode(object : ActionMode.Callback2() {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                menu.add(Menu.NONE, MENU_COPY, 0, android.R.string.copy)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                menu.add(Menu.NONE, MENU_SELECT_ALL, 1, android.R.string.selectAll)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                menu.add(Menu.NONE, MENU_SHARE, 2, context.getString(R.string.viewer_share))
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                // Add system PROCESS_TEXT activities (Search, Translate, etc.)
                processTextApps.forEachIndexed { i, app ->
                    menu.add(Menu.NONE, PROCESS_TEXT_BASE + i, 10 + i, app.label)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                }
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                when (item.itemId) {
                    MENU_COPY -> onCopy()
                    MENU_SELECT_ALL -> onSelectAll()
                    MENU_SHARE -> onShare()
                    else -> {
                        val idx = item.itemId - PROCESS_TEXT_BASE
                        if (idx in processTextApps.indices) {
                            launchProcessText(context, processTextApps[idx], selectedText)
                            onDismiss()
                        }
                    }
                }
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                actionMode = null
                // Only clear selection if dismissed by user interaction (isVisible still true).
                // Skip when programmatically hidden during drag (isVisible already false).
                if (isVisible) onDismiss()
            }

            override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) {
                outRect.set(anchorX - 1, anchorY - 10, anchorX + 1, anchorY)
            }
        }, ActionMode.TYPE_FLOATING)
    }

    fun finish() {
        actionMode?.finish()
        actionMode = null
    }
}

private data class ProcessTextApp(val label: String, val pkg: String, val cls: String)

/** Query installed apps that handle ACTION_PROCESS_TEXT (provides Search, Translate, etc.) */
private fun queryProcessTextActivities(context: Context): List<ProcessTextApp> {
    val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply { type = "text/plain" }
    val pm = context.packageManager
    val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, 0)
    }
    return resolved.map { info ->
        ProcessTextApp(
            label = info.loadLabel(pm).toString(),
            pkg = info.activityInfo.packageName,
            cls = info.activityInfo.name
        )
    }
}

private fun launchProcessText(context: Context, app: ProcessTextApp, text: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_PROCESS_TEXT).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_PROCESS_TEXT, text)
            putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
            component = ComponentName(app.pkg, app.cls)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    } catch (_: Exception) { /* Activity unavailable */ }
}
