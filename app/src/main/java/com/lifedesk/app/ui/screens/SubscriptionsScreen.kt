package com.lifedesk.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.lifedesk.app.data.Category
import com.lifedesk.app.data.LifeItem
import com.lifedesk.app.data.monthlyCost
import com.lifedesk.app.domain.UNUSED_AFTER_DAYS
import com.lifedesk.app.domain.daysSinceUsed
import com.lifedesk.app.domain.forecast
import com.lifedesk.app.domain.isUnusedSubscription
import com.lifedesk.app.domain.money
import com.lifedesk.app.ui.AppViewModel
import com.lifedesk.app.ui.components.CategoryTile
import com.lifedesk.app.ui.components.Donut
import com.lifedesk.app.ui.components.GlassCard
import com.lifedesk.app.ui.components.GlowDot
import com.lifedesk.app.ui.components.GradientButton
import com.lifedesk.app.ui.components.NeonBars
import com.lifedesk.app.ui.components.NeonProgress
import com.lifedesk.app.ui.components.SectionHeader
import com.lifedesk.app.ui.components.accent
import com.lifedesk.app.ui.theme.Mono
import com.lifedesk.app.ui.theme.Neon
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SubscriptionsScreen(vm: AppViewModel, nav: NavHostController) {
    val items by vm.items.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val cur = settings.currency
    val subs = items.filter { it.category == Category.SUBSCRIPTION }
        .sortedWith(compareByDescending<LifeItem> { it.isUnusedSubscription(today) }.thenByDescending { it.monthlyCost ?: 0.0 })
    val monthly = subs.mapNotNull { it.monthlyCost }.sum()
    val unused = subs.filter { it.isUnusedSubscription(today) }
    val saving = unused.mapNotNull { it.monthlyCost }.sum()
    val f = remember(items) { forecast(items, today, 12) }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val yearTotal = f.sumOf { it.total }
    val byCategory = remember(f) {
        f.flatMap { it.byCategory.entries }.groupBy({ it.key }, { it.value }).mapValues { it.value.sum() }
            .entries.sortedByDescending { it.value }
    }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
    ) {
        item {
            Text("FINANCE", style = MaterialTheme.typography.labelSmall, color = Neon.Cyan)
            Text("Money radar", style = MaterialTheme.typography.headlineMedium, color = Neon.Text)
        }

        // ------------------------------------------------ 12-month overview: donut + legend
        item {
            GlassCard(Modifier.fillMaxWidth().padding(top = 14.dp), glow = Neon.Violet) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Donut(byCategory.map { it.key.accent to it.value }, size = 140.dp, stroke = 16.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("12 MONTHS", fontSize = 8.sp, letterSpacing = 1.4.sp, color = Neon.Muted)
                            Text(compact(yearTotal), fontFamily = Mono, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Neon.Text)
                            Text(cur, fontSize = 10.sp, color = Neon.Muted)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (byCategory.isEmpty()) Text("Add bills and subscriptions with amounts to see your breakdown.", color = Neon.Muted,
                            style = MaterialTheme.typography.bodySmall)
                        byCategory.take(6).forEach { (c, v) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(c.accent))
                                Spacer(Modifier.width(6.dp))
                                Text(c.label, style = MaterialTheme.typography.bodySmall, color = Neon.Text, modifier = Modifier.weight(1f), maxLines = 1)
                                Text("${(v / yearTotal.coerceAtLeast(1.0) * 100).toInt()}%", fontFamily = Mono, fontSize = 11.sp, color = Neon.Muted)
                            }
                        }
                    }
                }
            }
        }

        // ------------------------------------------------ month by month
        item {
            SectionHeader("Month by month", color = Neon.Cyan)
            GlassCard(Modifier.fillMaxWidth()) {
                NeonBars(
                    values = f.map { it.total },
                    labels = f.map { it.month.month.getDisplayName(TextStyle.NARROW, Locale.ENGLISH) },
                    highlight = selected,
                    onSelect = { selected = it },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                )
                val m = f.getOrNull(selected)
                if (m != null) {
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 14.dp)) {
                        Text(
                            "${m.month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${m.month.year}".uppercase(),
                            style = MaterialTheme.typography.labelSmall, color = Neon.Muted, modifier = Modifier.weight(1f),
                        )
                        Text(money(m.total, cur), fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Neon.Cyan)
                    }
                    m.byCategory.entries.sortedByDescending { it.value }.forEach { (c, v) ->
                        Column(Modifier.padding(top = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(c.label, style = MaterialTheme.typography.bodySmall, color = Neon.Text, modifier = Modifier.weight(1f))
                                Text(money(v, cur), fontFamily = Mono, fontSize = 12.sp, color = Neon.Text)
                            }
                            NeonProgress((v / m.total.coerceAtLeast(1.0)).toFloat(), Modifier.fillMaxWidth().padding(top = 4.dp),
                                Brush.horizontalGradient(listOf(c.accent, c.accent.copy(alpha = 0.4f))))
                        }
                    }
                    if (m.byCategory.isEmpty()) Text("Nothing due this month", color = Neon.Muted, modifier = Modifier.padding(top = 8.dp))
                    Text("Tap a bar to switch month", style = MaterialTheme.typography.labelSmall, color = Neon.Faint, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }

        // ------------------------------------------------ subscriptions
        item {
            SectionHeader("Subscriptions", color = Neon.Pink)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassCard(Modifier.weight(1f), glow = Neon.Pink, padding = 14.dp) {
                    Text("PER MONTH", style = MaterialTheme.typography.labelSmall, color = Neon.Muted)
                    Text(money(monthly, cur), fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Neon.Text)
                    Text("${money(monthly * 12, cur)} / year", style = MaterialTheme.typography.bodySmall, color = Neon.Muted)
                }
                GlassCard(Modifier.weight(1f), glow = Neon.Green, padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Savings, null, tint = Neon.Green, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("CAN SAVE", style = MaterialTheme.typography.labelSmall, color = Neon.Muted)
                    }
                    Text(money(saving, cur), fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Neon.Green)
                    Text("${unused.size} unused 30+ days", style = MaterialTheme.typography.bodySmall, color = Neon.Muted)
                }
            }
        }
        if (subs.isEmpty()) item {
            Text("No subscriptions yet. Scan a receipt or add one below.", color = Neon.Muted, modifier = Modifier.padding(vertical = 16.dp))
        }
        items(subs, key = { it.id }) { s -> SubscriptionCard(s, today, onOpen = { nav.navigate("item/${s.id}") }, onUsed = { vm.markUsed(s) }) }
        item {
            GradientButton(
                "Add subscription", { vm.startManual(Category.SUBSCRIPTION); nav.navigate("edit") },
                Modifier.fillMaxWidth().padding(top = 12.dp), icon = Icons.Outlined.Add,
            )
        }
    }
}

/** A subscription with a "freshness" bar: full when used today, empty after 30 days unused. */
@Composable
private fun SubscriptionCard(s: LifeItem, today: LocalDate, onOpen: () -> Unit, onUsed: () -> Unit) {
    val flagged = s.isUnusedSubscription(today)
    val days = s.daysSinceUsed(today)
    val freshness = 1f - (days.toFloat() / UNUSED_AFTER_DAYS).coerceIn(0f, 1f)
    GlassCard(Modifier.fillMaxWidth().padding(vertical = 5.dp), glow = if (flagged) Neon.Red else Neon.Pink, onClick = onOpen, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryTile(s.category, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.title, style = MaterialTheme.typography.titleMedium, color = Neon.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlowDot(if (flagged) Neon.Red else Neon.Green, 4.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (s.lastUsed == null) "Usage not recorded" else if (days == 0L) "Used today" else "Used $days days ago",
                        style = MaterialTheme.typography.bodySmall, color = if (flagged) Neon.Red else Neon.Muted,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(s.monthlyCost?.let { money(it, s.currency) } ?: "—", fontFamily = Mono, fontWeight = FontWeight.Bold, color = Neon.Text)
                Text("/ month", fontSize = 10.sp, color = Neon.Muted)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
            NeonProgress(freshness, Modifier.weight(1f), if (flagged) Neon.Danger else Neon.Success)
            Spacer(Modifier.width(10.dp))
            Row(
                Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, Neon.Stroke, RoundedCornerShape(10.dp))
                    .clickable(onClick = onUsed).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.ThumbUp, null, tint = Neon.Cyan, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Used today", fontSize = 11.sp, color = Neon.Cyan, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** 12450.5 → "12.5K" for tight spaces. */
private fun compact(v: Double): String = when {
    v >= 1_000_000 -> "%.1fM".format(v / 1_000_000)
    v >= 10_000 -> "%.0fK".format(v / 1000)
    v >= 1_000 -> "%.1fK".format(v / 1000)
    else -> "%.0f".format(v)
}
