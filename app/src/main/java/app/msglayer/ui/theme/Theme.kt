package app.msglayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** HyperOS-inspired light palette — soft gray canvas, white cards, system blue. */
val Bg = Color(0xFFF5F6FA)
val Surface = Color(0xFFFFFFFF)
val Surface2 = Color(0xFFEEF0F5)
val Border = Color(0xFFE6E8EE)
val Divider = Color(0xFFEBEDF2)
val TextPrimary = Color(0xFF1A1A1A)
val TextSecondary = Color(0xFF8A8F9A)
val TextTertiary = Color(0xFFB0B4BE)
val Accent = Color(0xFF3482FF)
val AccentSoft = Color(0xFFD6E6FF)
val AccentMuted = Color(0xFF5B8DEF)
val Positive = Color(0xFF1DBF73)
val Warning = Color(0xFFFF8F1F)
val Danger = Color(0xFFFF4D4F)

private val HyperColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = AccentMuted,
    onSecondary = Color.White,
    tertiary = Positive,
    background = Bg,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    outline = Border,
    outlineVariant = Divider,
    error = Danger,
    onError = Color.White,
    inversePrimary = AccentSoft
)

private val HyperType = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp,
        color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
        color = TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextPrimary
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.1.sp,
        color = TextSecondary
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = TextSecondary
    )
)

@Composable
fun MsgLayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HyperColors,
        typography = HyperType,
        content = content
    )
}
