package com.lifedesk.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.lifedesk.app.data.Category
import com.lifedesk.app.domain.QueryEngine
import com.lifedesk.app.domain.Urgency
import com.lifedesk.app.domain.isSnoozed
import com.lifedesk.app.domain.urgency
import com.lifedesk.app.ui.AppViewModel
import com.lifedesk.app.ui.components.SwipeItemRow
import com.lifedesk.app.ui.components.GlassCard
import com.lifedesk.app.ui.components.accent
import com.lifedesk.app.ui.components.icon
import com.lifedesk.app.ui.theme.Mono
import com.lifedesk.app.ui.theme.Neon
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.time.LocalDate

private enum class StatusFilter(val label: String) { ALL("All"), URGENT("🔴 Urgent"), UPCOMING("🟠 Upcoming"), MONITORED("🟢 Monitored") }

private val suggestions = listOf(
    "What payments are due this week?",
    "Show documents expiring this month",
    "When does my insurance expire?",
    "How much do I spend on subscriptions?",
    "Overdue",
)

@Composable
fun ItemsScreen(vm: AppViewModel, nav: NavHostController, initialStatus: String, initialQuery: String) {
    val items by vm.items.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    var status by rememberSaveable { mutableStateOf(runCatching { StatusFilter.valueOf(initialStatus) }.getOrDefault(StatusFilter.ALL)) }
    var category by rememberSaveable { mutableStateOf<Category?>(null) }
    val today = LocalDate.now()

    val result = remember(items, query, status, category) {
        val r = QueryEngine.run(query, items, today, settings.currency)
        val filtered = r.items.filter { item ->
            val u = if (item.isSnoozed(today)) Urgency.MONITORED else item.urgency(today)
            val statusOk = when (status) {
                StatusFilter.ALL -> true
                StatusFilter.URGENT -> u == Urgency.OVERDUE || u == Urgency.URGENT
                StatusFilter.UPCOMING -> u == Urgency.UPCOMING
                StatusFilter.MONITORED -> u == Urgency.MONITORED || u == Urgency.NO_DATE
            }
            statusOk && (category == null || item.category == category)
        }
        r.copy(items = filtered)
    }
    val usedCategories = remember(items) { items.map { it.category }.distinct().sortedBy { it.ordinal } }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
    ) {
        item {
            Text("DATABASE", style = MaterialTheme.typography.labelSmall, color = Neon.Cyan)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Everything", style = MaterialTheme.typography.headlineMedium, color = Neon.Text, modifier = Modifier.weight(1f))
                Text("${result.items.size}/${items.size}", fontFamily = Mono, color = Neon.Muted, fontSize = 13.sp)
            }
            // Natural-language search bar
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp).height(54.dp).clip(RoundedCornerShape(18.dp))
                    .background(Neon.Surface.copy(alpha = 0.9f))
                    .border(1.dp, Brush.linearGradient(listOf(Neon.Cyan.copy(alpha = 0.6f), Neon.Violet.copy(alpha = 0.4f))), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, null, tint = Neon.Cyan, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Ask: when does my insurance expire?", color = Neon.Faint, maxLines = 1)
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = TextStyle(color = Neon.Text, fontSize = 15.sp), cursorBrush = SolidColor(Neon.Cyan),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Clear", tint = Neon.Muted) }
            }
            if (query.isEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { s -> NeonChip(s, selected = false, color = Neon.Violet) { query = s } }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusFilter.entries.forEach { f -> NeonChip(f.label.replace(Regex("^[^\\p{L}]+\\s*"), ""), status == f, f.color) { status = f } }
            }
            if (usedCategories.size > 1) {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    usedCategories.forEach { c ->
                        NeonChip(c.label, category == c, c.accent, icon = c.icon) { category = if (category == c) null else c }
                    }
                }
            }
            if (query.isNotBlank() && result.answer != null) {
                GlassCard(Modifier.fillMaxWidth().padding(top = 12.dp), glow = Neon.Violet, padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AutoAwesome, null, tint = Neon.Violet, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("ANSWER", style = MaterialTheme.typography.labelSmall, color = Neon.Violet)
                    }
                    Text(result.answer, style = MaterialTheme.typography.titleMedium, color = Neon.Text, modifier = Modifier.padding(top = 6.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (result.items.isEmpty()) {
            item {
                Text(
                    if (items.isEmpty()) "Nothing here yet. Tap the scan button to add your first document." else "No matching items.",
                    modifier = Modifier.padding(vertical = 32.dp), color = Neon.Muted,
                )
            }
        }
        // Group by month so long lists read like a timeline.
        val groups = result.items.groupBy { it.dueDate?.let { d -> java.time.YearMonth.from(d) } }
        groups.forEach { (month, list) ->
            item(key = "h-$month") {
                Text(
                    month?.let { "${it.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)} ${it.year}" }?.uppercase() ?: "NO DATE",
                    style = MaterialTheme.typography.labelSmall, color = Neon.Muted, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                )
            }
            items(list, key = { it.id }) {
                SwipeItemRow(it, today, onClick = { nav.navigate("item/${it.id}") }, onComplete = { vm.complete(it) }, onSnooze = { vm.snooze(it, 3) })
            }
        }
    }
}

private val StatusFilter.color: Color
    get() = when (this) {
        StatusFilter.ALL -> Neon.Cyan
        StatusFilter.URGENT -> Neon.Red
        StatusFilter.UPCOMING -> Neon.Amber
        StatusFilter.MONITORED -> Neon.Green
    }

@Composable
private fun NeonChip(text: String, selected: Boolean, color: Color, icon: ImageVector? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.clip(shape)
            .background(if (selected) color.copy(alpha = 0.2f) else Neon.Surface.copy(alpha = 0.7f))
            .border(1.dp, if (selected) color else Neon.Stroke, shape)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Icon(it, null, tint = color, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, fontSize = 13.sp, color = if (selected) color else Neon.Text, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
