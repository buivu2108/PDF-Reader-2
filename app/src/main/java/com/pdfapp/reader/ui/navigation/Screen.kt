package com.pdfapp.reader.ui.navigation

/** All navigation destinations in the app. */
sealed class Screen(val route: String) {

    // First-launch flow
    object Splash : Screen("splash")
    object Language : Screen("language")
    object Onboarding : Screen("onboarding")
    object PermissionStorage : Screen("permission_storage")
    object PermissionCamera : Screen("permission_camera")

    // Bottom nav destinations (main app shell)
    object Home : Screen("home")
    object Tools : Screen("tools")
    object Settings : Screen("settings")

    // Deep routes with arguments
    object PdfViewer : Screen("pdf_viewer/{encodedUri}") {
        fun createRoute(encodedUri: String) = "pdf_viewer/$encodedUri"
        const val ARG_URI = "encodedUri"
    }

    object EditMode : Screen("edit_mode/{encodedUri}") {
        fun createRoute(encodedUri: String, pageIndex: Int? = null): String {
            val base = "edit_mode/$encodedUri"
            return if (pageIndex != null) "$base?page=$pageIndex" else base
        }
        const val ARG_URI = "encodedUri"
    }

    // Tool screens
    object ScanToPdf : Screen("tools/scan")
    object ImageToPdf : Screen("tools/image_to_pdf")
    object SplitPdf : Screen("tools/split")
    object LockPdf : Screen("tools/lock")
    object UnlockPdf : Screen("tools/unlock")
    object PdfToImage : Screen("tools/pdf_to_image")
    object ExtractText : Screen("tools/extract_text")
    object MergePdf : Screen("tools/merge")
    object CompressPdf : Screen("tools/compress")
    object PageManager : Screen("tools/page_manager")
}
