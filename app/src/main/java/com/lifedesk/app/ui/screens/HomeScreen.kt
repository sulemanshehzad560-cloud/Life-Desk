package com.lifedesk.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Close
import com.lifedesk.app.ui.components.RadarSweep
import com.lifedesk.app.ui.components.TypewriterText
import com.lifedesk.app.ui.components.holoBorder
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.lifedesk.app.data.LifeItem
import com.lifedesk.app.domain.Urgency
import com.lifedesk.app.domain.daysLeft
import com.lifedesk.app.domain.forecast
import com.lifedesk.app.domain.money
import com.lifedesk.app.domain.overview
import com.lifedesk.app.domain.relativeDays
import com.lifedesk.app.domain.urgency
import com.lifedesk.app.ui.AppViewModel
import com.lifedesk.app.ui.components.CategoryTile
import com.lifedesk.app.ui.components.BannerAd
import com.lifedesk.app.ui.components.GlassCard
import com.lifedesk.app.ui.components.GlowDot
import com.lifedesk.app.ui.components.GradientButton
import com.lifedesk.app.ui.components.LivePulse
import com.lifedesk.app.ui.components.NeonBars
import com.lifedesk.app.ui.components.RingGauge
import com.lifedesk.app.ui.components.SectionHeader
import com.lifedesk.app.ui.components.StatPill
import com.lifedesk.app.ui.components.SwipeItemRow
import com.lifedesk.app.ui.components.accent
import com.lifedesk.app.ui.goTab
import com.lifedesk.app.ui.theme.Mono
import com.lifedesk.app.ui.theme.Neon
import com.lifedesk.app.ui.theme.urgencyColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.format.TextStyle as DateTextStyle

private val Ink = Color(0xFF06101E)

@Composable
fun HomeScreen(vm: AppViewModel, nav: NavHostController) {
    val items by vm.items.collectAsStateWithLifecycle()
    val prices by vm.prices.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val o = remember(items, prices) { overview(items, prices, today) }
    val cur = settings.currency

    val greeting = when (LocalTime.now().hour) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }
    val name = settings.name.trim().takeIf { it.isNotEmpty() }

    // 100 = nothing overdue or urgent; each overdue / urgent / upcoming item lowers the score.
    val overdue = items.count { it.urgency(today) == Urgency.OVERDUE }
    val score = (100 - overdue * 20 - (o.urgentCount - overdue) * 8 - o.upcomingCount * 2).coerceIn(0, 100)

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
    ) {
        // ---------------------------------------------------------------- header
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LivePulse()
                        Spacer(Modifier.width(6.dp))
                        Text(
                            today.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)).uppercase(),
                            style = MaterialTheme.typography.labelSmall, color = Neon.Muted,
                        )
                    }
                    Text(
                        greeting + (name?.let { ",\n$it" } ?: ""),
                        style = MaterialTheme.typography.headlineMedium.copy(brush = Brush.linearGradient(listOf(Neon.Text, Neon.Cyan))),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                HeaderButton(Icons.Outlined.Search) { nav.goTab("items") }
            }
        }

        // ---------------------------------------------------------------- hero: life score
        item {
            val glow = if (score >= 80) Neon.Green else if (score >= 50) Neon.Amber else Neon.Red
            GlassCard(Modifier.fillMaxWidth().padding(top = 16.dp).holoBorder(), glow = glow, padding = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val ringColors = when {
                        score >= 80 -> listOf(Neon.Green, Neon.Cyan, Neon.Green)
                        score >= 50 -> listOf(Neon.Amber, Neon.Cyan, Neon.Amber)
                        else -> listOf(Neon.Red, Neon.Pink, Neon.Red)
                    }
                    RingGauge(score / 100f, size = 108.dp, stroke = 10.dp, colors = ringColors) {
                        RadarSweep(Modifier.matchParentSize(), ringColors.first())
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$score", fontFamily = Mono, fontWeight = FontWeight.Black, fontSize = 32.sp, color = Neon.Text)
                            Text("LIFE SCORE", fontSize = 8.sp, letterSpacing = 1.4.sp, color = Neon.Muted)
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                items.isEmpty() -> "Let's set up your desk"
                                o.attention.isEmpty() -> "All clear"
                                else -> "${o.attention.size} need${if (o.attention.size == 1) "s" else ""} attention"
                            },
                            style = MaterialTheme.typography.titleLarge, color = Neon.Text,
                        )
                        Text(
                            when {
                                items.isEmpty() -> "Scan a bill, ID or policy to start tracking."
                                o.attention.isEmpty() -> "Nothing due soon. You're on top of everything."
                                else -> o.attention.first().let { "Next: ${it.title} ${it.daysLeft(today)?.let(::relativeDays).orEmpty()}" }
                            },
                            style = MaterialTheme.typography.bodySmall, color = Neon.Muted, modifier = Modifier.padding(top = 4.dp),
                        )
                        if (o.dueThisMonth > 0) {
                            Spacer(Modifier.height(10.dp))
                            Text("DUE THIS MONTH", style = MaterialTheme.typography.labelSmall, color = Neon.Muted)
                            Text(money(o.dueThisMonth, cur), fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Neon.Cyan)
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------- command bar
        item { CommandBar(vm) }
        item { AssistantPanel(vm, nav) }
        if (items.isNotEmpty()) item { BriefingCard(vm, items, prices, today, cur) }

        if (items.isEmpty()) {
            item { EmptyHome(onScan = { nav.goTab("scan") }) }
            return@LazyColumn
        }

        // ---------------------------------------------------------------- status pills
        item {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill("Urgent", "${o.urgentCount}", Neon.Red, Modifier.weight(1f)) { nav.navigate("items?status=URGENT") }
                StatPill("Soon", "${o.upcomingCount}", Neon.Amber, Modifier.weight(1f)) { nav.navigate("items?status=UPCOMING") }
                StatPill("Tracked", "${o.monitoredCount}", Neon.Green, Modifier.weight(1f)) { nav.navigate("items?status=MONITORED") }
            }
        }

        // ---------------------------------------------------------------- 14-day timeline
        item { Timeline(items, nav, today) }
        item { BannerAd() }

        // ---------------------------------------------------------------- attention lists
        val now = o.attention.filter { it.urgency(today) in setOf(Urgency.OVERDUE, Urgency.URGENT) }
        val soon = o.attention - now.toSet()
        if (now.isNotEmpty()) {
            item { SectionHeader("Needs attention now", color = Neon.Red) }
            items(now, key = { "now-${it.id}" }) {
                Box(Modifier.animateItem()) {
                    SwipeItemRow(it, today, onClick = { nav.navigate("item/${it.id}") }, onComplete = { vm.complete(it) }, onSnooze = { vm.snooze(it, 3) })
                }
            }
            item {
                Text(
                    "← swipe to snooze · swipe to complete →", style = MaterialTheme.typography.labelSmall, color = Neon.Faint,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
        if (soon.isNotEmpty()) {
            item { SectionHeader("Coming up", color = Neon.Amber) }
            items(soon.take(6), key = { "soon-${it.id}" }) {
                Box(Modifier.animateItem()) {
                    SwipeItemRow(it, today, onClick = { nav.navigate("item/${it.id}") }, onComplete = { vm.complete(it) }, onSnooze = { vm.snooze(it, 3) })
                }
            }
            if (soon.size > 6) item {
                TextButton(onClick = { nav.navigate("items?status=UPCOMING") }) { Text("See all ${soon.size} upcoming →", color = Neon.Cyan) }
            }
        }

        // ---------------------------------------------------------------- insights
        if (o.priceIncreases.isNotEmpty() || o.unusedSubscriptions.isNotEmpty()) {
            item { SectionHeader("Insights", color = Neon.Violet) }
        }
        items(o.priceIncreases, key = { "price-${it.item.id}" }) { ch ->
            InsightRow(
                icon = Icons.AutoMirrored.Outlined.TrendingUp, color = Neon.Red,
                title = "${ch.item.title} went up ${ch.percent}%",
                subtitle = "${money(ch.old, ch.item.currency)} → ${money(ch.new, ch.item.currency)}",
            ) { nav.navigate("item/${ch.item.id}") }
        }
        if (o.unusedSubscriptions.isNotEmpty()) item {
            val n = o.unusedSubscriptions.size
            InsightRow(
                icon = Icons.Outlined.Savings, color = Neon.Green,
                title = "Save ${money(o.potentialSavings, cur)}/month",
                subtitle = "$n subscription${if (n == 1) "" else "s"} unused 30+ days: " + o.unusedSubscriptions.joinToString(", ") { it.title },
            ) { nav.goTab("subscriptions") }
        }

        // ---------------------------------------------------------------- cash-flow
        item {
            val f = remember(items) { forecast(items, today, 6) }
            SectionHeader("Cash-flow · 6 months", color = Neon.Cyan)
            GlassCard(Modifier.fillMaxWidth(), onClick = { nav.goTab("subscriptions") }) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("PROJECTED OUTGOINGS", style = MaterialTheme.typography.labelSmall, color = Neon.Muted)
                        Text(money(f.sumOf { it.total }, cur), fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Neon.Text)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("SUBSCRIPTIONS", style = MaterialTheme.typography.labelSmall, color = Neon.Muted)
                        Text("${money(o.subscriptionsMonthly, cur)}/mo", fontFamily = Mono, fontWeight = FontWeight.SemiBold, color = Neon.Violet)
                    }
                }
                Spacer(Modifier.height(14.dp))
                NeonBars(
                    values = f.map { it.total },
                    labels = f.map { it.month.month.getDisplayName(DateTextStyle.SHORT, Locale.ENGLISH).uppercase() },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    highlight = 0,
                )
            }
        }

        // ---------------------------------------------------------------- vault & assets
        item {
            SectionHeader("Vault", color = Neon.Blue)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VaultTile(Icons.Outlined.Description, Neon.Blue, "${o.documents}", "documents", Modifier.weight(1f)) {
                    nav.navigate("items?q=documents")
                }
                VaultTile(Icons.Outlined.NotificationsActive, Neon.Amber, "${o.documentsExpiringSoon}", "expiring soon", Modifier.weight(1f)) {
                    nav.navigate("items?status=UPCOMING")
                }
            }
        }

        if (o.assets.isNotEmpty()) {
            item { SectionHeader("Your things", color = Neon.Green) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(o.assets.entries.toList(), key = { it.key }) { (asset, list) -> AssetCard(asset, list, today) {
                        nav.navigate("items?q=${android.net.Uri.encode(asset)}")
                    } }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun HeaderButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Neon.Surface2)
            .border(1.dp, Neon.Stroke, RoundedCornerShape(15.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Neon.Text) }
}

@Composable
private fun VaultTile(icon: ImageVector, color: Color, value: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier, glow = color, onClick = onClick, padding = 14.dp) {
        Icon(icon, null, tint = color)
        Text(value, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Neon.Text, modifier = Modifier.padding(top = 6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Neon.Muted)
    }
}

@Composable
private fun AssetCard(asset: String, list: List<LifeItem>, today: LocalDate, onClick: () -> Unit) {
    GlassCard(Modifier.width(230.dp), glow = list.first().category.accent, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryTile(list.first().category, 36.dp)
            Spacer(Modifier.width(10.dp))
            Text(asset, style = MaterialTheme.typography.titleMedium, color = Neon.Text, maxLines = 1)
        }
        Spacer(Modifier.height(8.dp))
        list.sortedBy { it.dueDate }.take(3).forEach { item ->
            val c = urgencyColor(item.urgency(today))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                GlowDot(c, 4.dp)
                Spacer(Modifier.width(6.dp))
                Text(item.title, style = MaterialTheme.typography.bodySmall, color = Neon.Muted, modifier = Modifier.weight(1f), maxLines = 1)
                Text(item.daysLeft(today)?.let { d -> if (d < 0) "late" else "${d}d" } ?: "—", fontFamily = Mono, fontSize = 12.sp, color = c)
            }
        }
    }
}

@Composable
private fun InsightRow(icon: ImageVector, color: Color, title: String, subtitle: String, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().padding(vertical = 5.dp), glow = color, onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = color) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Neon.Text)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Neon.Muted, maxLines = 2)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = Neon.Faint, modifier = Modifier.size(18.dp))
        }
    }
}

/** Next 14 days as a strip; tap a day to see what's due. */
@Composable
private fun Timeline(items: List<LifeItem>, nav: NavHostController, today: LocalDate) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val days = remember(today) { (0 until 14).map { today.plusDays(it.toLong()) } }
    val byDay = remember(items, today) { items.mapNotNull { i -> i.dueDate?.let { it to i } }.groupBy({ it.first }, { it.second }) }
    Column {
        SectionHeader("Next 14 days", color = Neon.Violet)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(days.size) { i ->
                val d = days[i]
                val due = byDay[d].orEmpty()
                val on = i == selected
                Column(
                    Modifier.width(52.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (on) Neon.Primary else Brush.verticalGradient(listOf(Neon.Surface2, Neon.Surface)))
                        .border(1.dp, if (on) Color.Transparent else Neon.Stroke, RoundedCornerShape(16.dp))
                        .clickable { selected = i }.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        d.dayOfWeek.getDisplayName(DateTextStyle.SHORT, Locale.ENGLISH).uppercase(), fontSize = 10.sp, letterSpacing = 1.sp,
                        color = if (on) Ink else Neon.Muted, fontWeight = FontWeight.SemiBold,
                    )
                    Text("${d.dayOfMonth}", fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (on) Ink else Neon.Text)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(8.dp).padding(top = 3.dp)) {
                        due.take(3).forEach { Box(Modifier.size(5.dp).clip(CircleShape).background(if (on) Ink else it.category.accent)) }
                    }
                }
            }
        }
        val sel = days[selected.coerceIn(0, days.lastIndex)]
        val list = byDay[sel].orEmpty()
        Column(Modifier.animateContentSize().padding(top = 8.dp)) {
            if (list.isEmpty()) {
                Text(
                    "Nothing due " + if (selected == 0) "today" else "on " + sel.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)),
                    style = MaterialTheme.typography.bodySmall, color = Neon.Faint, modifier = Modifier.padding(start = 4.dp),
                )
            } else list.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(Neon.Surface.copy(alpha = 0.6f))
                        .clickable { nav.navigate("item/${item.id}") }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryTile(item.category, 30.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(item.title, Modifier.weight(1f), color = Neon.Text, maxLines = 1)
                    item.amount?.let { Text(money(it, item.currency), fontFamily = Mono, fontSize = 13.sp, color = Neon.Cyan) }
                }
            }
        }
    }
}

/** Terminal-style quick add: "› DEWA bill 450 dirhams due next Friday". Type or speak. */
@Composable
private fun CommandBar(vm: AppViewModel) {
    var text by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) { vm.command(spoken, speak = true); text = "" }
    }
    fun submit() { if (vm.command(text)) text = "" }
    // Rotating hint: shows both halves of the bar — adding and asking.
    val hints = listOf("gym 250 monthly on the 1st", "when does my passport expire?", "DEWA 450 due next friday", "how much on subscriptions?")
    var hintIndex by remember { mutableIntStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(3500); hintIndex = (hintIndex + 1) % hints.size } }
    val hint = hints[hintIndex]
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp).height(56.dp).clip(RoundedCornerShape(18.dp))
            .background(Neon.Surface.copy(alpha = 0.9f))
            .border(1.dp, Brush.linearGradient(listOf(Neon.Cyan.copy(alpha = 0.6f), Neon.Violet.copy(alpha = 0.4f))), RoundedCornerShape(18.dp))
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.AutoAwesome, null, tint = Neon.Cyan, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("›", fontFamily = Mono, color = Neon.Cyan, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f)) {
            if (text.isEmpty()) Text(hint, fontFamily = Mono, fontSize = 14.sp, color = Neon.Faint, maxLines = 1)
            BasicTextField(
                value = text, onValueChange = { text = it }, singleLine = true,
                textStyle = TextStyle(fontFamily = Mono, fontSize = 14.sp, color = Neon.Text),
                cursorBrush = SolidColor(Neon.Cyan),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (text.isBlank()) {
            IconButton(onClick = {
                val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "e.g. “car insurance 2,850 dirhams due 10 October”")
                runCatching { voice.launch(intent) }.onFailure {
                    android.widget.Toast.makeText(context, "Voice input isn't available on this phone", android.widget.Toast.LENGTH_SHORT).show()
                }
            }) { Icon(Icons.Outlined.Mic, "Speak", tint = Neon.Violet) }
        } else {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Neon.Primary).clickable { submit() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Outlined.Send, "Add", tint = Ink, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun EmptyHome(onScan: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().padding(top = 16.dp), glow = Neon.Violet, padding = 22.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            RingGauge(0.72f, size = 110.dp, stroke = 6.dp) {
                Icon(Icons.Outlined.CameraAlt, null, tint = Neon.Cyan, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Just take a photo", style = MaterialTheme.typography.titleLarge, color = Neon.Text)
            Spacer(Modifier.height(6.dp))
            Text(
                "Invoices, policies, passports, Emirates ID, warranties, tenancy contracts, bills. LifeDesk reads them, extracts every date and amount, and reminds you before it's due.",
                textAlign = TextAlign.Center, color = Neon.Muted, style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(20.dp))
            GradientButton("Scan my first document", onScan, Modifier.fillMaxWidth(), icon = Icons.Outlined.CameraAlt)
        }
    }
}


/** The assistant's answer to a typed or spoken question, with matching items. */
@Composable
private fun AssistantPanel(vm: AppViewModel, nav: NavHostController) {
    val reply by vm.reply.collectAsStateWithLifecycle()
    androidx.compose.animation.AnimatedVisibility(
        visible = reply != null,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(),
    ) {
        val r = reply ?: return@AnimatedVisibility
        GlassCard(Modifier.fillMaxWidth().padding(top = 10.dp), glow = Neon.Violet, padding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = Neon.Violet, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("ASSISTANT", style = MaterialTheme.typography.labelSmall, color = Neon.Violet, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.speak(r.answer) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.VolumeUp, "Read aloud", tint = Neon.Cyan, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { vm.clearReply() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Close, "Close", tint = Neon.Muted, modifier = Modifier.size(18.dp))
                }
            }
            Text("› ${r.question}", fontFamily = Mono, fontSize = 12.sp, color = Neon.Muted, modifier = Modifier.padding(top = 4.dp))
            TypewriterText(r.answer, Modifier.padding(top = 6.dp), MaterialTheme.typography.titleMedium, Neon.Text)
            r.items.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(12.dp)).background(Neon.Surface.copy(alpha = 0.6f))
                        .clickable { nav.navigate("item/${item.id}") }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryTile(item.category, 28.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(item.title, Modifier.weight(1f), color = Neon.Text, maxLines = 1, fontSize = 14.sp)
                    Text(item.daysLeft(LocalDate.now())?.let { d -> if (d < 0) "late" else "${d}d" } ?: "—",
                        fontFamily = Mono, fontSize = 12.sp, color = urgencyColor(item.urgency(LocalDate.now())))
                }
            }
        }
    }
}

/** Daily AI briefing: a few sentences about what matters now, typed out and optionally read aloud. */
@Composable
private fun BriefingCard(vm: AppViewModel, items: List<LifeItem>, prices: List<com.lifedesk.app.data.PriceRecord>, today: LocalDate, cur: String) {
    val lines = remember(items, prices) { com.lifedesk.app.domain.Briefing.build(items, prices, today, cur) }
    SectionHeader("AI briefing", color = Neon.Pink)
    GlassCard(Modifier.fillMaxWidth(), glow = Neon.Pink, padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LivePulse(Neon.Pink)
            Spacer(Modifier.width(8.dp))
            Text("GENERATED ${java.time.LocalTime.now().withSecond(0).withNano(0)}", style = MaterialTheme.typography.labelSmall,
                color = Neon.Muted, modifier = Modifier.weight(1f))
            Row(
                Modifier.clip(RoundedCornerShape(10.dp)).background(Neon.Pink.copy(alpha = 0.15f))
                    .clickable { vm.speak(lines.joinToString(" ")) }.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Outlined.VolumeUp, null, tint = Neon.Pink, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Read aloud", fontSize = 12.sp, color = Neon.Pink, fontWeight = FontWeight.SemiBold)
            }
        }
        lines.forEachIndexed { i, line ->
            Row(Modifier.padding(top = 10.dp)) {
                Text("0${i + 1}", fontFamily = Mono, fontSize = 11.sp, color = Neon.Pink, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
                Spacer(Modifier.width(10.dp))
                TypewriterText(line, style = MaterialTheme.typography.bodyMedium, color = Neon.Text, speedMs = 10)
            }
        }
    }
}
