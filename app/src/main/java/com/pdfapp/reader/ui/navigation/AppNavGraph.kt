package com.pdfapp.reader.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Create
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pdfapp.reader.AppContainer
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.ui.home.HomeScreen
import com.pdfapp.reader.ui.home.HomeViewModel
import com.pdfapp.reader.ui.language.LanguageScreen
import com.pdfapp.reader.ui.onboarding.OnboardingScreen
import com.pdfapp.reader.ui.permission.PermissionScreen
import com.pdfapp.reader.ui.settings.SettingsScreen
import com.pdfapp.reader.ui.settings.SettingsViewModel
import com.pdfapp.reader.ui.splash.SplashScreen
import com.pdfapp.reader.ui.tools.ToolsScreen
import com.pdfapp.reader.ui.tools.imagetopdf.ImageToPdfScreen
import com.pdfapp.reader.ui.tools.imagetopdf.ImageToPdfViewModel
import com.pdfapp.reader.ui.tools.lock.LockPdfScreen
import com.pdfapp.reader.ui.tools.lock.LockPdfViewModel
import com.pdfapp.reader.ui.tools.scan.ScanToPdfScreen
import com.pdfapp.reader.ui.tools.split.SplitPdfScreen
import com.pdfapp.reader.ui.tools.split.SplitPdfViewModel
import com.pdfapp.reader.ui.tools.unlock.UnlockPdfScreen
import com.pdfapp.reader.ui.tools.unlock.UnlockPdfViewModel
import com.pdfapp.reader.ui.tools.pdftoimage.PdfToImageScreen
import com.pdfapp.reader.ui.tools.pdftoimage.PdfToImageViewModel
import com.pdfapp.reader.ui.tools.extracttext.ExtractTextScreen
import com.pdfapp.reader.ui.tools.extracttext.ExtractTextViewModel
import com.pdfapp.reader.ui.tools.merge.MergePdfScreen
import com.pdfapp.reader.ui.tools.merge.MergePdfViewModel
import com.pdfapp.reader.ui.tools.compress.CompressPdfScreen
import com.pdfapp.reader.ui.tools.compress.CompressPdfViewModel
import com.pdfapp.reader.ui.tools.pagemanager.PageManagerScreen
import com.pdfapp.reader.ui.tools.pagemanager.PageManagerViewModel
import com.pdfapp.reader.ui.editmode.EditModeCoordinator
import com.pdfapp.reader.ui.editmode.EditModeScreen
import com.pdfapp.reader.domain.usecase.ExtractTextBlocksUseCase
import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfapp.reader.ui.viewer.ComposePdfViewerScreen
import com.pdfapp.reader.ui.viewer.PdfViewerViewModel
import com.pdfapp.reader.util.toDecodedUri
import com.pdfapp.reader.util.toEncodedUri

private const val NAV_TAG = "AppNavGraph"

/** Resolve a navigation argument to a URI, handling both auto-decoded and raw encoded values. */
private fun resolveNavUri(rawArg: String): Uri {
    val parsed = Uri.parse(rawArg)
    return if (parsed.scheme == "content" || parsed.scheme == "file") {
        parsed // Already decoded by Navigation
    } else {
        Uri.parse(rawArg.toDecodedUri()) // Still encoded — decode once
    }
}

private val bottomNavItems = listOf(
    Triple(Screen.Home, Icons.Default.Home, R.string.nav_home),
    Triple(Screen.Tools, Icons.Default.Create, R.string.nav_tools),
    Triple(Screen.Settings, Icons.Default.Settings, R.string.nav_settings)
)

@Composable
fun AppNavGraph(appContainer: AppContainer) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current

    val showBottomBar = currentDestination?.hierarchy?.any { dest ->
        bottomNavItems.any { (screen, _, _) -> dest.route == screen.route }
    } == true

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    preferences = appContainer.preferences,
                    onNavigateToLanguage = { navController.navigate(Screen.Language.route) { popUpTo(Screen.Splash.route) { inclusive = true } } },
                    onNavigateToHome = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Splash.route) { inclusive = true } } },
                    onNavigateToPermission = { navController.navigate(Screen.PermissionStorage.route) { popUpTo(Screen.Splash.route) { inclusive = true } } }
                )
            }
            composable(Screen.Language.route) {
                LanguageScreen(
                    preferences = appContainer.preferences,
                    onContinue = { navController.navigate(Screen.Onboarding.route) }
                )
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinish = { navController.navigate(Screen.PermissionStorage.route) }
                )
            }
            composable(Screen.PermissionStorage.route) {
                PermissionScreen(
                    isStorage = true,
                    onGranted = { navController.navigate(Screen.PermissionCamera.route) },
                    onSkip = { navController.navigate(Screen.PermissionCamera.route) }
                )
            }
            composable(Screen.PermissionCamera.route) {
                PermissionScreen(
                    isStorage = false,
                    preferences = appContainer.preferences,
                    onGranted = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Splash.route) { inclusive = true } } },
                    onSkip = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Splash.route) { inclusive = true } } }
                )
            }

            // Home
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(appContainer.pdfFileRepository, context)
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onFileClick = { file ->
                        Log.d(NAV_TAG, "onFileClick: file.uri=${file.uri}")
                        val encoded = file.uri.toEncodedUri()
                        Log.d(NAV_TAG, "onFileClick: encoded=$encoded")
                        navController.navigate(Screen.PdfViewer.createRoute(encoded))
                    }
                )
            }

            // Tools
            composable(Screen.Tools.route) {
                ToolsScreen(onToolClick = { route ->
                    when (route) {
                        "scan" -> navController.navigate(Screen.ScanToPdf.route)
                        "image_to_pdf" -> navController.navigate(Screen.ImageToPdf.route)
                        "split" -> navController.navigate(Screen.SplitPdf.route)
                        "lock" -> navController.navigate(Screen.LockPdf.route)
                        "unlock" -> navController.navigate(Screen.UnlockPdf.route)
                        "pdf_to_image" -> navController.navigate(Screen.PdfToImage.route)
                        "extract_text" -> navController.navigate(Screen.ExtractText.route)
                        "merge" -> navController.navigate(Screen.MergePdf.route)
                        "compress" -> navController.navigate(Screen.CompressPdf.route)
                        "page_manager" -> navController.navigate(Screen.PageManager.route)
                    }
                })
            }

            // Tool screens
            composable(Screen.ScanToPdf.route) {
                ScanToPdfScreen(
                    adManager = appContainer.adManager,
                    onBack = { navController.popBackStack() },
                    onOpenPdf = { uriString ->
                        val encoded = uriString.toEncodedUri()
                        navController.navigate(Screen.PdfViewer.createRoute(encoded))
                    }
                )
            }
            composable(Screen.ImageToPdf.route) {
                val vm: ImageToPdfViewModel = viewModel(factory = ImageToPdfViewModel.Factory(context))
                ImageToPdfScreen(
                    viewModel = vm,
                    adManager = appContainer.adManager,
                    onBack = { navController.popBackStack() },
                    onOpenPdf = { uriString ->
                        val encoded = uriString.toEncodedUri()
                        navController.navigate(Screen.PdfViewer.createRoute(encoded))
                    }
                )
            }
            composable(Screen.SplitPdf.route) {
                val vm: SplitPdfViewModel = viewModel(factory = SplitPdfViewModel.Factory(context))
                SplitPdfScreen(
                    viewModel = vm,
                    adManager = appContainer.adManager,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.LockPdf.route) {
                val vm: LockPdfViewModel = viewModel(factory = LockPdfViewModel.Factory(context))
                LockPdfScreen(
                    viewModel = vm,
                    adManager = appContainer.adManager,
                    onBack = { navController.popBackStack() },
                    onOpenPdf = { uriString ->
                        val encoded = uriString.toEncodedUri()
                        navController.navigate(Screen.PdfViewer.createRoute(encoded))
                    }
                )
            }
            composable(Screen.UnlockPdf.route) {
                val vm: UnlockPdfViewModel = viewModel(factory = UnlockPdfViewModel.Factory(context))
                UnlockPdfScreen(
                    viewModel = vm,
                    adManager = appContainer.adManager,
                    onBack = { navController.popBackStack() },
                    onOpenPdf = { uriString ->
                        val encoded = uriString.toEncodedUri()
                        navController.navigate(Screen.PdfViewer.createRoute(encoded))
                    }
                )
            }
            composable(Screen.PdfToImage.route) {
                val vm: PdfToImageViewModel = viewModel(factory = PdfToImageViewModel.Factory(context))
                PdfToImageScreen(
                    viewModel = vm,
                    adManager = appContainer.adManager,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.ExtractText.route) {
                val vm: ExtractTextViewModel = viewModel(factory = ExtractTextViewModel.Factory(context))
                ExtractTextScreen(
                    viewModel = vm,
                    adManager = appContainer.adManager,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.MergePdf.route) {
                val vm: MergePdfViewModel = viewModel(factory = MergePdfViewModel.Factory(context))
                MergePdfScreen(
                    viewModel = vm,
                    adManager = appContainer.adManager,
                    onBack = { navController.popBackStack() },
                    onOpenPdf = { uriString ->
                        val encoded = uriString.toEncodedUri()
                        navController.navigate(Screen.PdfViewer.createRoute(encoded))
                    }
                )
            }
            composable(Screen.CompressPdf.route) {
                val vm: CompressPdfViewModel = viewModel(factory = CompressPdfViewModel.Factory(context))
                CompressPdfScreen(
                    viewModel = vm,
                    adManager = appContainer.adManager,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.PageManager.route) {
                val vm: PageManagerViewModel = viewModel(factory = PageManagerViewModel.Factory(context))
                PageManagerScreen(
                    viewModel = vm,
                    adManager = appContainer.adManager,
                    onBack = { navController.popBackStack() },
                    onOpenPdf = { uriString ->
                        val encoded = uriString.toEncodedUri()
                        navController.navigate(Screen.PdfViewer.createRoute(encoded))
                    }
                )
            }

            // Settings
            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(appContainer.settingsRepository)
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToLanguage = { navController.navigate(Screen.Language.route) }
                )
            }

            // PDF Viewer
            composable(
                route = Screen.PdfViewer.route,
                arguments = listOf(navArgument(Screen.PdfViewer.ARG_URI) { type = NavType.StringType })
            ) { backStackEntry ->
                val rawArg = backStackEntry.arguments?.getString(Screen.PdfViewer.ARG_URI) ?: ""
                val uri = resolveNavUri(rawArg)
                val fileName = uri.lastPathSegment ?: "PDF"
                Log.d(NAV_TAG, "PdfViewer: uri=$uri, fileName=$fileName")

                val viewerViewModel: PdfViewerViewModel = viewModel(
                    factory = PdfViewerViewModel.Factory(context, appContainer)
                )

                // Observe saved PDF result from EditMode via savedStateHandle
                val savedPdfUri = backStackEntry.savedStateHandle
                    .getStateFlow<String?>("saved_pdf_uri", null)
                    .collectAsStateWithLifecycle()
                LaunchedEffect(savedPdfUri.value) {
                    savedPdfUri.value?.let { uriStr ->
                        viewerViewModel.loadPdf(Uri.parse(uriStr))
                        backStackEntry.savedStateHandle.remove<String>("saved_pdf_uri")
                    }
                }

                ComposePdfViewerScreen(
                    uri = uri,
                    fileName = fileName,
                    viewModel = viewerViewModel,
                    onBack = { navController.popBackStack() },
                    onEditMode = { pageIndex, _ ->
                        val encoded = uri.toString().toEncodedUri()
                        navController.navigate(Screen.EditMode.createRoute(
                            encoded,
                            pageIndex = pageIndex
                        ))
                    }
                )
            }

            // Edit Mode (redesigned 3-tab: Mark, Annotate, Fill & Sign)
            composable(
                route = Screen.EditMode.route + "?page={page}",
                arguments = listOf(
                    navArgument(Screen.EditMode.ARG_URI) { type = NavType.StringType },
                    navArgument("page") { type = NavType.IntType; defaultValue = -1 }
                )
            ) { backStackEntry ->
                val rawArg = backStackEntry.arguments?.getString(Screen.EditMode.ARG_URI) ?: ""
                val uri = resolveNavUri(rawArg)
                val pageIndex = backStackEntry.arguments?.getInt("page", -1) ?: -1
                val initialPage = if (pageIndex >= 0) pageIndex else null

                val context = LocalContext.current
                val appContainer = (context.applicationContext as com.pdfapp.reader.App).appContainer
                val coordinator = remember { EditModeCoordinator() }
                val extractTextBlocksUseCase = remember { ExtractTextBlocksUseCase(context) }
                EditModeScreen(
                    uri = uri,
                    coordinator = coordinator,
                    extractTextBlocksUseCase = extractTextBlocksUseCase,
                    signatureRepository = appContainer.signatureRepository,
                    saveAnnotatedPdfUseCase = appContainer.saveAnnotatedPdfUseCase,
                    onBack = { navController.popBackStack() },
                    onSaveComplete = { savedUri ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("saved_pdf_uri", savedUri.toString())
                        navController.popBackStack()
                    },
                    initialPageIndex = initialPage
                )
            }
        }

        if (showBottomBar) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(50),
                        ambientColor = Color.Black.copy(alpha = 0.1f),
                        spotColor = Color.Black.copy(alpha = 0.3f)
                    )
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                    .border(
                        1.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                        RoundedCornerShape(50)
                    )
            ) {
                NavigationBar(
                    modifier = Modifier.fillMaxWidth().height(65.dp),
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0)
                ) {
                    bottomNavItems.forEach { (screen, icon, labelRes) ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp)) },
                            label = { Text(stringResource(labelRes), fontSize = 10.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}
