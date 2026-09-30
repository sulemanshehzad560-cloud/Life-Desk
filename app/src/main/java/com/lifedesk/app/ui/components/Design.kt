package com.lifedesk.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifedesk.app.data.Category
import com.lifedesk.app.ui.theme.Mono
import com.lifedesk.app.ui.theme.Neon

// ---------------------------------------------------------------- category identity

val Category.icon: ImageVector
    get() = when (this) {
        Category.INSURANCE -> Icons.Outlined.Shield
        Category.BILL -> Icons.Outlined.Receipt
        Category.CREDIT_CARD -> Icons.Outlined.CreditCard
        Category.LOAN -> Icons.Outlined.AccountBalance
        Category.SUBSCRIPTION -> Icons.Outlined.Autorenew
        Category.ID_DOCUMENT -> Icons.Outlined.Badge
        Category.VEHICLE -> Icons.Outlined.DirectionsCar
        Category.RENT -> Icons.Outlined.Home
        Category.WARRANTY -> Icons.Outlined.Build
        Category.SCHOOL -> Icons.Outlined.School
        Category.MEDICAL -> Icons.Outlined.MedicalServices
        Category.TRAVEL -> Icons.Outlined.Flight
        Category.FINE -> Icons.Outlined.Gavel
        Category.OTHER -> Icons.Outlined.PushPin
    }

val Category.accent: Color
    get() = when (this) {
        Category.INSURANCE -> Color(0xFF60A5FA)
        Category.BILL -> Color(0xFF22D3EE)
        Category.CREDIT_CARD -> Color(0xFFA78BFA)
        Category.LOAN -> Color(0xFF818CF8)
        Category.SUBSCRIPTION -> Color(0xFFF472B6)
        Category.ID_DOCUMENT -> Color(0xFFFBBF24)
        Category.VEHICLE -> Color(0xFF38BDF8)
        Category.RENT -> Color(0xFF34D399)
        Category.WARRANTY -> Color(0xFFFB923C)
        Category.SCHOOL -> Color(0xFFA3E635)
        Category.MEDICAL -> Color(0xFFF87171)
        Category.TRAVEL -> Color(0xFF2DD4BF)
        Category.FINE -> Color(0xFFEF4444)
        Category.OTHER -> Color(0xFF94A3B8)
    }

/** Rounded tile with a glowing gradient and the category icon. */
@Composable
fun CategoryTile(category: Category, size: Dp = 44.dp) {
    val c = category.accent
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.3f))
            .background(Brush.linearGradient(listOf(c.copy(alpha = 0.28f), c.copy(alpha = 0.08f))))
            .border(1.dp, c.copy(alpha = 0.45f), RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) { Icon(category.icon, category.label, tint = c, modifier = Modifier.size(size * 0.5f)) }
}

// ---------------------------------------------------------------- surfaces

/** Frosted-glass card with a faint gradient hairline border. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    glow: Color = Neon.Cyan,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Neon.Surface2.copy(alpha = 0.92f), Neon.Surface.copy(alpha = 0.85f))))
            .border(1.dp, Brush.linearGradient(listOf(glow.copy(alpha = 0.45f), Neon.Stroke.copy(alpha = 0.6f), Neon.Stroke.copy(alpha = 0.25f))), shape)
            .let { if (onClick != null) it.pressable(onClick) else it }
            .padding(padding),
        content = content,
    )
}

/** Deep-space background: slow drifting neon glows plus a faint tech grid. */
@Composable
fun NeonBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val t = rememberInfiniteTransition(label = "bg")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Reverse), label = "drift")
    Box(
        modifier.fillMaxSize().background(Neon.Bg).drawBehind {
            val w = size.width
            val h = size.height
            drawCircle(
                Brush.radialGradient(listOf(Neon.Violet.copy(alpha = 0.22f), Color.Transparent), center = Offset(w * (0.15f + 0.2f * drift), h * 0.08f), radius = w * 0.8f),
                radius = w * 0.8f, center = Offset(w * (0.15f + 0.2f * drift), h * 0.08f),
            )
            drawCircle(
                Brush.radialGradient(listOf(Neon.Cyan.copy(alpha = 0.16f), Color.Transparent), center = Offset(w * (0.95f - 0.25f * drift), h * 0.35f), radius = w * 0.7f),
                radius = w * 0.7f, center = Offset(w * (0.95f - 0.25f * drift), h * 0.35f),
            )
            drawCircle(
                Brush.radialGradient(listOf(Neon.Blue.copy(alpha = 0.10f), Color.Transparent), center = Offset(w * 0.3f, h * (0.85f - 0.1f * drift)), radius = w * 0.9f),
                radius = w * 0.9f, center = Offset(w * 0.3f, h * (0.85f - 0.1f * drift)),
            )
            // Drifting particles: a slow starfield of cyan/violet specks.
            for (i in 0 until 46) {
                val seed = (i * 7919) % 1000 / 1000f
                val seed2 = (i * 104729) % 1000 / 1000f
                val px = (seed * w + drift * w * 0.08f * (if (i % 2 == 0) 1 else -1) + w) % w
                val py = (seed2 * h - drift * h * 0.12f * (1 + i % 3) + h * 2) % h
                val r = (0.6f + (i % 4) * 0.45f).dp.toPx()
                val c = if (i % 3 == 0) Neon.Violet else Neon.Cyan
                drawCircle(c.copy(alpha = 0.10f + (i % 5) * 0.05f), r, Offset(px, py))
            }
            val step = 36.dp.toPx()
            val line = Color.White.copy(alpha = 0.025f)
            var x = 0f
            while (x < w) { drawLine(line, Offset(x, 0f), Offset(x, h)); x += step }
            var y = 0f
            while (y < h) { drawLine(line, Offset(0f, y), Offset(w, y)); y += step }
        },
        content = content,
    )
}

/** Small caps section label with a gradient hairline: "● NEEDS ATTENTION ————". */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, color: Color = Neon.Cyan, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        GlowDot(color, 7.dp)
        Spacer(Modifier.width(8.dp))
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = Neon.Muted)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.5f), Color.Transparent))))
        action?.let { Spacer(Modifier.width(8.dp)); it() }
    }
}

@Composable
fun GlowDot(color: Color, size: Dp = 8.dp) {
    Box(
        Modifier.size(size * 2.2f).drawBehind {
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.55f), Color.Transparent)), radius = this.size.minDimension / 2)
            drawCircle(color, radius = size.toPx() / 2)
        },
    )
}

/** Pill with gradient fill, used for primary actions. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    brush: Brush = Neon.Primary,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Row(
        modifier.height(52.dp).clip(RoundedCornerShape(16.dp))
            .background(if (enabled) brush else Brush.linearGradient(listOf(Neon.Surface2, Neon.Surface2)))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon?.let { Icon(it, null, tint = Color(0xFF06101E)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = if (enabled) Color(0xFF06101E) else Neon.Faint, fontWeight = FontWeight.Bold)
    }
}

/** Compact stat pill: label on top, big monospace value. */
@Composable
fun StatPill(label: String, value: String, color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    GlassCard(modifier, glow = color, onClick = onClick, padding = 14.dp, shape = RoundedCornerShape(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlowDot(color, 6.dp)
            Spacer(Modifier.width(6.dp))
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Neon.Muted)
        }
        Text(value, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = color, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun AnimatedCount(target: Int, style: TextStyle, color: Color = Neon.Text) {
    val v by animateIntAsState(target, tween(900), label = "count")
    Text("$v", style = style, color = color, fontFamily = Mono)
}

/** Small rounded label: "RENEWAL", "MONTHLY", etc. */
@Composable
fun Tag(text: String, color: Color = Neon.Cyan) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

// ---------------------------------------------------------------- charts & gauges

/** Animated circular gauge with a gradient arc; [content] is drawn in the middle. */
@Composable
fun RingGauge(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    stroke: Dp = 10.dp,
    colors: List<Color> = listOf(Neon.Cyan, Neon.Violet, Neon.Cyan),
    content: @Composable BoxScope.() -> Unit = {},
) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(1200), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val arcSize = Size(this.size.width - s, this.size.height - s)
            val tl = Offset(s / 2, s / 2)
            drawArc(Neon.Stroke.copy(alpha = 0.7f), -90f, 360f, false, tl, arcSize, style = Stroke(s, cap = StrokeCap.Round))
            // soft glow under the arc
            drawArc(colors.first().copy(alpha = 0.18f), -90f, 360f * p, false, tl, arcSize, style = Stroke(s * 2.2f, cap = StrokeCap.Round))
            drawArc(Brush.sweepGradient(colors), -90f, 360f * p, false, tl, arcSize, style = Stroke(s, cap = StrokeCap.Round))
        }
        content()
    }
}

/** Countdown ring for an item: how close the date is inside its attention window. */
@Composable
fun CountdownRing(daysLeft: Long?, window: Int, color: Color, size: Dp = 48.dp) {
    val progress = when {
        daysLeft == null -> 0f
        daysLeft <= 0 -> 1f
        else -> (1f - daysLeft.toFloat() / (window * 2f).coerceAtLeast(1f)).coerceIn(0.05f, 1f)
    }
    RingGauge(progress, size = size, stroke = 4.dp, colors = listOf(color, color.copy(alpha = 0.6f), color)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when {
                    daysLeft == null -> "—"
                    daysLeft < 0 -> "${-daysLeft}"
                    daysLeft > 999 -> "999+"
                    else -> "$daysLeft"
                },
                fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = if ((daysLeft ?: 0) > 99) 11.sp else 14.sp, color = color, lineHeight = 14.sp,
            )
            Text(if (daysLeft != null && daysLeft < 0) "LATE" else "DAYS", fontSize = 7.sp, letterSpacing = 1.sp, color = Neon.Muted, lineHeight = 8.sp)
        }
    }
}

/** Vertical gradient bars with rounded tops; [highlight] bar glows. */
@Composable
fun NeonBars(values: List<Double>, labels: List<String>, modifier: Modifier = Modifier, highlight: Int = -1, onSelect: ((Int) -> Unit)? = null) {
    val max = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            val target = (v / max).toFloat().coerceIn(0.03f, 1f)
            val f by animateFloatAsState(target, tween(900, delayMillis = i * 40), label = "bar$i")
            val on = i == highlight
            Column(
                Modifier.weight(1f).let { m -> if (onSelect != null) m.clickable { onSelect(i) } else m },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.fillMaxWidth().height((120 * f).dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(
                            if (on || highlight < 0) Brush.verticalGradient(listOf(Neon.Cyan, Neon.Violet))
                            else Brush.verticalGradient(listOf(Neon.Cyan.copy(alpha = 0.35f), Neon.Violet.copy(alpha = 0.25f))),
                        ),
                )
                Spacer(Modifier.height(6.dp))
                Text(labels.getOrElse(i) { "" }, fontSize = 10.sp, color = if (on) Neon.Text else Neon.Muted, fontFamily = Mono, maxLines = 1)
            }
        }
    }
}

/** Donut chart of shares with a gap between segments. */
@Composable
fun Donut(slices: List<Pair<Color, Double>>, modifier: Modifier = Modifier, size: Dp = 150.dp, stroke: Dp = 18.dp, content: @Composable BoxScope.() -> Unit = {}) {
    val total = slices.sumOf { it.second }.takeIf { it > 0 } ?: 1.0
    val grow by animateFloatAsState(1f, tween(1000), label = "donut")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val arc = Size(this.size.width - s, this.size.height - s)
            val tl = Offset(s / 2, s / 2)
            drawArc(Neon.Stroke.copy(alpha = 0.5f), 0f, 360f, false, tl, arc, style = Stroke(s))
            var start = -90f
            slices.forEach { (c, v) ->
                val sweep = (v / total * 360.0).toFloat() * grow
                drawArc(c, start + 1.5f, (sweep - 3f).coerceAtLeast(0.5f), false, tl, arc, style = Stroke(s, cap = StrokeCap.Butt))
                start += sweep
            }
        }
        content()
    }
}

/** Minimal line chart with gradient fill (e.g. price history). */
@Composable
fun Sparkline(values: List<Double>, modifier: Modifier = Modifier, color: Color = Neon.Cyan) {
    if (values.size < 2) return
    val min = values.minOrNull() ?: 0.0
    val max = values.maxOrNull() ?: 1.0
    val range = (max - min).takeIf { it > 0 } ?: 1.0
    Canvas(modifier) {
        val stepX = size.width / (values.size - 1)
        fun y(v: Double) = (size.height * (1 - ((v - min) / range) * 0.8 - 0.1)).toFloat()
        val line = Path().apply {
            values.forEachIndexed { i, v -> if (i == 0) moveTo(0f, y(v)) else lineTo(i * stepX, y(v)) }
        }
        val fill = Path().apply { addPath(line); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)))
        drawPath(line, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        values.forEachIndexed { i, v ->
            drawCircle(Neon.Bg, 5.dp.toPx(), Offset(i * stepX, y(v)))
            drawCircle(color, 3.5.dp.toPx(), Offset(i * stepX, y(v)))
        }
    }
}

/** Thin progress bar with gradient fill. */
@Composable
fun NeonProgress(fraction: Float, modifier: Modifier = Modifier, brush: Brush = Neon.Primary) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(800), label = "progress")
    Box(modifier.height(6.dp).clip(RoundedCornerShape(3.dp)).background(Neon.Stroke.copy(alpha = 0.6f))) {
        Box(Modifier.fillMaxWidth(f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(brush))
    }
}

/** A labelled key/value tile for detail grids. */
@Composable
fun RowScope.InfoTile(label: String, value: String, color: Color = Neon.Text, onClick: (() -> Unit)? = null) {
    GlassCard(Modifier.weight(1f), padding = 12.dp, shape = RoundedCornerShape(16.dp), glow = Neon.Stroke, onClick = onClick) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Neon.Muted)
        Text(value, fontFamily = Mono, fontWeight = FontWeight.SemiBold, color = color, maxLines = 2, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Pulsing "live" indicator used in headers. */
@Composable
fun LivePulse(color: Color = Neon.Green) {
    val t = rememberInfiniteTransition(label = "pulse")
    val a by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "a")
    Box(Modifier.size(8.dp).clip(CircleShape).background(color.copy(alpha = a)))
}

/** Corner-bracket rounded rectangle, like a camera viewfinder. */
fun Modifier.viewfinder(color: Color, corner: Dp = 26.dp, length: Dp = 34.dp, width: Dp = 3.dp): Modifier = drawBehind {
    val l = length.toPx()
    val w = width.toPx()
    val r = corner.toPx()
    val st = Stroke(w, cap = StrokeCap.Round)
    fun corner(x: Float, y: Float, dx: Float, dy: Float) {
        val p = Path().apply {
            moveTo(x, y + dy * l)
            lineTo(x, y + dy * r)
            quadraticTo(x, y, x + dx * r, y)
            lineTo(x + dx * l, y)
        }
        drawPath(p, color, style = st)
    }
    corner(0f, 0f, 1f, 1f)
    corner(size.width, 0f, -1f, 1f)
    corner(0f, size.height, 1f, -1f)
    corner(size.width, size.height, -1f, -1f)
    drawRoundRect(color.copy(alpha = 0.05f), cornerRadius = CornerRadius(r, r))
}


// ---------------------------------------------------------------- futuristic effects

/** Animated holographic border: a light sweep that travels around the card edge. */
@Composable
fun Modifier.holoBorder(shape: Shape = RoundedCornerShape(22.dp), width: Dp = 1.5.dp): Modifier {
    val t = rememberInfiniteTransition(label = "holo")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(5_000, easing = LinearEasing)), label = "holoAngle")
    return this.border(width, HoloBrush(angle), shape)
}

/** Sweep gradient (cyan → violet → pink) rotated by [angle] degrees around the centre. */
private class HoloBrush(private val angle: Float) : androidx.compose.ui.graphics.ShaderBrush() {
    override fun createShader(size: Size): androidx.compose.ui.graphics.Shader {
        val c = Offset(size.width / 2, size.height / 2)
        val shader = androidx.compose.ui.graphics.SweepGradientShader(
            c,
            listOf(Neon.Cyan, Neon.Violet.copy(alpha = 0.15f), Neon.Pink, Neon.Cyan.copy(alpha = 0.1f), Neon.Cyan),
        )
        shader.setLocalMatrix(android.graphics.Matrix().apply { setRotate(angle, c.x, c.y) })
        return shader
    }
    override fun equals(other: Any?) = other is HoloBrush && other.angle == angle
    override fun hashCode() = angle.hashCode()
}

/** Shrinks slightly while pressed and gives a light haptic tick — makes cards feel physical. */
@Composable
fun Modifier.pressable(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.965f else 1f, tween(140), label = "press")
    val haptic = LocalHapticFeedback.current
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

/** Types [text] out character by character, like a terminal. Re-types when the text changes. */
@Composable
fun TypewriterText(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodyMedium, color: Color = Neon.Text, speedMs: Long = 14) {
    var shown by remember(text) { mutableIntStateOf(0) }
    LaunchedEffect(text) {
        while (shown < text.length) { kotlinx.coroutines.delay(speedMs); shown++ }
    }
    val cursor = if (shown < text.length) "▍" else ""
    Text(text.take(shown) + cursor, modifier = modifier, style = style, color = color)
}

/** A rotating radar sweep, drawn over a ring gauge. */
@Composable
fun RadarSweep(modifier: Modifier = Modifier, color: Color = Neon.Cyan) {
    val t = rememberInfiniteTransition(label = "radar")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(3_200, easing = LinearEasing)), label = "radarAngle")
    Canvas(modifier) {
        rotate(angle) {
            drawCircle(
                Brush.sweepGradient(listOf(Color.Transparent, Color.Transparent, color.copy(alpha = 0.0f), color.copy(alpha = 0.28f))),
                radius = size.minDimension / 2 * 0.78f,
            )
        }
    }
}
