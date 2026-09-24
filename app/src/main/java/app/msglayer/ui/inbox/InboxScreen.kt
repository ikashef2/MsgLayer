package app.msglayer.ui.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.msglayer.core.common.TimeFormat
import app.msglayer.data.repository.InboxFilter
import app.msglayer.ui.InboxViewModel
import app.msglayer.ui.components.QuietPanel
import app.msglayer.ui.components.SectionLabel
import app.msglayer.ui.theme.TextSecondary

@Composable
fun InboxScreen(
    onOpenMessage: (String) -> Unit,
    vm: InboxViewModel = hiltViewModel()
) {
    val filter by vm.filterState.collectAsStateWithLifecycle()
    val stateTick by vm.messages.collectAsStateWithLifecycle()
    val list = remember(filter, stateTick) { vm.refreshMessages() }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        SectionLabel("Inbox")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
            InboxFilter.entries.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { vm.setFilter(f) },
                    label = { Text(f.name.lowercase().replaceFirstChar { it.titlecase() }) }
                )
            }
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(list, key = { it.id }) { msg ->
                QuietPanel(onClick = { onOpenMessage(msg.id) }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(vm.senderName(msg.senderId), style = MaterialTheme.typography.titleMedium)
                        Text(
                            TimeFormat.relative(System.currentTimeMillis(), msg.timestamp),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                    Text(
                        msg.body.lines().firstOrNull().orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 2
                    )
                    val labels = msg.classification?.labels?.joinToString(" · ") { it.label.name.lowercase() }.orEmpty()
                    if (labels.isNotEmpty()) {
                        Text(labels, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    }
                }
            }
        }
    }
}
