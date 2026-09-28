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

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text("Everything", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                placeholder = { Text("Ask anything… “When does my insurance expire?”") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Clear") } },
                singleLine = true, shape = RoundedCornerShape(28.dp),
            )
            if (query.isEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { s -> AssistChip(onClick = { query = s }, label = { Text(s) }) }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusFilter.entries.forEach { f -> FilterChip(selected = status == f, onClick = { status = f }, label = { Text(f.label) }) }
            }
            if (usedCategories.size > 1) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    usedCategories.forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = if (category == c) null else c }, label = { Text("${c.emoji} ${c.label}") })
                    }
                }
            }
            if (query.isNotBlank() && result.answer != null) {
                Card(Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) { Text(result.answer, style = MaterialTheme.typography.titleMedium) }
                }
            }
        }
        if (result.items.isEmpty()) {
            item {
                Text(
                    if (items.isEmpty()) "Nothing here yet. Tap Scan to add your first document." else "No matching items.",
                    modifier = Modifier.padding(vertical = 32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(result.items, key = { it.id }) {
            SwipeItemRow(it, today, onClick = { nav.navigate("item/${it.id}") }, onComplete = { vm.complete(it) }, onSnooze = { vm.snooze(it, 3) })
        }
    }
}
