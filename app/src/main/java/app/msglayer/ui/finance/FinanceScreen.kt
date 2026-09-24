package app.msglayer.ui.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.msglayer.core.common.MoneyNormalizer
import app.msglayer.ui.FinanceViewModel
import app.msglayer.ui.components.KeyValueRow
import app.msglayer.ui.components.QuietPanel
import app.msglayer.ui.components.ScreenHeader
import app.msglayer.ui.components.SectionLabel
import app.msglayer.ui.components.VerticalSpacer
import app.msglayer.ui.theme.TextSecondary
import app.msglayer.ui.theme.Warning

@Composable
fun FinanceScreen(vm: FinanceViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeader(
                title = "Finance",
                subtitle = "Last known from SMS — not live banking"
            )
        }
        item { SectionLabel("Accounts") }
        if (state.accounts.isEmpty()) {
            item {
                QuietPanel {
                    Text("No accounts yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Sync Device SMS in Settings to extract balances.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(state.accounts) { acc ->
                QuietPanel {
                    Text(acc.bankName, style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    VerticalSpacer(4)
                    Text(
                        vm.format(acc.lastKnownBalanceToman),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    VerticalSpacer(4)
                    Text(
                        "Updated ${vm.relative(acc.balanceUpdatedAt)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }
        }
        item { SectionLabel("Transactions") }
        if (state.transactions.isEmpty()) {
            item {
                QuietPanel {
                    Text("No transactions parsed yet", color = TextSecondary)
                }
            }
        } else {
            items(state.transactions) { tx ->
                QuietPanel {
                    KeyValueRow(
                        tx.type.name.lowercase().replaceFirstChar { it.titlecase() },
                        tx.amount.amountToman?.let { MoneyNormalizer.formatToman(it) } ?: tx.amount.originalText
                    )
                    VerticalSpacer(4)
                    Text(
                        listOfNotNull(tx.merchant, tx.accountId.removePrefix("acc-")).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    if (!tx.amount.conversionConfident) {
                        VerticalSpacer(4)
                        Text("Currency uncertain — not assumed", style = MaterialTheme.typography.labelMedium, color = Warning)
                    }
                }
            }
        }
    }
}
