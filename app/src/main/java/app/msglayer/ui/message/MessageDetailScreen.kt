package app.msglayer.ui.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.modifier.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.msglayer.core.common.TimeFormat
import app.msglayer.ui.MessageDetailViewModel
import app.msglayer.ui.components.KeyValueRow
import app.msglayer.ui.components.QuietPanel
import app.msglayer.ui.components.SectionLabel
import app.msglayer.ui.components.VerticalSpacer
import app.msglayer.ui.theme.TextSecondary

@Composable
fun MessageDetailScreen(
    messageId: String,
    vm: MessageDetailViewModel = hiltViewModel()
) {
    val msg = vm.message(messageId)
    val sender = msg?.let { vm.sender(it.senderId) }
    val facts = vm.relatedFacts(messageId)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(sender?.displayName ?: "Message", style = MaterialTheme.typography.displaySmall)
            if (msg != null) {
                Text(TimeFormat.relative(System.currentTimeMillis(), msg.timestamp), color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            }
        }
        item {
            SectionLabel("Original")
            QuietPanel {
                Text(msg?.body ?: "Not found", style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (msg != null) {
            item {
                SectionLabel("Organization")
                QuietPanel {
                    KeyValueRow("Visibility", msg.visibility.name.lowercase())
                    KeyValueRow("Lifecycle", msg.lifecycle.type.name.lowercase())
                    val labels = msg.classification?.labels?.joinToString { "${it.label.name.lowercase()} ${(it.confidence * 100).toInt()}%" }.orEmpty()
                    VerticalSpacer(6)
                    Text(labels, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }
        if (facts.isNotEmpty()) {
            item { SectionLabel("Derived facts") }
            facts.forEach { fact ->
                item {
                    QuietPanel {
                        Text(fact.type.name, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Text(fact.displayValue, style = MaterialTheme.typography.titleLarge)
                        if (fact.supersededBy != null || !fact.isCurrent) {
                            Text("Superseded — kept for history", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
