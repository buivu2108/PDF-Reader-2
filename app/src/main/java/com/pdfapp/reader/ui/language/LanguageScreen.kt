package com.pdfapp.reader.ui.language

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.data.preferences.AppPreferences
import com.pdfapp.reader.domain.model.AppLanguage
import com.pdfapp.reader.ui.theme.Accent

@Composable
fun LanguageScreen(
    preferences: AppPreferences,
    onContinue: () -> Unit
) {
    val vm: LanguageViewModel = viewModel(factory = LanguageViewModel.Factory(preferences))
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.confirmed) {
        if (state.confirmed) onContinue()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(56.dp))
        Text(
            text = stringResource(R.string.language_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.language_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.languages) { language ->
                LanguageItem(
                    language = language,
                    isSelected = language == state.selectedLanguage,
                    onClick = { vm.selectLanguage(language) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { vm.confirmSelection() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            Text(
                text = stringResource(R.string.language_continue),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun LanguageItem(
    language: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = language.emojiFlag, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = language.nativeName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = language.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Accent)
        )
    }
}
