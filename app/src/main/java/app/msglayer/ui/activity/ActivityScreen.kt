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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.msglayer.core.common.TimeFormat
import app.msglayer.ui.ActivityViewModel
import app.msglayer.ui.components.QuietPanel
import app.msglayer.ui.components.ScreenHeader
import app.msglayer.ui.theme.TextSecondary

@Composable
fun ActivityScreen(
    onBack: () -> Unit = {},
    vm: ActivityViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenHeader(
                title = "Activity",
                subtitle = "What the organizer did — undo when possible",
                onBack = onBack
            )
        }
        items(state.actions) { action ->
            QuietPanel {
                Text(
                    TimeFormat.relative(System.currentTimeMillis(), action.timestamp),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Text(action.description, style = MaterialTheme.typography.bodyLarge)
                if (action.undone) {
                    Text("Undone", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                } else if (action.undoable) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { vm.undo(action.id) }) { Text("Undo") }
                    }
                }
            }
        }
    }
}
