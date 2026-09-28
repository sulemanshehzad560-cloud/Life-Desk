package com.lifedesk.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lifedesk.app.domain.Urgency

/** LifeDesk "command center" palette: deep space background, glass surfaces, neon accents. */
object Neon {
    val Bg = Color(0xFF060A13)
    val Bg2 = Color(0xFF0A1020)
    val Surface = Color(0xFF0F1729)
    val Surface2 = Color(0xFF152036)
    val Stroke = Color(0xFF223052)
    val Text = Color(0xFFE7EEF9)
    val Muted = Color(0xFF8E9BB5)
    val Faint = Color(0xFF5B6886)

    val Cyan = Color(0xFF22D3EE)
    val Violet = Color(0xFF8B5CF6)
    val Blue = Color(0xFF3B82F6)
    val Green = Color(0xFF34D399)
    val Amber = Color(0xFFFBBF24)
    val Red = Color(0xFFF87171)
    val Pink = Color(0xFFF472B6)

    val Primary = Brush.linearGradient(listOf(Cyan, Violet))
    val Warm = Brush.linearGradient(listOf(Amber, Pink))
    val Danger = Brush.linearGradient(listOf(Red, Pink))
    val Success = Brush.linearGradient(listOf(Green, Cyan))
}

private val Colors = darkColorScheme(
    primary = Neon.Cyan,
    onPrimary = Color(0xFF00212A),
    primaryContainer = Color(0xFF0E2A3D),
    onPrimaryContainer = Color(0xFFBDF4FF),
    secondary = Neon.Violet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF231A45),
    onSecondaryContainer = Color(0xFFE2D8FF),
    tertiary = Neon.Green,
    background = Neon.Bg,
    onBackground = Neon.Text,
    surface = Neon.Bg,
    onSurface = Neon.Text,
    surfaceVariant = Neon.Surface2,
    onSurfaceVariant = Neon.Muted,
    surfaceContainerLowest = Neon.Bg,
    surfaceContainerLow = Neon.Bg2,
    surfaceContainer = Neon.Surface,
    surfaceContainerHigh = Neon.Surface2,
    surfaceContainerHighest = Color(0xFF1B2844),
    outline = Neon.Stroke,
    outlineVariant = Color(0xFF1A2540),
    error = Neon.Red,
)

val Mono = FontFamily.Monospace

private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Black, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp, lineHeight = 14.sp),
)

/** Always dark: LifeDesk is designed as a night-mode command center. */
@Composable
fun LifeDeskTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = AppTypography, content = content)
}

fun urgencyColor(u: Urgency): Color = when (u) {
    Urgency.OVERDUE, Urgency.URGENT -> Neon.Red
    Urgency.UPCOMING -> Neon.Amber
    Urgency.MONITORED -> Neon.Green
    Urgency.NO_DATE -> Neon.Faint
}

val SavingsGreen = Neon.Green
