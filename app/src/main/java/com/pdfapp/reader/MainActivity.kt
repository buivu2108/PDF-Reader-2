package com.pdfapp.reader

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.pdfapp.reader.domain.model.ThemeMode
import com.pdfapp.reader.ui.navigation.AppNavGraph
import com.pdfapp.reader.ui.theme.AppTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val appContainer get() = (application as App).appContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Restore saved language on startup
        lifecycleScope.launch {
            val savedLang = appContainer.preferences.selectedLanguage.first()
            if (savedLang.isNotEmpty()) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(savedLang))
            }
        }

        lifecycleScope.launch {
            appContainer.preferences.keepScreenOn.collectLatest { keepOn ->
                if (keepOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }

        setContent {
            val themeModeFlow = remember {
                appContainer.preferences.themeMode.map { ThemeMode.fromString(it) }
            }
            val themeMode by themeModeFlow.collectAsStateWithLifecycle(ThemeMode.SYSTEM)

            AppTheme(themeMode = themeMode) {
                AppNavGraph(appContainer = appContainer)
            }
        }
    }
}
