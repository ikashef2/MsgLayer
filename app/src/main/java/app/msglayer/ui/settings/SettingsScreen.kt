package app.msglayer.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.msglayer.SourceMode
import app.msglayer.core.common.TimeFormat
import app.msglayer.ui.SettingsViewModel
import app.msglayer.ui.components.GroupCard
import app.msglayer.ui.components.ListRow
import app.msglayer.ui.components.QuietPanel
import app.msglayer.ui.components.ScreenHeader
import app.msglayer.ui.components.SectionLabel
import app.msglayer.ui.components.VerticalSpacer
import app.msglayer.ui.theme.Accent
import app.msglayer.ui.theme.TextSecondary
import app.msglayer.ui.theme.Warning
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onRules: () -> Unit,
    onActivity: () -> Unit,
    vm: SettingsViewModel = viewModel()
) {
    val status by vm.status.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> vm.onPermissionResult(granted) }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(3500)
            vm.clearToast()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader(
                title = "Settings",
                subtitle = "Sources, privacy, organization",
                onBack = onBack
            )
            toast?.let {
                Text(it, color = Accent, style = MaterialTheme.typography.bodyMedium)
            }
        }

        item { SectionLabel("Message sources") }
        item {
            QuietPanel {
                Text(
                    if (status.mode == SourceMode.DEVICE_SMS) "Device SMS" else "Mock SMS",
                    style = MaterialTheme.typography.titleLarge
                )
                VerticalSpacer(4)
                Text(
                    "${status.messageCount} messages indexed" +
                        (status.lastSyncAt?.let {
                            " · synced ${TimeFormat.relative(System.currentTimeMillis(), it)}"
                        } ?: ""),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                status.lastError?.let {
                    VerticalSpacer(4)
                    Text(it, color = Warning, style = MaterialTheme.typography.bodyMedium)
                }
                VerticalSpacer(14)
                if (!status.hasSmsPermission) {
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) { Text("Grant SMS permission") }
                    VerticalSpacer(8)
                }
                Button(
                    onClick = { vm.syncDeviceSms() },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) { Text(if (busy) "Syncing…" else "Sync Device SMS") }
                VerticalSpacer(8)
                OutlinedButton(
                    onClick = { vm.useMock() },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Use Mock SMS") }
            }
        }

        item { SectionLabel("Organization") }
        item {
            GroupCard {
                ListRow("Organization rules", onClick = onRules, showDivider = true)
                ListRow("Activity history", onClick = onActivity)
            }
        }

        item { SectionLabel("AI & privacy") }
        item {
            GroupCard {
                ListRow(
                    title = "Local retrieval",
                    subtitle = "No cloud upload. AiProvider is swappable.",
                    showDivider = true
                )
                ListRow(
                    title = "On-device only",
                    subtitle = "Never permanently deletes SMS."
                )
            }
        }
    }
}
