package com.lifedesk.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lifedesk.app.domain.Urgency

private val Brand = Color(0xFF0F5C4D)
private val BrandLight = Color(0xFF9FE3D2)

private val Light = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4F1E8),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF4A635C),
    secondaryContainer = Color(0xFFCCE8DF),
    tertiary = Color(0xFF3F6374),
    background = Color(0xFFF7FAF8),
    surface = Color(0xFFF7FAF8),
    surfaceVariant = Color(0xFFDBE5E0),
    surfaceContainer = Color(0xFFEBF1EE),
    surfaceContainerHigh = Color(0xFFE5ECE9),
)

private val Dark = darkColorScheme(
    primary = BrandLight,
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF005143),
    onPrimaryContainer = Color(0xFFBCEFDF),
    secondary = Color(0xFFB1CCC3),
    secondaryContainer = Color(0xFF334B45),
    tertiary = Color(0xFFA7CCE0),
    background = Color(0xFF0F1513),
    surface = Color(0xFF0F1513),
    surfaceVariant = Color(0xFF3F4945),
    surfaceContainer = Color(0xFF1B2220),
    surfaceContainerHigh = Color(0xFF252C2A),
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
)

@Composable
fun LifeDeskTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, typography = AppTypography, content = content)
}

@Composable
fun urgencyColor(u: Urgency): Color {
    val dark = isSystemInDarkTheme()
    return when (u) {
        Urgency.OVERDUE, Urgency.URGENT -> if (dark) Color(0xFFFF8A80) else Color(0xFFC62828)
        Urgency.UPCOMING -> if (dark) Color(0xFFFFC266) else Color(0xFFE08600)
        Urgency.MONITORED -> if (dark) Color(0xFF7DD99A) else Color(0xFF2E7D32)
        Urgency.NO_DATE -> MaterialTheme.colorScheme.outline
    }
}

val SavingsGreen = Color(0xFF2E7D32)
