package com.lifedesk.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifedesk.app.ui.AppViewModel
import com.lifedesk.app.ui.components.GlassCard
import com.lifedesk.app.ui.components.GradientButton
import com.lifedesk.app.ui.components.RingGauge
import com.lifedesk.app.ui.components.neonFieldColors
import com.lifedesk.app.ui.theme.Neon
import kotlinx.coroutines.launch

private data class Slide(val icon: ImageVector, val color: Color, val kicker: String, val title: String, val body: String, val fill: Float)

private val slides = listOf(
    Slide(Icons.Outlined.DocumentScanner, Neon.Cyan, "CAPTURE", "Snap anything with a date",
        "Bills, policies, passports, Emirates ID, tenancy contracts, warranties, PDFs and emails. On-device AI extracts every date, amount and reference.", 0.35f),
    Slide(Icons.Outlined.NotificationsActive, Neon.Amber, "REMIND", "Nudged at the right time",
        "Days ahead for bills, months ahead for passports and visas. Mark paid or snooze straight from the notification.", 0.6f),
    Slide(Icons.Outlined.Insights, Neon.Violet, "ANALYSE", "See your money clearly",
        "12-month cash-flow, price-increase alerts and subscriptions you forgot you pay for.", 0.85f),
    Slide(Icons.Outlined.AutoAwesome, Neon.Green, "YOURS", "Private and synced",
        "Everything is processed on your phone. Add an account to back up and restore anywhere.", 1f),
)

@Composable
fun WelcomeScreen(vm: AppViewModel, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val pager = rememberPagerState { slides.size + 1 }
    val scope = rememberCoroutineScope()
    fun finish() {
        vm.updateSettings { it.copy(name = name.trim(), onboarded = true) }
        onDone()
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { finish() }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LIFEDESK", style = MaterialTheme.typography.labelSmall.copy(brush = Neon.Primary), modifier = Modifier.weight(1f))
            if (pager.currentPage < slides.size) TextButton(onClick = { scope.launch { pager.animateScrollToPage(slides.size) } }) {
                Text("Skip", color = Neon.Muted)
            }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            if (page < slides.size) {
                val s = slides[page]
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    RingGauge(s.fill, size = 190.dp, stroke = 12.dp, colors = listOf(s.color, Neon.Violet, s.color)) {
                        Box(
                            Modifier.size(96.dp).clip(RoundedCornerShape(30.dp))
                                .background(Brush.linearGradient(listOf(s.color.copy(alpha = 0.3f), s.color.copy(alpha = 0.05f)))),
                            contentAlignment = Alignment.Center,
                        ) { Icon(s.icon, null, tint = s.color, modifier = Modifier.size(48.dp)) }
                    }
                    Spacer(Modifier.height(36.dp))
                    Text(s.kicker, style = MaterialTheme.typography.labelSmall, color = s.color)
                    Text(s.title, style = MaterialTheme.typography.displaySmall, color = Neon.Text, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp))
                    Text(s.body, style = MaterialTheme.typography.bodyLarge, color = Neon.Muted, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp))
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    Text("LET'S BEGIN", style = MaterialTheme.typography.labelSmall, color = Neon.Cyan)
                    Text("Everything in your life that has a date, payment or deadline.",
                        style = MaterialTheme.typography.headlineMedium.copy(brush = Brush.linearGradient(listOf(Neon.Text, Neon.Cyan))),
                        modifier = Modifier.padding(top = 6.dp))
                    GlassCard(Modifier.fillMaxWidth().padding(top = 24.dp), glow = Neon.Violet) {
                        OutlinedTextField(
                            name, { name = it }, label = { Text("What should we call you?") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth(), colors = neonFieldColors(), shape = RoundedCornerShape(14.dp),
                        )
                    }
                }
            }
        }
        // Page indicator
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
            repeat(slides.size + 1) { i ->
                val w by animateDpAsState(if (i == pager.currentPage) 26.dp else 8.dp, label = "dot")
                Box(
                    Modifier.padding(horizontal = 3.dp).height(8.dp).width(w).clip(RoundedCornerShape(4.dp))
                        .background(if (i == pager.currentPage) Neon.Primary else Brush.linearGradient(listOf(Neon.Stroke, Neon.Stroke))),
                )
            }
        }
        if (pager.currentPage < slides.size) {
            GradientButton("Next", { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, Modifier.fillMaxWidth())
        } else {
            GradientButton("Get started", {
                if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else finish()
            }, Modifier.fillMaxWidth())
        }
    }
}
