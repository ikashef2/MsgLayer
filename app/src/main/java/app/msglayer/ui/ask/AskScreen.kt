import androidx.lifecycle.viewmodel.compose.viewModel
package app.msglayer.ui.ask

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.modifier.`Modifier` as Mod
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.msglayer.core.common.TimeFormat
import app.msglayer.ui.AskViewModel
import app.msglayer.ui.components.QuietPanel
import app.msglayer.ui.components.SectionLabel
import app.msglayer.ui.components.VerticalSpacer
import app.msglayer.ui.theme.TextSecondary
import app.msglayer.ui.theme.Warning

@Composable
fun AskScreen(
    onOpenMessage: (String) -> Unit,
    vm: AskViewModel = viewModel()
) {
    var query by remember { mutableStateOf("") }
    val answer by vm.answer.collectAsStateWithLifecycle()
    val suggestions = listOf(
        "How much money do I have?",
        "When is my dentist appointment?",
        "What was that address Reza sent me?",
        "Find the Figma link someone sent me.",
        "What is my latest Melli account balance?"
    )

    Column(Mod.fillMaxSize().padding(16.dp)) {
        Text("Ask My Messages", style = MaterialTheme.typography.displaySmall)
        Text("Answers come from your indexed messages, with evidence.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        VerticalSpacer(12)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Mod.fillMaxWidth(),
            placeholder = { Text("Ask about balances, appointments, people…") },
            singleLine = true
        )
        VerticalSpacer(8)
        Button(onClick = { vm.ask(query) }, modifier = Mod.fillMaxWidth()) {
            Text("Ask")
        }
        VerticalSpacer(12)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (answer == null) {
                item { SectionLabel("Try") }
                items(suggestions) { s ->
                    QuietPanel(onClick = { query = s; vm.ask(s) }) {
                        Text(s, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                item {
                    QuietPanel {
                        Text(answer!!.answerText, style = MaterialTheme.typography.titleLarge)
                        VerticalSpacer(8)
                        answer!!.structuredLines.forEach {
                            Text(it, style = MaterialTheme.typography.bodyLarge)
                            VerticalSpacer(4)
                        }
                        answer!!.freshnessNote?.let {
                            VerticalSpacer(8)
                            Text(it, style = MaterialTheme.typography.labelMedium, color = Warning)
                        }
                    }
                }
                item { SectionLabel("Source") }
                items(answer!!.evidence.messageIds) { id ->
                    val msg = vm.message(id) ?: return@items
                    QuietPanel {
                        Row(Mod.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(vm.senderName(msg.senderId), style = MaterialTheme.typography.titleMedium)
                            Text(TimeFormat.relative(System.currentTimeMillis(), msg.timestamp), color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                        }
                        Text(msg.body.lines().take(3).joinToString("\n"), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        TextButton(onClick = { onOpenMessage(id) }) { Text("Open message") }
                    }
                }
            }
        }
    }
}
