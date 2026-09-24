package app.msglayer.ui.inbox

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.msglayer.core.common.TimeFormat
import app.msglayer.data.repository.InboxFilter
import app.msglayer.ui.InboxViewModel
import app.msglayer.ui.components.QuietPanel
import app.msglayer.ui.components.ScreenHeader
import app.msglayer.ui.components.SoftAvatar
import app.msglayer.ui.components.SoftChip
import app.msglayer.ui.components.VerticalSpacer
import app.msglayer.ui.theme.TextSecondary

@Composable
fun InboxScreen(
    onOpenMessage: (String) -> Unit,
    vm: InboxViewModel = viewModel()
) {
    val filter by vm.filterState.collectAsStateWithLifecycle()
    val stateTick by vm.messages.collectAsStateWithLifecycle()
    val list = remember(filter, stateTick) { vm.refreshMessages() }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        ScreenHeader(title = "Inbox", subtitle = "${list.size} messages")
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InboxFilter.entries.forEach { f ->
                SoftChip(
                    label = f.name.lowercase().replaceFirstChar { it.titlecase() },
                    selected = filter == f,
                    onClick = { vm.setFilter(f) }
                )
            }
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(list, key = { it.id }) { msg ->
                val name = vm.senderName(msg.senderId)
                QuietPanel(onClick = { onOpenMessage(msg.id) }) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SoftAvatar(name)
                        Column(Modifier.weight(1f)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    TimeFormat.relative(System.currentTimeMillis(), msg.timestamp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                            VerticalSpacer(2)
                            Text(
                                msg.body.lines().firstOrNull().orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
