package app.msglayer.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.msglayer.SourceMode
import app.msglayer.core.common.TimeFormat
import app.msglayer.ui.SettingsViewModel
import app.msglayer.ui.components.QuietPanel
import app.msglayer.ui.components.SectionLabel
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("Back") }
            }
            Text("Settings", style = MaterialTheme.typography.displaySmall)
            Text(
                "Sources, privacy, and organization — non-destructive.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            toast?.let {
                Text(it, color = Accent, style = MaterialTheme.typography.bodyMedium)
            }
        }

        item { SectionLabel("Message sources") }
        item {
            QuietPanel {
                Text(
                    if (status.mode == SourceMode.DEVICE_SMS) "Active: Device SMS" else "Active: Mock SMS",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${status.messageCount} messages indexed" +
                        (status.lastSyncAt?.let { " · synced ${TimeFormat.relative(System.currentTimeMillis(), it)}" } ?: ""),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                status.lastError?.let {
                    Text(it, color = Warning, style = MaterialTheme.typography.bodyMedium)
                }
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!status.hasSmsPermission) {
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Grant SMS permission") }
                    } else {
                        Text("SMS permission granted", color = Accent, style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        onClick = { vm.syncDeviceSms() },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (busy) "Syncing…" else "Sync Device SMS") }
                    OutlinedButton(
                        onClick = { vm.useMock() },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Use Mock SMS (demo)") }
                }
            }
        }

        item { SectionLabel("Organization") }
        item { QuietPanel(onClick = onRules) { Text("Organization rules") } }
        item { QuietPanel(onClick = onActivity) { Text("Activity history") } }

        item { SectionLabel("AI") }
        item {
            QuietPanel {
                Text("Local retrieval provider", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Answers from indexed messages on-device. No cloud upload. AiProvider is swappable later.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item { SectionLabel("Privacy") }
        item {
            QuietPanel {
                Text("Device messages stay on device.", style = MaterialTheme.typography.titleMedium)
                Text(
                    "MsgLayer never permanently deletes SMS. READ_SMS is used only to index.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
