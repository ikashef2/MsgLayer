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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.msglayer.ui.RulesViewModel
import app.msglayer.ui.components.QuietPanel
import app.msglayer.ui.components.ScreenHeader
import app.msglayer.ui.components.SectionLabel
import app.msglayer.ui.theme.Accent
import app.msglayer.ui.theme.TextSecondary

@Composable
fun RulesScreen(
    onBack: () -> Unit = {},
    vm: RulesViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "Rules",
                subtitle = "Non-destructive — never permanent delete",
                onBack = onBack
            )
            SectionLabel("Active")
        }
        items(state.rules) { rule ->
            QuietPanel {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        rule.naturalLanguage,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = rule.enabled,
                        onCheckedChange = { vm.toggle(rule.id) },
                        colors = SwitchDefaults.colors(checkedTrackColor = Accent)
                    )
                }
                Text(rule.conditionsSummary, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
