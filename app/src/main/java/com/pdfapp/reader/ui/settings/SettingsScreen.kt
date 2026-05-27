package com.pdfapp.reader.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pdfapp.reader.domain.model.PageMode
import com.pdfapp.reader.prefers.AppPrefs
import com.vtsoft.pdfapp.reader.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToLanguage: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val isDark = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isDark) {
                    SolidColor(Color(0xFF40226D))
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFDFCAFF),
                            Color(0xFFF2EAFC),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                }
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
            ) {
                // --- Top avatar + banner area (center avatar, banner below) ---
                val lifecycleOwner = LocalLifecycleOwner.current

                // initial read from AppPrefs
                var posAvatar by remember { mutableStateOf(AppPrefs.get().applyAvatarPosition) }
                var posBanner by remember { mutableStateOf(AppPrefs.get().applyBannerPosition) }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            // Refresh positions when screen resumes (after returning from AvatarBannerActivity)
                            posAvatar = AppPrefs.get().applyAvatarPosition
                            posBanner = AppPrefs.get().applyBannerPosition
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar: centered, 60x60; avatarRes handles -1 -> default drawable
                    Image(
                        painter = painterResource(id = avatarRes(posAvatar)),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Banner: full width, height = 60dp; bannerRes handles -1 -> default drawable
                    Image(
                        painter = painterResource(id = bannerRes(posBanner)),
                        contentDescription = "Banner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(100.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                // --- end top area ---

                SectionHeader(text = stringResource(R.string.settings_general))
                Spacer(Modifier.height(8.dp))
                GeneralSection(
                    languageName = state.language.displayName,
                    onNavigateToLanguage = onNavigateToLanguage,
                    readerMode = state.readerMode,
                    onReaderModeSelect = viewModel::setReaderMode,
                    keepScreenOn = state.keepScreenOn,
                    onKeepScreenOnChange = viewModel::setKeepScreenOn
                )
                Spacer(Modifier.height(16.dp))
                SectionHeader(text = stringResource(R.string.settings_about))
                AboutSection(context)
                Spacer(Modifier.height(130.dp))
            }
        }
    }
}

@Composable
private fun GeneralSection(
    languageName: String,
    onNavigateToLanguage: () -> Unit,
    readerMode: PageMode,
    onReaderModeSelect: (PageMode) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit
) {
    SettingsSectionCard {
        SettingsClickRow(
            icon = Icons.Default.Settings,
            label = stringResource(R.string.settings_language),
            value = languageName,
            iconTint = Color(0xFF007AFF), // Vibrant iOS blue
            onClick = onNavigateToLanguage
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        Text(
            text = stringResource(R.string.settings_reader_mode),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
        )
        listOf(PageMode.CONTINUOUS, PageMode.SINGLE).forEach { mode ->
            val label = when (mode) {
                PageMode.CONTINUOUS -> stringResource(R.string.settings_continuous)
                PageMode.SINGLE -> stringResource(R.string.settings_single_page)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReaderModeSelect(mode) }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = readerMode == mode, onClick = { onReaderModeSelect(mode) })
                Spacer(Modifier.width(8.dp))
                Text(text = label, style = MaterialTheme.typography.bodyMedium)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        SettingsSwitchRow(
            icon = Icons.Default.Build,
            label = stringResource(R.string.settings_keep_screen_on),
            checked = keepScreenOn,
            iconTint = Color(0xFFFF9500), // Vibrant iOS orange
            onCheckedChange = onKeepScreenOnChange
        )
    }
}

@Composable
private fun AboutSection(context: android.content.Context) {
    // AboutSection no longer contains avatar/banner to avoid duplication.
    SettingsSectionCard {
        // Avatar & Banner item (mở AvatarBannerActivity)
        SettingsClickRow(
            icon = Icons.Default.Person,
            label = "Avatar & Banner",
            iconTint = Color(0xFF007AFF),
            onClick = {
                context.startActivity(
                    Intent(
                        context,
                        com.pdfapp.reader.avatar.AvatarBannerActivity::class.java
                    )
                )
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Buy More Point item (mở PointListActivity)
        SettingsClickRow(
            icon = Icons.Default.CreditCard,
            label = "Buy More Point",
            iconTint = Color(0xFF34C759),
            onClick = {
                context.startActivity(
                    Intent(
                        context,
                        com.pdfapp.reader.point.PointListActivity::class.java
                    )
                )
            }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        SettingsClickRow(
            icon = Icons.Default.Lock,
            label = stringResource(R.string.settings_privacy),
            iconTint = Color(0xFF5856D6), // Vibrant purple
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://sites.google.com/view/vtsoft-pdf-reader")
                )
                runCatching { context.startActivity(intent) }
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        SettingsClickRow(
            icon = Icons.Default.Info,
            label = stringResource(R.string.settings_version),
            iconTint = Color(0xFF32ADE6), // Vibrant cyan
            value = context.packageManager.getPackageInfo(context.packageName, 0).versionName
                ?: "1.0.0"
        )
    }
}

@androidx.annotation.DrawableRes
private fun avatarRes(pos: Int): Int = when (pos) {
    0 -> R.drawable.avatar_01
    1 -> R.drawable.avatar_02
    2 -> R.drawable.avatar_03
    3 -> R.drawable.avatar_04
    4 -> R.drawable.avatar_05
    5 -> R.drawable.avatar_06
    6 -> R.drawable.avatar_07
    else -> R.drawable.avatar_default
}

@androidx.annotation.DrawableRes
private fun bannerRes(pos: Int): Int = when (pos) {
    0 -> R.drawable.banner_01
    1 -> R.drawable.banner_02
    2 -> R.drawable.banner_03
    3 -> R.drawable.banner_04
    4 -> R.drawable.banner_05
    5 -> R.drawable.banner_06
    6 -> R.drawable.banner_07
    else -> R.drawable.banner_default
}