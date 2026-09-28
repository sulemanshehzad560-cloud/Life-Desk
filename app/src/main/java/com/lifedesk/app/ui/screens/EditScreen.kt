package com.lifedesk.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
        topBar = {
            TopAppBar(
                title = { Text(if (item.id == 0L) "Review & save" else "Edit") },
                navigationIcon = { IconButton(onClick = ::close) { Icon(Icons.Outlined.Close, "Cancel") } },
                actions = {
                    TextButton(onClick = {
                        vm.saveDraft(item.copy(amount = amountText.replace(",", "").toDoubleOrNull())) { id ->
                            nav.navigate("item/$id") { popUpTo("home") }
                        }
                    }) { Text("Save", fontWeight = FontWeight.Bold) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (bitmap != null) {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Image(bitmap.asImageBitmap(), "Document photo", Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Crop)
                }
            }
            d.confidence?.let { c ->
                val (msg, color) = when {
                    c >= 0.8 -> "✅ I've read this document. Check the details below and save." to SavingsGreen
                    c >= 0.4 -> "🔎 I found some details — please fill in anything missing." to urgencyColor(Urgency.UPCOMING)
                    else -> "✍️ I couldn't read much from this photo. Fill in the details and the photo stays attached." to urgencyColor(Urgency.UPCOMING)
                }
                Text(msg, color = color, fontWeight = FontWeight.Medium)
            }

            val amount = amountText.replace(",", "").toDoubleOrNull()
            val prev = previous
            if (prev != null && amount != null && prev.amount != amount && prev.amount > 0) {
                val pct = percentChange(prev.amount, amount)
                val diff = amount - prev.amount
                val up = diff > 0
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
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

            OutlinedTextField(item.title, { v -> set { copy(title = v) } }, label = { Text("What is it?") },
                placeholder = { Text("e.g. Car insurance, Passport, DEWA bill") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Dropdown("Type", item.category, Category.entries, { "${it.emoji} ${it.label}" }, { c ->
                set { copy(category = c, kind = c.defaultKind, recurrence = if (recurrence == Recurrence.NONE) c.defaultRecurrence else recurrence) }
            }, Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Dropdown("Date means", item.kind, DateKind.entries, { it.label }, { k -> set { copy(kind = k) } }, Modifier.weight(1f))
                DateField("Date", item.dueDate, { v -> set { copy(dueDate = v) } }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(amountText, { amountText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } }, label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                Dropdown("Currency", item.currency, currencies, { it }, { c -> set { copy(currency = c) } }, Modifier.width(120.dp))
            }
            Dropdown("Repeats", item.recurrence, Recurrence.entries, { it.label }, { r -> set { copy(recurrence = r) } }, Modifier.fillMaxWidth())
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
            Button(
                onClick = {
                    vm.saveDraft(item.copy(amount = amountText.replace(",", "").toDoubleOrNull())) { id -> nav.navigate("item/$id") { popUpTo("home") } }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(if (item.id == 0L) "Add to my LifeDesk" else "Save changes") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
