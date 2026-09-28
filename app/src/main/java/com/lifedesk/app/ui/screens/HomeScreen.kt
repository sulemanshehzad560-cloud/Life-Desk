package com.lifedesk.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.lifedesk.app.domain.Urgency
import com.lifedesk.app.domain.daysLeft
import com.lifedesk.app.domain.money
import com.lifedesk.app.domain.overview
import com.lifedesk.app.domain.relativeDays
import com.lifedesk.app.domain.urgency
import com.lifedesk.app.ui.AppViewModel
import com.lifedesk.app.ui.components.Dot
import com.lifedesk.app.ui.components.ItemRow
import com.lifedesk.app.ui.components.SectionTitle
import com.lifedesk.app.ui.goTab
import com.lifedesk.app.ui.theme.SavingsGreen
import com.lifedesk.app.ui.theme.urgencyColor
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(vm: AppViewModel, nav: NavHostController) {
    val items by vm.items.collectAsStateWithLifecycle()
    val prices by vm.prices.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val o = remember(items, prices) { overview(items, prices, today) }
    val cur = settings.currency

    val greeting = when (LocalTime.now().hour) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }
    val name = settings.name.trim().takeIf { it.isNotEmpty() }?.let { ", $it" } ?: ""

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("$greeting$name.", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        when {
                            items.isEmpty() -> "Let's add the first thing you don't want to forget."
                            o.attention.isEmpty() -> "Nothing needs your attention right now. 🎉"
                            else -> "You have ${o.attention.size} thing${if (o.attention.size == 1) "" else "s"} that need${if (o.attention.size == 1) "s" else ""} attention."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { nav.goTab("items") }) { Icon(Icons.Outlined.Search, "Search") }
            }
        }

        if (items.isEmpty()) {
            item { EmptyHome(onScan = { nav.goTab("scan") }, onSample = vm::loadSampleData) }
            return@LazyColumn
        }

        item {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("${o.urgentCount}", "urgent", urgencyColor(Urgency.URGENT), Modifier.weight(1f)) { nav.navigate("items?status=URGENT") }
                StatTile("${o.upcomingCount}", "upcoming", urgencyColor(Urgency.UPCOMING), Modifier.weight(1f)) { nav.navigate("items?status=UPCOMING") }
                StatTile("${o.monitoredCount}", "monitored", urgencyColor(Urgency.MONITORED), Modifier.weight(1f)) { nav.navigate("items?status=MONITORED") }
            }
        }

        val now = o.attention.filter { it.urgency(today) in setOf(Urgency.OVERDUE, Urgency.URGENT) }
        val soon = o.attention - now.toSet()
        if (now.isNotEmpty()) {
            item { SectionTitle("🔴 Needs attention now") }
            items(now, key = { "now-${it.id}" }) { ItemRow(it, today, onClick = { nav.navigate("item/${it.id}") }) }
        }
        if (soon.isNotEmpty()) {
            item { SectionTitle("🟠 Coming up") }
            items(soon.take(6), key = { "soon-${it.id}" }) { ItemRow(it, today, onClick = { nav.navigate("item/${it.id}") }) }
            if (soon.size > 6) item {
                TextButton(onClick = { nav.navigate("items?status=UPCOMING") }) { Text("See all ${soon.size} upcoming") }
            }
        }

        if (o.priceIncreases.isNotEmpty()) {
            item { SectionTitle("📈 Price changes") }
            items(o.priceIncreases, key = { "price-${it.item.id}" }) { ch ->
                InsightCard(onClick = { nav.navigate("item/${ch.item.id}") }) {
                    Text("${ch.item.title} increased", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${money(ch.old, ch.item.currency)} → ${money(ch.new, ch.item.currency)}  (+${ch.percent}%)",
                        color = urgencyColor(Urgency.URGENT), fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        if (o.unusedSubscriptions.isNotEmpty()) {
            item {
                SectionTitle("🟢 You could be saving money")
                InsightCard(onClick = { nav.goTab("subscriptions") }) {
                    val n = o.unusedSubscriptions.size
                    Text("We found $n subscription${if (n == 1) "" else "s"} you haven't used recently.", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Potential monthly saving: ${money(o.potentialSavings, cur)}", color = SavingsGreen, fontWeight = FontWeight.Bold)
                    Text(o.unusedSubscriptions.joinToString(" · ") { it.title }, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            SectionTitle("💰 Money")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    MoneyLine("Bills & renewals due this month", money(o.dueThisMonth, cur))
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    MoneyLine("Subscriptions per month", money(o.subscriptionsMonthly, cur))
                    if (o.potentialSavings > 0) {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        MoneyLine("Potential savings", money(o.potentialSavings, cur))
                    }
                }
            }
        }

        item {
            SectionTitle("📄 Documents")
            InsightCard(onClick = { nav.navigate("items?q=documents") }) {
                Text("${o.documents} document${if (o.documents == 1) "" else "s"}", style = MaterialTheme.typography.titleMedium)
                if (o.documentsExpiringSoon > 0) Text("${o.documentsExpiringSoon} expiring soon", color = urgencyColor(Urgency.UPCOMING))
                else Text("Nothing expiring soon", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (o.assets.isNotEmpty()) {
            item { SectionTitle("🚗 Your things") }
            items(o.assets.entries.toList(), key = { "asset-${it.key}" }) { (asset, list) ->
                InsightCard(onClick = { nav.navigate("items?q=${android.net.Uri.encode(asset)}") }) {
                    Text(asset, style = MaterialTheme.typography.titleMedium)
                    list.sortedBy { it.dueDate }.forEach { item ->
                        val d = item.daysLeft(today)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Dot(urgencyColor(item.urgency(today)), 8)
                            Spacer(Modifier.width(8.dp))
                            Text("${item.title}: ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(d?.let(::relativeDays) ?: "no date", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun StatTile(value: String, label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(color)
                Spacer(Modifier.width(6.dp))
                Text(value, style = MaterialTheme.typography.headlineMedium)
            }
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MoneyLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun InsightCard(onClick: () -> Unit, content: @Composable () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun EmptyHome(onScan: () -> Unit, onSample: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("📸", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text("Just take a photo", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Invoice, insurance policy, passport, Emirates ID, warranty, tenancy contract, utility bill… " +
                "LifeDesk reads it, works out what matters and reminds you before it's due.",
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onScan, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Outlined.CameraAlt, null)
            Spacer(Modifier.width(8.dp))
            Text("Scan my first document")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onSample, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Explore with sample data") }
    }
}
