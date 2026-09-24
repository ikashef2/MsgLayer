package app.msglayer.ui.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.modifier.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.msglayer.ui.RulesViewModel
import app.msglayer.ui.components.QuietPanel
import app.msglayer.ui.components.SectionLabel
import app.msglayer.ui.theme.TextSecondary

@Composable
fun RulesScreen(vm: RulesViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Organization rules", style = MaterialTheme.typography.displaySmall)
            Text("Non-destructive only — never permanent delete.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            SectionLabel("Active")
        }
        items(state.rules) { rule ->
            QuietPanel {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(rule.naturalLanguage, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = rule.enabled, onCheckedChange = { vm.toggle(rule.id) })
                }
                Text(rule.conditionsSummary, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Text(rule.actions.joinToString { it.name.lowercase() }, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }
        }
    }
}
