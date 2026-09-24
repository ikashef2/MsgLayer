package app.msglayer.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.msglayer.ui.SearchViewModel
import app.msglayer.ui.components.QuietPanel
import app.msglayer.ui.components.ScreenHeader
import app.msglayer.ui.components.SoftAvatar
import app.msglayer.ui.components.VerticalSpacer
import app.msglayer.ui.theme.TextSecondary
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment

@Composable
fun SearchScreen(
    onOpenMessage: (String) -> Unit,
    vm: SearchViewModel = viewModel()
) {
    var q by remember { mutableStateOf("") }
    val results by vm.results.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        ScreenHeader(title = "Search", subtitle = "Messages, senders, labels")
        OutlinedTextField(
            value = q,
            onValueChange = { q = it; vm.search(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Type to search…") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
        VerticalSpacer(14)
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(results, key = { it.id }) { msg ->
                val name = vm.senderName(msg.senderId)
                QuietPanel(onClick = { onOpenMessage(msg.id) }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SoftAvatar(name)
                        Column {
                            Text(name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                msg.body.lines().firstOrNull().orEmpty(),
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
