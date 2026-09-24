package app.msglayer.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.msglayer.core.common.MoneyNormalizer
import app.msglayer.core.common.TimeFormat
import app.msglayer.ui.OverviewViewModel
import app.msglayer.ui.components.GroupCard
import app.msglayer.ui.components.KeyValueRow
import app.msglayer.ui.components.ListRow
import app.msglayer.ui.components.QuietPanel
import app.msglayer.ui.components.ScreenHeader
import app.msglayer.ui.components.SectionLabel
import app.msglayer.ui.components.VerticalSpacer
import app.msglayer.ui.theme.Accent
import app.msglayer.ui.theme.TextSecondary

@Composable
fun OverviewScreen(
    onAsk: () -> Unit,
    onOpenMessage: (String) -> Unit,
    onOpenActivity: () -> Unit,
    onOpenSettings: () -> Unit,
    vm: OverviewViewModel = viewModel()
) {
    val snap by vm.overview.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            ScreenHeader(
                title = snap.greeting,
                subtitle = if (snap.attentionCount > 0) {
                    "${snap.attentionCount} things need attention"
                } else {
                    "All clear for now"
                },
                trailing = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Accent)
                    }
                }
            )
        }

        item {
            SectionLabel("Needs attention")
            GroupCard {
                if (snap.attentionItems.isEmpty()) {
                    ListRow(title = "Nothing urgent", subtitle = "You're caught up")
                } else {
                    snap.attentionItems.take(5).forEachIndexed { i, item ->
                        ListRow(
                            title = item.title,
                            subtitle = item.subtitle,
                            onClick = { onOpenMessage(item.messageId) },
                            showDivider = i < snap.attentionItems.take(5).lastIndex,
                            trailing = {
                                Text(
                                    TimeFormat.relative(System.currentTimeMillis(), item.timestamp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                        )
                    }
                }
            }
        }

        item {
            SectionLabel("Finance")
            QuietPanel {
                if (snap.accounts.isEmpty()) {
                    Text("No balances yet", style = MaterialTheme.typography.titleMedium)
                    Text("Sync SMS in Settings", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                } else {
                    snap.accounts.take(3).forEach { acc ->
                        KeyValueRow(
                            acc.bankName,
                            acc.lastKnownBalanceToman?.let { MoneyNormalizer.formatToman(it) } ?: "—"
                        )
                        VerticalSpacer(8)
                    }
                    KeyValueRow("Spent today", MoneyNormalizer.formatToman(snap.spentTodayToman))
                    snap.knownTotalBalanceToman?.let { total ->
                        VerticalSpacer(8)
                        KeyValueRow("Known total", MoneyNormalizer.formatToman(total))
                    }
                }
            }
        }

        item {
            SectionLabel("Orders")
            QuietPanel {
                Text(
                    if (snap.ordersArrivingToday > 0) "${snap.ordersArrivingToday} arriving today"
                    else "No deliveries today",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        item {
            SectionLabel("What changed")
            GroupCard {
                if (snap.changeEvents.isEmpty()) {
                    ListRow(title = "No recent changes")
                } else {
                    snap.changeEvents.take(6).forEachIndexed { i, ev ->
                        ListRow(
                            title = ev.text,
                            subtitle = TimeFormat.relative(System.currentTimeMillis(), ev.timestamp),
                            onClick = { ev.relatedMessageIds.firstOrNull()?.let(onOpenMessage) },
                            showDivider = i < snap.changeEvents.take(6).lastIndex
                        )
                    }
                }
            }
        }

        item {
            SectionLabel("Cleaned up")
            GroupCard {
                if (snap.cleanedUp.isEmpty()) {
                    ListRow(title = "No grouped clutter yet")
                } else {
                    snap.cleanedUp.forEachIndexed { i, g ->
                        ListRow(
                            title = "${g.count} ${g.title}",
                            subtitle = g.reason,
                            onClick = { g.messageIds.firstOrNull()?.let(onOpenMessage) },
                            showDivider = i < snap.cleanedUp.lastIndex
                        )
                    }
                }
            }
        }

        item {
            QuietPanel(onClick = onAsk) {
                Text("Ask My Messages", style = MaterialTheme.typography.titleLarge, color = Accent)
                VerticalSpacer(4)
                Text(
                    "Answers from your indexed history — with sources",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onOpenActivity) { Text("Activity") }
            }
        }
    }
}
