package app.msglayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Ensure repository boots with mock data
        AppGraph.repository
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0B0C0E))) {
                Surface(color = Color(0xFF0B0C0E)) {
                    BootScreen()
                }
            }
        }
    }
}

@Composable
private fun BootScreen() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("MsgLayer", color = Color(0xFFE8EAED), fontSize = 28.sp)
        Text(
            "Build OK — full UI restore next. Mock intelligence layer is initialized.",
            color = Color(0xFF9AA0A6),
            fontSize = 14.sp
        )
        val overview = AppGraph.repository.overview()
        Text(overview.greeting, color = Color(0xFFE8EAED), fontSize = 18.sp)
        Text(
            "${overview.attentionCount} things need attention",
            color = Color(0xFFC4A574),
            fontSize = 14.sp
        )
        overview.accounts.forEach { acc ->
            Text(
                "${acc.bankName}: ${acc.lastKnownBalanceToman ?: "-"} T",
                color = Color(0xFFE8EAED),
                fontSize = 14.sp
            )
        }
    }
}