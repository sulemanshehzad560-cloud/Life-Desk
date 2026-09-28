package com.lifedesk.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.lifedesk.app.ui.components.GlassCard
import com.lifedesk.app.ui.components.GradientButton
import com.lifedesk.app.ui.components.RingGauge
import com.lifedesk.app.ui.components.SectionHeader
import com.lifedesk.app.ui.theme.Mono
import com.lifedesk.app.ui.theme.Neon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.lifedesk.app.data.Category
import com.lifedesk.app.data.DateKind
import com.lifedesk.app.data.LifeItem
import com.lifedesk.app.data.PriceRecord
import com.lifedesk.app.data.Recurrence
import com.lifedesk.app.domain.Urgency
import com.lifedesk.app.domain.money
import com.lifedesk.app.domain.percentChange
import com.lifedesk.app.domain.pretty
import com.lifedesk.app.ocr.DocumentStore
import com.lifedesk.app.ui.AppViewModel
import com.lifedesk.app.ui.components.DateField
import com.lifedesk.app.ui.components.Dropdown
import com.lifedesk.app.ui.theme.SavingsGreen
import com.lifedesk.app.ui.theme.urgencyColor

val currencies = listOf("AED", "SAR", "QAR", "OMR", "KWD", "BHD", "USD", "EUR", "GBP", "INR", "PKR")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(vm: AppViewModel, nav: NavHostController) {
    val draft by vm.draft.collectAsStateWithLifecycle()
    val d = draft
    if (d == null) {
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }
    var item by remember(d) { mutableStateOf(d.item) }
    var amountText by remember(d) { mutableStateOf(d.item.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var showText by remember { mutableStateOf(false) }
    var previous by remember { mutableStateOf<PriceRecord?>(null) }
    val bitmap = remember(item.imagePath) { item.imagePath?.let { DocumentStore.loadBitmap(it, 1200) } }

    LaunchedEffect(item.category, item.provider) { previous = vm.previousPrice(item.category, item.provider) }

    fun close() { vm.discardDraft(); nav.popBackStack() }
    BackHandler { close() }

    fun set(transform: LifeItem.() -> LifeItem) { item = item.transform() }
    fun opt(s: String) = s.ifBlank { null }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text(if (d.scanned) "EXTRACTION RESULT" else "EDITOR", style = MaterialTheme.typography.labelSmall, color = Neon.Cyan)
                        Text(if (item.id == 0L) "Review & save" else "Edit item", color = Neon.Text)
                    }
                },
                navigationIcon = { IconButton(onClick = ::close) { Icon(Icons.Outlined.Close, "Cancel") } },
                actions = {
                    TextButton(onClick = {
                        vm.saveDraft(item.copy(amount = amountText.replace(",", "").toDoubleOrNull())) { id ->
                            nav.navigate("item/$id") { popUpTo("home") }
                        }
                    }) { Text("SAVE", fontWeight = FontWeight.Black, color = Neon.Cyan, letterSpacing = 1.sp) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (bitmap != null) {
                Box(
                    Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Brush.linearGradient(listOf(Neon.Cyan, Neon.Violet)), RoundedCornerShape(20.dp)),
                ) {
                    Image(bitmap.asImageBitmap(), "Document photo", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Neon.Bg.copy(alpha = 0.8f)))))
                }
            }
            d.confidence?.let { c ->
                val (msg, color) = when {
                    c >= 0.8 -> "Document understood. Check the details and save." to Neon.Green
                    c >= 0.4 -> "Partial read. Fill in anything missing." to Neon.Amber
                    else -> "Couldn't read much. Fill it in — the photo stays attached." to Neon.Red
                }
                GlassCard(Modifier.fillMaxWidth(), glow = color, padding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RingGauge(c.toFloat(), size = 54.dp, stroke = 5.dp, colors = listOf(color, Neon.Cyan, color)) {
                            Text("${(c * 100).toInt()}%", fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Neon.Text)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("AI CONFIDENCE", style = MaterialTheme.typography.labelSmall, color = Neon.Muted)
                            Text(msg, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            val amount = amountText.replace(",", "").toDoubleOrNull()
            val prev = previous
            if (prev != null && amount != null && prev.amount != amount && prev.amount > 0) {
                val pct = percentChange(prev.amount, amount)
                val diff = amount - prev.amount
                val up = diff > 0
                GlassCard(Modifier.fillMaxWidth(), glow = if (up) Neon.Red else Neon.Green, padding = 0.dp) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "This is ${kotlin.math.abs(pct)}% ${if (up) "higher" else "lower"} than last time",
                            fontWeight = FontWeight.Bold,
                            color = if (up) urgencyColor(Urgency.URGENT) else SavingsGreen,
                        )
                        Text("Previous: ${money(prev.amount, prev.currency)} (${prev.date.pretty()})")
                        Text("Current: ${money(amount, item.currency)}")
                        Text("Difference: ${if (up) "+" else "−"}${money(kotlin.math.abs(diff), item.currency)}")
                    }
                }
            }

            d.parsed?.let { parsed ->
                FoundSection(
                    parsed = parsed,
                    currentDate = item.dueDate,
                    currentAmount = amountText.replace(",", "").toDoubleOrNull(),
                    onDate = { v -> set { copy(dueDate = v) } },
                    onAmount = { v, c -> amountText = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString(); set { copy(currency = c) } },
                    onField = { label, value -> set { copy(notes = listOfNotNull(notes, "$label: $value").joinToString("\n")) } },
                    onSchedule = {
                        vm.saveSchedule(item.copy(amount = amountText.replace(",", "").toDoubleOrNull()), parsed.schedule) {
                            nav.navigate("home") { popUpTo("home") { inclusive = true } }
                        }
                    },
                )
            }

            SectionHeader("Basics", color = Neon.Cyan)
            OutlinedTextField(item.title, { v -> set { copy(title = v) } }, label = { Text("What is it?") },
                placeholder = { Text("e.g. Car insurance, Passport, DEWA bill") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Dropdown("Type", item.category, Category.entries, { "${it.emoji} ${it.label}" }, { c ->
                set { copy(category = c, kind = c.defaultKind, recurrence = if (recurrence == Recurrence.NONE) c.defaultRecurrence else recurrence) }
            }, Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Dropdown("Date means", item.kind, DateKind.entries, { it.label }, { k -> set { copy(kind = k) } }, Modifier.weight(1f))
                DateField("Date", item.dueDate, { v -> set { copy(dueDate = v) } }, Modifier.weight(1f))
            }
            SectionHeader("Money", color = Neon.Violet)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(amountText, { amountText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } }, label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                Dropdown("Currency", item.currency, currencies, { it }, { c -> set { copy(currency = c) } }, Modifier.width(120.dp))
            }
            Dropdown("Repeats", item.recurrence, Recurrence.entries, { it.label }, { r -> set { copy(recurrence = r) } }, Modifier.fillMaxWidth())
            SectionHeader("Details", color = Neon.Blue)
            OutlinedTextField(item.provider ?: "", { v -> set { copy(provider = opt(v)) } }, label = { Text("Who is it with?") },
                placeholder = { Text("Provider, bank, insurer, landlord…") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(item.referenceNumber ?: "", { v -> set { copy(referenceNumber = opt(v)) } }, label = { Text("Reference / policy / account no.") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(item.asset ?: "", { v -> set { copy(asset = opt(v)) } }, label = { Text("Belongs to (optional)") },
                placeholder = { Text("e.g. Toyota Camry, Washing machine, Villa 12") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            if (item.category == Category.SUBSCRIPTION) {
                DateField("Last used", item.lastUsed, { v -> set { copy(lastUsed = v) } }, Modifier.fillMaxWidth())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(item.contactPhone ?: "", { v -> set { copy(contactPhone = opt(v)) } }, label = { Text("Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(item.contactEmail ?: "", { v -> set { copy(contactEmail = opt(v)) } }, label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(item.notes ?: "", { v -> set { copy(notes = opt(v)) } }, label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(), minLines = 2)

            item.rawText?.takeIf { it.isNotBlank() }?.let { raw ->
                TextButton(onClick = { showText = !showText }) { Text(if (showText) "Hide extracted text" else "Show extracted text") }
                if (showText) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                        Text(raw, Modifier.padding(12.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            GradientButton(
                if (item.id == 0L) "Add to my LifeDesk" else "Save changes",
                {
                    vm.saveDraft(item.copy(amount = amountText.replace(",", "").toDoubleOrNull())) { id -> nav.navigate("item/$id") { popUpTo("home") } }
                },
                Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}


/** "Everything we found": every date, amount, labelled field and any payment schedule in the document. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FoundSection(
    parsed: com.lifedesk.app.domain.ParsedDocument,
    currentDate: java.time.LocalDate?,
    currentAmount: Double?,
    onDate: (java.time.LocalDate) -> Unit,
    onAmount: (Double, String) -> Unit,
    onField: (String, String) -> Unit,
    onSchedule: () -> Unit,
) {
    var showAllFields by remember { mutableStateOf(false) }
    if (parsed.schedule.size >= 2) {
        GlassCard(Modifier.fillMaxWidth(), glow = Neon.Green, padding = 0.dp) {
            Column(Modifier.padding(14.dp)) {
                Text("📅 Payment schedule found: ${parsed.schedule.size} payments", fontWeight = FontWeight.Bold)
                parsed.schedule.forEachIndexed { i, (date, amount) ->
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text("${i + 1}. ${date.pretty()}", Modifier.weight(1f))
                        Text(money(amount, parsed.currency ?: "AED"), fontWeight = FontWeight.Medium)
                    }
                }
                Button(onClick = onSchedule, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Text("Create ${parsed.schedule.size} payment reminders")
                }
            }
        }
    }
    if (parsed.allDates.size > 1 || parsed.allAmounts.size > 1 || parsed.fields.isNotEmpty()) {
        GlassCard(Modifier.fillMaxWidth(), glow = Neon.Violet, padding = 0.dp) {
            Column(Modifier.padding(14.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🔎 Everything we found", fontWeight = FontWeight.Bold)
                if (parsed.allDates.isNotEmpty()) {
                    Text("Dates — tap to use", style = MaterialTheme.typography.labelMedium)
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        parsed.allDates.forEach { d ->
                            androidx.compose.material3.FilterChip(selected = d == currentDate, onClick = { onDate(d) }, label = { Text(d.pretty()) })
                        }
                    }
                }
                if (parsed.allAmounts.isNotEmpty()) {
                    Text("Amounts — tap to use", style = MaterialTheme.typography.labelMedium)
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        parsed.allAmounts.forEach { (v, c) ->
                            androidx.compose.material3.FilterChip(selected = v == currentAmount, onClick = { onAmount(v, c) }, label = { Text(money(v, c)) })
                        }
                    }
                }
                if (parsed.fields.isNotEmpty()) {
                    Text("Details — tap to add to notes", style = MaterialTheme.typography.labelMedium)
                    val shown = if (showAllFields) parsed.fields else parsed.fields.take(6)
                    shown.forEach { (label, value) ->
                        Row(Modifier.fillMaxWidth().clickable { onField(label, value) }.padding(vertical = 3.dp)) {
                            Text(label, Modifier.weight(0.45f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            Text(value, Modifier.weight(0.55f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (parsed.fields.size > 6) TextButton(onClick = { showAllFields = !showAllFields }) {
                        Text(if (showAllFields) "Show less" else "Show all ${parsed.fields.size}")
                    }
                }
            }
        }
    }
}
