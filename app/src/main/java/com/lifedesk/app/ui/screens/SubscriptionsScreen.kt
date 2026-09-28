package com.lifedesk.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.lifedesk.app.data.Category
import com.lifedesk.app.data.monthlyCost
import com.lifedesk.app.domain.Urgency
import com.lifedesk.app.domain.daysSinceUsed
import com.lifedesk.app.domain.isUnusedSubscription
import com.lifedesk.app.domain.money
import com.lifedesk.app.ui.AppViewModel
import com.lifedesk.app.ui.theme.SavingsGreen
import com.lifedesk.app.ui.theme.urgencyColor
import java.time.LocalDate

@Composable
fun SubscriptionsScreen(vm: AppViewModel, nav: NavHostController) {
    val items by vm.items.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val subs = items.filter { it.category == Category.SUBSCRIPTION }
        .sortedWith(compareByDescending<com.lifedesk.app.data.LifeItem> { it.isUnusedSubscription(today) }.thenByDescending { it.monthlyCost ?: 0.0 })
    val monthly = subs.mapNotNull { it.monthlyCost }.sum()
    val unused = subs.filter { it.isUnusedSubscription(today) }
    val saving = unused.mapNotNull { it.monthlyCost }.sum()
    val cur = settings.currency

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item {
                Text("Where is my money going?", style = MaterialTheme.typography.headlineMedium)
                Card(Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("You're paying", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${money(monthly, cur)}/month", style = MaterialTheme.typography.headlineMedium)
                        Text("${money(monthly * 12, cur)} a year on ${subs.size} subscription${if (subs.size == 1) "" else "s"}")
                    }
                }
                if (unused.isNotEmpty()) {
                    Card(Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(Modifier.padding(16.dp)) {
                            unused.forEach { s ->
                                Text("You haven't used ${s.title} in ${s.daysSinceUsed(today)} days.", fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Cancel them to save ${money(saving, cur)}/month (${money(saving * 12, cur)}/year)", color = SavingsGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp)) {
                    Text("Subscription", Modifier.weight(1.4f), style = MaterialTheme.typography.labelLarge)
                    Text("Monthly", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                    Text("Last used", Modifier.weight(1.2f), style = MaterialTheme.typography.labelLarge)
                }
            }
            if (subs.isEmpty()) item {
                Text("No subscriptions yet. Scan a subscription receipt or add one manually.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp))
            }
            items(subs, key = { it.id }) { s ->
                val flagged = s.isUnusedSubscription(today)
                Card(onClick = { nav.navigate("item/${s.id}") }, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(s.title, Modifier.weight(1.4f), fontWeight = FontWeight.Medium, maxLines = 1)
                        Text(s.monthlyCost?.let { money(it, s.currency) } ?: "—", Modifier.weight(1f))
                        Column(Modifier.weight(1.2f)) {
                            val d = s.daysSinceUsed(today)
                            Text(
                                if (s.lastUsed == null) "—" else if (d == 0L) "Today" else "$d days ago",
                                color = if (flagged) urgencyColor(Urgency.URGENT) else MaterialTheme.colorScheme.onSurface,
                            )
                            TextButton(onClick = { vm.markUsed(s) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(28.dp)) {
                                Text("Used today", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        ExtendedFloatingActionButton(
            onClick = { vm.startManual(Category.SUBSCRIPTION); nav.navigate("edit") },
            icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Add subscription") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}
