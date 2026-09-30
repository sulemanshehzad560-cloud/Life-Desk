package com.lifedesk.app.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifedesk.app.data.LifeItem
import com.lifedesk.app.data.Recurrence
import com.lifedesk.app.domain.countdown
import com.lifedesk.app.domain.daysLeft
import com.lifedesk.app.domain.isSnoozed
import com.lifedesk.app.domain.money
import com.lifedesk.app.domain.pretty
import com.lifedesk.app.domain.urgency
import com.lifedesk.app.ui.theme.Mono
import com.lifedesk.app.ui.theme.Neon
import com.lifedesk.app.ui.theme.urgencyColor
import java.time.LocalDate

/** Kept for existing call sites: the neon section header. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    // Strip a leading emoji, the header draws its own glowing dot.
    val clean = text.replace(Regex("^[^\\p{L}\\p{N}]+\\s*"), "")
    SectionHeader(clean, modifier, action = action)
}

@Composable
fun Dot(color: Color, size: Int = 10) = GlowDot(color, (size * 0.8f).dp)

/** Neon field colours shared by every text field in the app. */
@Composable
fun neonFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Neon.Cyan,
    unfocusedBorderColor = Neon.Stroke,
    focusedLabelColor = Neon.Cyan,
    unfocusedLabelColor = Neon.Muted,
    cursorColor = Neon.Cyan,
    focusedContainerColor = Neon.Surface.copy(alpha = 0.7f),
    unfocusedContainerColor = Neon.Surface.copy(alpha = 0.5f),
)

/**
 * The main list card: category tile, title, status line with glow dot, tags, amount in monospace,
 * and a countdown ring on the right.
 */
@Composable
fun ItemRow(item: LifeItem, today: LocalDate, onClick: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    val u = item.urgency(today)
    val c = urgencyColor(u)
    GlassCard(Modifier.fillMaxWidth().padding(vertical = 5.dp), glow = c, onClick = onClick, padding = 14.dp, shape = RoundedCornerShape(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryTile(item.category)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Neon.Text)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    GlowDot(c, 5.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (item.isSnoozed(today)) "Snoozed · ${item.countdown(today)}" else item.countdown(today),
                        style = MaterialTheme.typography.bodySmall, color = c, maxLines = 1,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    item.amount?.let { Text(money(it, item.currency), fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Neon.Text) }
                    if (item.recurrence != Recurrence.NONE) Tag(item.recurrence.label, Neon.Violet)
                    item.provider?.takeIf { it != item.title }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Neon.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            if (trailing != null) trailing() else CountdownRing(item.daysLeft(today), item.category.upcomingDays, c)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String?, onClick: (() -> Unit)? = null) {
    if (value.isNullOrBlank()) return
    Row(
        Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Neon.Muted, modifier = Modifier.padding(end = 16.dp))
        Text(value, fontFamily = Mono, fontWeight = FontWeight.Medium, color = if (onClick != null) Neon.Cyan else Neon.Text)
    }
}

/** Read-only text field that opens a menu of [options]. */
@Composable
fun <T> Dropdown(label: String, value: T, options: List<T>, text: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = text(value), onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null) },
            modifier = Modifier.fillMaxWidth(), colors = neonFieldColors(), shape = RoundedCornerShape(14.dp),
        )
        // Transparent overlay so the whole field opens the menu.
        Box(Modifier.matchParentSize().padding(top = 8.dp).clickable { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Neon.Surface2) {
            options.forEach { o -> DropdownMenuItem(text = { Text(text(o)) }, onClick = { onSelect(o); open = false }) }
        }
    }
}

@Composable
fun DateField(label: String, date: LocalDate?, onChange: (LocalDate?) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    fun open() {
        val d = date ?: LocalDate.now()
        DatePickerDialog(context, { _, y, m, day -> onChange(LocalDate.of(y, m + 1, day)) }, d.year, d.monthValue - 1, d.dayOfMonth).show()
    }
    Box(modifier) {
        OutlinedTextField(
            value = date?.pretty() ?: "",
            onValueChange = {}, readOnly = true, label = { Text(label) }, placeholder = { Text("Tap to choose") },
            trailingIcon = { IconButton(onClick = { open() }) { Icon(Icons.Outlined.CalendarMonth, "Choose date", tint = Neon.Cyan) } },
            modifier = Modifier.fillMaxWidth(), colors = neonFieldColors(), shape = RoundedCornerShape(14.dp),
        )
        Box(Modifier.matchParentSize().padding(top = 8.dp, end = 56.dp).clickable { open() })
    }
}

/**
 * Swipe right → complete (paid / renewed), swipe left → snooze 3 days. The row springs back; the list updates itself.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SwipeItemRow(item: LifeItem, today: LocalDate, onClick: () -> Unit, onComplete: () -> Unit, onSnooze: () -> Unit) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val state = androidx.compose.material3.rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != androidx.compose.material3.SwipeToDismissBoxValue.Settled) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            }
            when (value) {
                androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd -> onComplete()
                androidx.compose.material3.SwipeToDismissBoxValue.EndToStart -> onSnooze()
                else -> Unit
            }
            false
        },
        positionalThreshold = { it * 0.35f },
    )
    androidx.compose.material3.SwipeToDismissBox(
        state = state,
        backgroundContent = {
            // Nothing behind the (translucent) card until the user actually swipes.
            if (state.dismissDirection == androidx.compose.material3.SwipeToDismissBoxValue.Settled) return@SwipeToDismissBox
            val toEnd = state.dismissDirection == androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd
            Box(
                Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(20.dp))
                    .background(if (toEnd) Neon.Success else Neon.Warm)
                    .padding(horizontal = 22.dp),
                contentAlignment = if (toEnd) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (toEnd) Icons.Outlined.CheckCircle else Icons.Outlined.Snooze, null, tint = Color(0xFF06101E))
                    Spacer(Modifier.width(8.dp))
                    Text(if (toEnd) "DONE" else "SNOOZE 3D", color = Color(0xFF06101E), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        },
    ) {
        ItemRow(item, today, onClick)
    }
}

/** Kept for existing call sites: neon gradient bars. */
@Composable
fun BarChart(values: List<Double>, labels: List<String>, modifier: Modifier = Modifier, highlight: Int = -1, barColor: Color = Neon.Cyan) {
    NeonBars(values, labels, modifier, highlight)
}

/** Card-style option row used in settings and menus. */
@Composable
fun OptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    color: Color = Neon.Cyan,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.padding(end = 12.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0.06f))))
                .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp)).padding(9.dp),
        ) { Icon(icon, null, tint = color, modifier = Modifier.height(20.dp).width(20.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Neon.Text)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Neon.Muted) }
        }
        trailing?.invoke()
    }
}
