package com.lifedesk.app.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifedesk.app.data.LifeItem
import com.lifedesk.app.domain.countdown
import com.lifedesk.app.domain.money
import com.lifedesk.app.domain.pretty
import com.lifedesk.app.domain.urgency
import com.lifedesk.app.ui.theme.urgencyColor
import java.time.LocalDate

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun Dot(color: Color, size: Int = 10) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(color))
}

/** One row in any list: emoji, title, countdown in urgency colour, amount on the right. */
@Composable
fun ItemRow(item: LifeItem, today: LocalDate, onClick: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    val u = item.urgency(today)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) { Text(item.category.emoji, style = MaterialTheme.typography.titleLarge) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(urgencyColor(u), 8)
                    Spacer(Modifier.width(6.dp))
                    Text(item.countdown(today), style = MaterialTheme.typography.bodyMedium, color = urgencyColor(u))
                }
                item.provider?.takeIf { it != item.title }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            if (trailing != null) trailing()
            else item.amount?.let { Text(money(it, item.currency), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String?, onClick: (() -> Unit)? = null) {
    if (value.isNullOrBlank()) return
    Row(
        Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 16.dp))
        Text(value, fontWeight = FontWeight.Medium, color = if (onClick != null) MaterialTheme.colorScheme.primary else Color.Unspecified)
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
            modifier = Modifier.fillMaxWidth(),
        )
        // Transparent overlay so the whole field opens the menu.
        Box(Modifier.matchParentSize().padding(top = 8.dp).clickable { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
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
            trailingIcon = { IconButton(onClick = { open() }) { Icon(Icons.Outlined.CalendarMonth, "Choose date") } },
            modifier = Modifier.fillMaxWidth(),
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
    val state = androidx.compose.material3.rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
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
            val toEnd = state.dismissDirection == androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd
            val color = if (toEnd) Color(0xFF2E7D32) else Color(0xFFE08600)
            Box(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(color).padding(horizontal = 20.dp),
                contentAlignment = if (toEnd) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Text(if (toEnd) "✓ Done" else "⏰ Snooze 3 days", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
    ) {
        ItemRow(item, today, onClick)
    }
}

/** Minimal bar chart (no chart library): one bar per value, labels underneath, highlighted index optional. */
@Composable
fun BarChart(values: List<Double>, labels: List<String>, modifier: Modifier = Modifier, highlight: Int = 0, barColor: Color = MaterialTheme.colorScheme.primary) {
    val max = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                val fraction = (v / max).toFloat().coerceIn(0.02f, 1f)
                val animated by androidx.compose.animation.core.animateFloatAsState(fraction, label = "bar")
                Box(
                    Modifier.fillMaxWidth().height((110 * animated).dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (i == highlight) barColor else barColor.copy(alpha = 0.45f)),
                )
                Text(labels.getOrElse(i) { "" }, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}
