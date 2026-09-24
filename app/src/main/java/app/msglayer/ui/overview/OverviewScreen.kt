package app.msglayer.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.msglayer.core.common.MoneyNormalizer
import app.msglayer.core.common.TimeFormat
import app.msglayer.ui.OverviewViewModel
import app.msglayer.ui.components.KeyValueRow
import app.msglayer.ui.components.QuietPanel
import app.msglayer.ui.components.SectionLabel
import app.msglayer.ui.components.VerticalSpacer
import app.msglayer.ui.theme.Accent
import app.msglayer.ui.theme.TextSecondary
import app.msglayer.ui.theme.Warning

@Composable
fun OverviewScreen(
    onAsk: () -> Unit,
    onOpenMessage: (String) -> Unit,
    onOpenActivity: () -> Unit,
    vm: OverviewViewModel = hiltViewModel()
) {
    val snap by vm.overview.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(snap.greeting, style = MaterialTheme.typography.displaySmall)
            VerticalSpacer(4)
            Text(
                "${snap.attentionCount} things need attention",
                style = MaterialTheme.typography.bodyLarge,
                color = Warning
            )
        }

        item {
            SectionLabel("Needs Attention")
            QuietPanel {
                snap.attentionItems.take(3).forEachIndexed { i, item ->
                    if (i > 0) VerticalSpacer(10)
                    Column(Modifier.fillMaxWidth()) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(item.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(TimeFormat.relative(System.currentTimeMillis(), item.timestamp), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    }
                }
            }
        }

        item {
            SectionLabel("Finance")
            QuietPanel {
                snap.accounts.forEach { acc ->
                    KeyValueRow(
                        "Latest · ${acc.bankName}",
                        acc.lastKnownBalanceToman?.let { MoneyNormalizer.formatToman(it) } ?: "—"
                    )
                    VerticalSpacer(6)
                }
                KeyValueRow("Spent today", MoneyNormalizer.formatToman(snap.spentTodayToman))
                snap.knownTotalBalanceToman?.let { total ->
                    VerticalSpacer(6)
                    KeyValueRow("Known total", MoneyNormalizer.formatToman(total))
                }
            }
        }

        item {
            SectionLabel("Orders")
            QuietPanel {
                Text(
                    if (snap.ordersArrivingToday > 0) "${snap.ordersArrivingToday} arriving today" else "No deliveries today",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        item {
            SectionLabel("What changed?")
            QuietPanel {
                Text("Since yesterday", style = MaterialTheme.typography.titleMedium, color = Accent)
                VerticalSpacer(8)
                snap.changeEvents.forEach {
                    Text("· ${it.text}", style = MaterialTheme.typography.bodyMedium)
                    VerticalSpacer(4)
                }
            }
        }

        item {
            SectionLabel("Cleaned up")
            QuietPanel {
                snap.cleanedUp.forEach {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("${it.count} ${it.title}", style = MaterialTheme.typography.titleMedium)
                            Text(it.reason, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        TextButton(onClick = { it.messageIds.firstOrNull()?.let(onOpenMessage) }) {
                            Text("View")
                        }
                    }
                    VerticalSpacer(6)
                }
            }
        }

        item {
            QuietPanel(onClick = onAsk) {
                Text("Ask anything about your messages", style = MaterialTheme.typography.titleMedium, color = Accent)
                Text("Retrieval over your indexed history — with sources", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onOpenActivity) { Text("Activity log") }
            }
        }
    }
}
