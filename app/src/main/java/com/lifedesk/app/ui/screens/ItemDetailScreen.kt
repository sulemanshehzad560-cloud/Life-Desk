package com.lifedesk.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.lifedesk.app.data.monthlyCost
import com.lifedesk.app.domain.daysLeft
import com.lifedesk.app.ui.components.CategoryTile
import com.lifedesk.app.ui.components.CountdownRing
import com.lifedesk.app.ui.components.GlassCard
import com.lifedesk.app.ui.components.GlowDot
import com.lifedesk.app.ui.components.InfoTile
import com.lifedesk.app.ui.components.NeonProgress
import com.lifedesk.app.ui.components.SectionHeader
import com.lifedesk.app.ui.components.Sparkline
import com.lifedesk.app.ui.components.Tag
import com.lifedesk.app.ui.components.accent
import com.lifedesk.app.ui.theme.Mono
import com.lifedesk.app.ui.theme.Neon
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

@Composable
private fun RoundIcon(icon: ImageVector, desc: String, tint: Color = Neon.Text, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(Neon.Surface2.copy(alpha = 0.85f)).border(1.dp, Neon.Stroke, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(20.dp)) }
}

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

    val u = entry.urgency(today)
    val uc = urgencyColor(u)
    val accent = entry.category.accent
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // ------------------------------------------------ hero
        Box(
            Modifier.fillMaxWidth().background(
                Brush.verticalGradient(listOf(accent.copy(alpha = 0.28f), Neon.Violet.copy(alpha = 0.10f), Color.Transparent)),
            ),
        ) {
            Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    RoundIcon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") { nav.popBackStack() }
                    Spacer(Modifier.weight(1f))
                    RoundIcon(Icons.Outlined.Edit, "Edit") { vm.startEdit(entry); nav.navigate("edit") }
                    Spacer(Modifier.width(8.dp))
                    RoundIcon(Icons.Outlined.Delete, "Delete", Neon.Red) { confirmDelete = true }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)) {
                    Column(Modifier.weight(1f)) {
                        CategoryTile(entry.category, 52.dp)
                        Text(entry.title, style = MaterialTheme.typography.headlineMedium, color = Neon.Text, modifier = Modifier.padding(top = 12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                            Tag(entry.category.label, accent)
                            if (entry.recurrence != Recurrence.NONE) Tag(entry.recurrence.label, Neon.Violet)
                            if (entry.isSnoozed(today)) Tag("Snoozed", Neon.Amber)
                        }
                        entry.provider?.let { Text(it, color = Neon.Muted, modifier = Modifier.padding(top = 6.dp)) }
                    }
                    CountdownRing(entry.daysLeft(today), entry.category.upcomingDays, uc, size = 104.dp)
                }
                GlassCard(Modifier.fillMaxWidth().padding(top = 8.dp), glow = uc, padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlowDot(uc, 6.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(entry.headline(today), style = MaterialTheme.typography.titleMedium, color = uc)
                    }
                    if (entry.category == Category.SUBSCRIPTION) {
                        val days = entry.daysSinceUsed(today)
                        Text(
                            if (entry.lastUsed == null) "Last used: not recorded" else "Last used ${if (days == 0L) "today" else "$days days ago"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (entry.isUnusedSubscription(today)) Neon.Red else Neon.Muted,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    if (entry.isSnoozed(today)) Text("Snoozed until ${entry.snoozedUntil!!.pretty()}", style = MaterialTheme.typography.bodySmall,
                        color = Neon.Amber, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            // ------------------------------------------------ key facts grid
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 14.dp)) {
                InfoTile("Amount", entry.amount?.let { money(it, entry.currency) } ?: "—", Neon.Cyan)
                InfoTile(entry.kind.label, entry.dueDate?.pretty() ?: "No date")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                InfoTile("Reference", entry.referenceNumber ?: "—")
                InfoTile("Monthly cost", entry.monthlyCost?.let { money(it, entry.currency) } ?: "—", Neon.Violet)
            }

            SectionHeader("Actions", color = Neon.Cyan)
            Actions(entry, vm, nav, context, onChecklist = { showChecklist = !showChecklist })

            if (showChecklist) {
                SectionHeader("Renewal checklist", color = Neon.Amber)
                val done = remember(entry.id) { mutableStateListOf<Int>() }
                val steps = renewalChecklist(entry)
                GlassCard(Modifier.fillMaxWidth(), glow = Neon.Amber, padding = 10.dp) {
                    NeonProgress(done.size / steps.size.toFloat(), Modifier.fillMaxWidth().padding(6.dp), Neon.Warm)
                    steps.forEachIndexed { i, step ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { if (i in done) done.remove(i) else done.add(i) }) {
                            Checkbox(checked = i in done, onCheckedChange = { c -> if (c) done.add(i) else done.remove(i) },
                                colors = CheckboxDefaults.colors(checkedColor = Neon.Amber, checkmarkColor = Color(0xFF06101E)))
                            Text(step, color = if (i in done) Neon.Faint else Neon.Text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            SectionHeader("Details", color = Neon.Violet)
            GlassCard(Modifier.fillMaxWidth(), glow = Neon.Stroke, padding = 14.dp) {
                InfoRow("Category", entry.category.label)
                InfoRow("Belongs to", entry.asset)
                InfoRow("Repeats", entry.recurrence.takeIf { r -> r != Recurrence.NONE }?.label)
                InfoRow("Phone", entry.contactPhone) { dial(context, entry.contactPhone!!) }
                InfoRow("Email", entry.contactEmail) { email(context, entry.contactEmail!!, entry) }
                InfoRow("Notes", entry.notes)
                InfoRow("Added", java.time.Instant.ofEpochMilli(entry.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate().pretty())
            }

            if (history.size > 1) {
                SectionHeader("Price history", color = Neon.Pink)
                GlassCard(Modifier.fillMaxWidth(), glow = Neon.Pink) {
                    val chron = history.reversed()
                    val first = chron.first().amount
                    val last = chron.last().amount
                    val pct = if (first > 0) percentChange(first, last) else 0
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(money(last, chron.last().currency), fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Neon.Text, modifier = Modifier.weight(1f))
                        Text("${if (pct > 0) "▲ +" else if (pct < 0) "▼ " else ""}$pct%", fontFamily = Mono, fontWeight = FontWeight.Bold,
                            color = if (pct > 0) Neon.Red else Neon.Green)
                    }
                    Sparkline(chron.map { it.amount }, Modifier.fillMaxWidth().height(90.dp).padding(top = 10.dp), if (pct > 0) Neon.Pink else Neon.Green)
                    chron.reversed().forEach { p ->
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                            Text(p.date.pretty(), style = MaterialTheme.typography.bodySmall, color = Neon.Muted, modifier = Modifier.weight(1f))
                            Text(money(p.amount, p.currency), fontFamily = Mono, fontSize = 13.sp, color = Neon.Text)
                        }
                    }
                }
            }

            entry.imagePath?.let { path ->
                val bmp = remember(path) { DocumentStore.loadBitmap(path, 1200) }
                if (bmp != null) {
                    SectionHeader("Document", color = Neon.Blue)
                    Box(
                        Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(20.dp))
                            .border(1.dp, Brush.linearGradient(listOf(Neon.Blue, Neon.Stroke)), RoundedCornerShape(20.dp))
                            .clickable { showImage = true },
                    ) {
                        Image(bmp.asImageBitmap(), "Document", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Neon.Bg.copy(alpha = 0.85f)))))
                        Text("TAP TO VIEW FULL DOCUMENT", style = MaterialTheme.typography.labelSmall, color = Neon.Text,
                            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp))
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
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
                ActionType.COMPLETE -> Pill(true, onClick = { vm.complete(item) }) { IconText(Icons.Outlined.CheckCircle, item.completeLabel()) }
                ActionType.SNOOZE -> Box {
                    Pill(false, onClick = { snoozeMenu = true }) { IconText(Icons.Outlined.Snooze, "Remind me later") }
                    DropdownMenu(expanded = snoozeMenu, onDismissRequest = { snoozeMenu = false }) {
                        listOf(1L to "Tomorrow", 3L to "In 3 days", 7L to "In a week", 30L to "In a month").forEach { (days, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { snoozeMenu = false; vm.snooze(item, days) })
                        }
                    }
                }
                ActionType.CONTACT -> when {
                    item.contactPhone != null -> Pill(false, onClick = { dial(context, item.contactPhone) }) { IconText(Icons.Outlined.Call, "Call ${item.provider ?: "provider"}") }
                    item.contactEmail != null -> Pill(false, onClick = { email(context, item.contactEmail, item) }) { IconText(Icons.Outlined.Email, "Email ${item.provider ?: "provider"}") }
                    item.provider != null -> Pill(false, onClick = { web(context, "${item.provider} customer service contact UAE") }) {
                        IconText(Icons.Outlined.TravelExplore, "Contact ${item.provider}")
                    }
                    else -> Unit
                }
                ActionType.COMPARE -> Pill(false, onClick = { web(context, "compare ${item.title.lowercase()} renewal quotes UAE") }) {
                    IconText(Icons.Outlined.CompareArrows, "Compare renewal")
                }
                ActionType.PLAN_RENEWAL -> Pill(false, onClick = onChecklist) { IconText(Icons.Outlined.FactCheck, "Plan renewal") }
                ActionType.WARRANTY_CLAIM -> Pill(false, onClick = { web(context, "${item.provider ?: ""} ${item.asset ?: item.title} warranty claim") }) {
                    IconText(Icons.Outlined.FactCheck, "Check warranty claim")
                }
                ActionType.MARK_USED -> Pill(false, onClick = { vm.markUsed(item) }) { IconText(Icons.Outlined.ThumbUp, "I used it today") }
                ActionType.REVIEW_SUBSCRIPTION -> Pill(false, onClick = { web(context, "how to cancel ${item.provider ?: item.title} subscription") }) {
                    IconText(Icons.Outlined.TravelExplore, "Review / cancel")
                }
                ActionType.NEW_DOCUMENT -> Pill(false, onClick = { nav.navigate("scan?replace=${item.id}") }) { IconText(Icons.Outlined.UploadFile, "Upload new document") }
                ActionType.ADD_TO_CALENDAR -> Pill(false, onClick = { addToCalendar(context, item) }) { IconText(Icons.Outlined.Event, "Add to calendar") }
                ActionType.SHARE -> Pill(false, onClick = { share(context, item) }) { IconText(Icons.Outlined.Share, "Share") }
            }
        }
    }
}

@Composable
private fun IconText(icon: ImageVector, text: String) {
    Icon(icon, null, Modifier.padding(end = 6.dp).size(18.dp))
    Text(text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
}

/** Glass action pill; the primary one is filled with the brand gradient. */
@Composable
private fun Pill(primary: Boolean, onClick: () -> Unit, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalContentColor provides if (primary) Color(0xFF06101E) else Neon.Text,
    ) {
        Row(
            Modifier.height(44.dp).clip(shape)
                .background(if (primary) Neon.Primary else Brush.verticalGradient(listOf(Neon.Surface2, Neon.Surface)))
                .border(1.dp, if (primary) Color.Transparent else Neon.Stroke, shape)
                .clickable(onClick = onClick).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
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
