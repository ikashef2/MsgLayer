package app.msglayer.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.msglayer.ui.components.QuietPanel
import app.msglayer.ui.components.SectionLabel
import app.msglayer.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    onRules: () -> Unit,
    onActivity: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.displaySmall)
            Text("Privacy-first · local mock source for MVP", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        item { SectionLabel("Organization") }
        item { QuietPanel(onClick = onRules) { Text("Organization rules") } }
        item { QuietPanel(onClick = onActivity) { Text("Activity history") } }
        item { SectionLabel("Message sources") }
        item {
            QuietPanel {
                Text("Mock SMS", style = MaterialTheme.typography.titleMedium)
                Text("Active · canRead only · never auto-deletes", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item { SectionLabel("AI") }
        item {
            QuietPanel {
                Text("Local retrieval provider", style = MaterialTheme.typography.titleMedium)
                Text("No cloud upload by default. AiProvider is swappable.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
