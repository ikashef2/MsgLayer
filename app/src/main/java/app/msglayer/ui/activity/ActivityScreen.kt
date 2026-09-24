package app.msglayer.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.msglayer.ui.UiModifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.msglayer.core.common.TimeFormat
import app.msglayer.ui.ActivityViewModel
import app.msglayer.ui.components.QuietPanel
import app.msglayer.ui.theme.TextSecondary

@Composable
fun ActivityScreen(vm: ActivityViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = UiModifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Activity", style = MaterialTheme.typography.displaySmall)
            Text("Everything the organizer did — undoable when possible.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        items(state.actions) { action ->
            QuietPanel {
                Text(TimeFormat.relative(System.currentTimeMillis(), action.timestamp), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Text(action.description, style = MaterialTheme.typography.bodyLarge)
                if (action.undone) {
                    Text("Undone", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                } else if (action.undoable) {
                    Row(UiModifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { vm.undo(action.id) }) { Text("Undo") }
                    }
                }
            }
        }
    }
}
