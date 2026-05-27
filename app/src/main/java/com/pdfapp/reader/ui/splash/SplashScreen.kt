package com.pdfapp.reader.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.data.preferences.AppPreferences
import com.pdfapp.reader.ui.theme.Accent

@Composable
fun SplashScreen(
    preferences: AppPreferences,
    onNavigateToLanguage: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToPermission: () -> Unit = {}
) {
    val vm: SplashViewModel = viewModel(factory = SplashViewModel.Factory(preferences))
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigateTo) {
        when (state.navigateTo) {
            SplashDestination.LANGUAGE -> onNavigateToLanguage()
            SplashDestination.HOME -> onNavigateToHome()
            SplashDestination.PERMISSION_STORAGE -> onNavigateToPermission()
            SplashDestination.NONE -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.splash_logo),
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.splash_title_pdf))
                    append(" ")
                    withStyle(SpanStyle(color = Accent)) {
                        append(stringResource(R.string.splash_title_reader))
                    }
                },
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.splash_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (state.isLoading) {
            CircularProgressIndicator(
                color = Accent,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp)
            )
        }
    }
}
