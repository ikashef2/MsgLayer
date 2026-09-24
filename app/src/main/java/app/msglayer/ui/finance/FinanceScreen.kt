package app.msglayer.ui.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.modifier.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.msglayer.core.common.MoneyNormalizer
import app.msglayer.ui.FinanceViewModel
import app.msglayer.ui.components.KeyValueRow
import app.msglayer.ui.components.QuietPanel
import app.msglayer.ui.components.SectionLabel
import app.msglayer.ui.components.VerticalSpacer
import app.msglayer.ui.theme.TextSecondary
import app.msglayer.ui.theme.Warning

@Composable
fun FinanceScreen(vm: FinanceViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Finance", style = MaterialTheme.typography.displaySmall)
            Text("Last known balances from SMS — not live banking", style = MaterialTheme.typography.bodyMedium, color = Warning)
        }
        item {
            SectionLabel("Accounts")
            QuietPanel {
                state.accounts.forEach { acc ->
                    Text(acc.bankName, style = MaterialTheme.typography.titleMedium)
                    Text(vm.format(acc.lastKnownBalanceToman), style = MaterialTheme.typography.headlineMedium)
                    Text("Updated ${vm.relative(acc.balanceUpdatedAt)}", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    VerticalSpacer(10)
                }
            }
        }
        item { SectionLabel("Transactions") }
        items(state.transactions) { tx ->
            QuietPanel {
                KeyValueRow(tx.type.name.lowercase(), tx.amount.amountToman?.let { MoneyNormalizer.formatToman(it) } ?: tx.amount.originalText)
                Text(
                    listOfNotNull(tx.merchant, "via ${tx.accountId.removePrefix("acc-")}").joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                if (!tx.amount.conversionConfident) {
                    Text("Currency unit uncertain — not assumed", style = MaterialTheme.typography.labelMedium, color = Warning)
                }
            }
        }
    }
}
