package com.lifedesk.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.lifedesk.app.data.Category
import com.lifedesk.app.data.LifeItem
import com.lifedesk.app.data.PriceRecord
import com.lifedesk.app.data.Recurrence
import com.lifedesk.app.domain.ActionType
import com.lifedesk.app.domain.completeLabel
import com.lifedesk.app.domain.daysSinceUsed
import com.lifedesk.app.domain.headline
import com.lifedesk.app.domain.isSnoozed
import com.lifedesk.app.domain.isUnusedSubscription
import com.lifedesk.app.domain.money
import com.lifedesk.app.domain.percentChange
import com.lifedesk.app.domain.pretty
import com.lifedesk.app.domain.renewalChecklist
import com.lifedesk.app.domain.suggestedActions
import com.lifedesk.app.domain.urgency
import com.lifedesk.app.ocr.DocumentStore
import com.lifedesk.app.ui.AppViewModel
import com.lifedesk.app.ui.components.InfoRow
import com.lifedesk.app.ui.components.SectionTitle
import com.lifedesk.app.ui.theme.urgencyColor
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemDetailScreen(vm: AppViewModel, nav: NavHostController, id: Long) {
    val itemFlow = remember(id) { vm.observeItem(id) }
    val item by itemFlow.collectAsState(initial = null)
    var loaded by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf<List<PriceRecord>>(emptyList()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showImage by remember { mutableStateOf(false) }
    var showChecklist by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val today = LocalDate.now()

    LaunchedEffect(item) {
        if (item != null) loaded = true
        history = vm.priceHistory(id)
    }
    val entry = item
    if (entry == null) {
        // Deleted, or still loading.
        if (loaded) LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { vm.startEdit(entry); nav.navigate("edit") }) { Icon(Icons.Outlined.Edit, "Edit") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Delete") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.category.emoji, style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(entry.title, style = MaterialTheme.typography.headlineMedium)
                    Text(listOfNotNull(entry.category.label, entry.provider).joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))

            val u = entry.urgency(today)
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text(entry.headline(today), style = MaterialTheme.typography.titleLarge, color = urgencyColor(u))
                    entry.dueDate?.let { d -> Text("${entry.kind.label}: ${d.pretty()}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    entry.amount?.let { a ->
                        Text(
                            money(a, entry.currency) + if (entry.recurrence != Recurrence.NONE) " · ${entry.recurrence.label.lowercase()}" else "",
                            style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    if (entry.isSnoozed(today)) Text("😴 Snoozed until ${entry.snoozedUntil!!.pretty()}", modifier = Modifier.padding(top = 6.dp))
                    if (entry.category == Category.SUBSCRIPTION) {
                        val days = entry.daysSinceUsed(today)
                        Text(
                            if (entry.lastUsed == null) "Last used: not recorded" else "Last used: ${if (days == 0L) "today" else "$days days ago"}",
                            color = if (entry.isUnusedSubscription(today)) urgencyColor(com.lifedesk.app.domain.Urgency.URGENT) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }

            SectionTitle("Actions")
            Actions(entry, vm, nav, context, onChecklist = { showChecklist = !showChecklist })

            if (showChecklist) {
                SectionTitle("What you'll need")
                val done = remember(entry.id) { mutableStateListOf<Int>() }
                renewalChecklist(entry).forEachIndexed { i, step ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { if (i in done) done.remove(i) else done.add(i) }) {
                        Checkbox(checked = i in done, onCheckedChange = { c -> if (c) done.add(i) else done.remove(i) })
                        Text(step)
                    }
                }
            }

            SectionTitle("Details")
            InfoRow("Reference", entry.referenceNumber)
            InfoRow("Belongs to", entry.asset)
            InfoRow("Repeats", entry.recurrence.takeIf { r -> r != Recurrence.NONE }?.label)
            InfoRow("Phone", entry.contactPhone) { dial(context, entry.contactPhone!!) }
            InfoRow("Email", entry.contactEmail) { email(context, entry.contactEmail!!, entry) }
            InfoRow("Notes", entry.notes)

            if (history.size > 1) {
                SectionTitle("Price history")
                history.forEachIndexed { i, p ->
                    val older = history.getOrNull(i + 1)
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(p.date.pretty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row {
                            Text(money(p.amount, p.currency), fontWeight = FontWeight.Medium)
                            if (older != null && older.amount > 0 && older.amount != p.amount) {
                                val pct = percentChange(older.amount, p.amount)
                                Text("  ${if (pct > 0) "+" else ""}$pct%", color = urgencyColor(if (pct > 0) com.lifedesk.app.domain.Urgency.URGENT else com.lifedesk.app.domain.Urgency.MONITORED))
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }

            entry.imagePath?.let { path ->
                val bmp = remember(path) { DocumentStore.loadBitmap(path, 1200) }
                if (bmp != null) {
                    SectionTitle("Document")
                    Card(shape = RoundedCornerShape(16.dp), onClick = { showImage = true }) {
                        Image(bmp.asImageBitmap(), "Document", Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Crop)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showImage) entry.imagePath?.let { path ->
        val full = remember(path) { DocumentStore.loadBitmap(path, 2400) }
        Dialog(onDismissRequest = { showImage = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().clickable { showImage = false }, contentAlignment = Alignment.Center) {
                full?.let { b -> Image(b.asImageBitmap(), "Document", Modifier.fillMaxWidth(), contentScale = ContentScale.Fit) }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${entry.title}?") },
            text = { Text("This removes the item, its reminders and the stored photo.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(entry) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Actions(item: LifeItem, vm: AppViewModel, nav: NavHostController, context: Context, onChecklist: () -> Unit) {
    var snoozeMenu by remember { mutableStateOf(false) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item.suggestedActions().forEach { action ->
            when (action) {
                ActionType.COMPLETE -> Button(onClick = { vm.complete(item) }) { IconText(Icons.Outlined.CheckCircle, item.completeLabel()) }
                ActionType.SNOOZE -> Box {
                    OutlinedButton(onClick = { snoozeMenu = true }) { IconText(Icons.Outlined.Snooze, "Remind me later") }
                    DropdownMenu(expanded = snoozeMenu, onDismissRequest = { snoozeMenu = false }) {
                        listOf(1L to "Tomorrow", 3L to "In 3 days", 7L to "In a week", 30L to "In a month").forEach { (days, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { snoozeMenu = false; vm.snooze(item, days) })
                        }
                    }
                }
                ActionType.CONTACT -> when {
                    item.contactPhone != null -> OutlinedButton(onClick = { dial(context, item.contactPhone) }) { IconText(Icons.Outlined.Call, "Call ${item.provider ?: "provider"}") }
                    item.contactEmail != null -> OutlinedButton(onClick = { email(context, item.contactEmail, item) }) { IconText(Icons.Outlined.Email, "Email ${item.provider ?: "provider"}") }
                    item.provider != null -> OutlinedButton(onClick = { web(context, "${item.provider} customer service contact UAE") }) {
                        IconText(Icons.Outlined.TravelExplore, "Contact ${item.provider}")
                    }
                    else -> Unit
                }
                ActionType.COMPARE -> FilledTonalButton(onClick = { web(context, "compare ${item.title.lowercase()} renewal quotes UAE") }) {
                    IconText(Icons.Outlined.CompareArrows, "Compare renewal")
                }
                ActionType.PLAN_RENEWAL -> FilledTonalButton(onClick = onChecklist) { IconText(Icons.Outlined.FactCheck, "Plan renewal") }
                ActionType.WARRANTY_CLAIM -> FilledTonalButton(onClick = { web(context, "${item.provider ?: ""} ${item.asset ?: item.title} warranty claim") }) {
                    IconText(Icons.Outlined.FactCheck, "Check warranty claim")
                }
                ActionType.MARK_USED -> FilledTonalButton(onClick = { vm.markUsed(item) }) { IconText(Icons.Outlined.ThumbUp, "I used it today") }
                ActionType.REVIEW_SUBSCRIPTION -> OutlinedButton(onClick = { web(context, "how to cancel ${item.provider ?: item.title} subscription") }) {
                    IconText(Icons.Outlined.TravelExplore, "Review / cancel")
                }
                ActionType.NEW_DOCUMENT -> OutlinedButton(onClick = { nav.navigate("scan?replace=${item.id}") }) { IconText(Icons.Outlined.UploadFile, "Upload new document") }
                ActionType.ADD_TO_CALENDAR -> OutlinedButton(onClick = { addToCalendar(context, item) }) { IconText(Icons.Outlined.Event, "Add to calendar") }
                ActionType.SHARE -> OutlinedButton(onClick = { share(context, item) }) { IconText(Icons.Outlined.Share, "Share") }
            }
        }
    }
}

@Composable
private fun IconText(icon: ImageVector, text: String) {
    Icon(icon, null, Modifier.padding(end = 6.dp))
    Text(text)
}

private fun launch(context: Context, intent: Intent) {
    try { context.startActivity(intent) } catch (_: ActivityNotFoundException) { }
}

private fun dial(context: Context, phone: String) = launch(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.filter { c -> c.isDigit() || c == '+' })))

private fun email(context: Context, address: String, item: LifeItem) = launch(
    context,
    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")).putExtra(
        Intent.EXTRA_SUBJECT, listOfNotNull(item.title, item.referenceNumber?.let { "Ref $it" }).joinToString(" — "),
    ),
)

/** All-day event in Google Calendar (or any calendar app) with the item's details. */
private fun addToCalendar(context: Context, item: LifeItem) {
    val date = item.dueDate ?: return
    val start = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    val intent = Intent(Intent.ACTION_INSERT).setData(android.provider.CalendarContract.Events.CONTENT_URI)
        .putExtra(android.provider.CalendarContract.Events.TITLE, "${item.category.emoji} ${item.title} — ${item.kind.label.lowercase()}")
        .putExtra(android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        .putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        .putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, start + 86_400_000L)
        .putExtra(android.provider.CalendarContract.Events.DESCRIPTION, shareText(item))
    launch(context, intent)
}

private fun shareText(item: LifeItem): String = buildString {
    appendLine("${item.category.emoji} ${item.title}")
    item.dueDate?.let { appendLine("${item.kind.label}: ${it.pretty()}") }
    item.amount?.let { appendLine("Amount: ${money(it, item.currency)}") }
    item.provider?.let { appendLine("Provider: $it") }
    item.referenceNumber?.let { appendLine("Reference: $it") }
    item.notes?.let { appendLine(it) }
    append("— from LifeDesk")
}

/** Share details (e.g. with a spouse) via WhatsApp, email, etc. */
private fun share(context: Context, item: LifeItem) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText(item))
    launch(context, Intent.createChooser(send, "Share ${item.title}"))
}

private fun web(context: Context, query: String) =
    launch(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query.trim()))))
