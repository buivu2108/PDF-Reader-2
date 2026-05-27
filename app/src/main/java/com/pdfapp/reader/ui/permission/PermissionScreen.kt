package com.pdfapp.reader.ui.permission

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.data.preferences.AppPreferences
import com.pdfapp.reader.ui.theme.Accent

/**
 * Reusable permission request screen.
 * [isStorage] = true for storage permission, false for camera permission.
 * Camera screen passes [preferences] to mark first-launch complete on completion.
 */
@Composable
fun PermissionScreen(
    isStorage: Boolean,
    preferences: AppPreferences? = null,
    onGranted: () -> Unit,
    onSkip: () -> Unit
) {
    val vm: PermissionViewModel = viewModel(
        key = if (isStorage) "storage" else "camera",
        factory = PermissionViewModel.Factory(if (isStorage) null else preferences)
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Standard permission launcher (READ_EXTERNAL_STORAGE on API < 30, or CAMERA)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> vm.onPermissionResult(granted) }

    // MANAGE_EXTERNAL_STORAGE launcher (API 30+): opens Settings, check on return
    val manageStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            vm.onPermissionResult(Environment.isExternalStorageManager())
        }
    }

    // Auto-grant if MANAGE_EXTERNAL_STORAGE already enabled
    LaunchedEffect(Unit) {
        if (isStorage) vm.autoGrantIfNotNeeded()
    }

    // Navigate when VM signals completion (handles both granted and skip paths)
    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onGranted()
    }

    val imageRes = if (isStorage) R.drawable.permission_storage else R.drawable.permission_camera
    val titleRes = if (isStorage) R.string.permission_storage_title else R.string.permission_camera_title
    val descRes = if (isStorage) R.string.permission_storage_desc else R.string.permission_camera_desc
    val primaryRes = if (isStorage) R.string.permission_storage_allow else R.string.permission_camera_allow
    val secondaryRes = if (isStorage) R.string.permission_storage_deny else R.string.permission_camera_skip

    val permission = if (isStorage) vm.getStoragePermission()
    else android.Manifest.permission.CAMERA

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Accent.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(72.dp)
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(descRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))

        Button(
            onClick = {
                if (isStorage && vm.needsManageStorage()) {
                    // API 30+: open Settings for MANAGE_EXTERNAL_STORAGE
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    manageStorageLauncher.launch(intent)
                } else {
                    permissionLauncher.launch(permission)
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            Text(stringResource(primaryRes), fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                if (isStorage) onSkip() // Storage "Not Now" just skips to next screen
                else vm.skip()          // Camera "Skip" marks first-launch done
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(secondaryRes), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
