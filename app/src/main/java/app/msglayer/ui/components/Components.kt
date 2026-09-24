package app.msglayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.modifier.`Modifier` as Mod
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.msglayer.ui.theme.Border
import app.msglayer.ui.theme.Surface
import app.msglayer.ui.theme.TextSecondary

@Composable
fun SectionLabel(text: String, modifier: Mod = Mod) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = TextSecondary,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun QuietPanel(
    modifier: Mod = Mod,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface)
            .border(1.dp, Border, shape)
            .then(if (onClick != null) Mod.clickable(onClick = onClick) else Mod)
            .padding(14.dp),
        content = content
    )
}

@Composable
fun KeyValueRow(label: String, value: String) {
    Row(
        Mod.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun VerticalSpacer(h: Int = 12) {
    Spacer(Mod.height(h.dp))
}
