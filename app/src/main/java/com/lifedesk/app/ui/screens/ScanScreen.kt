package com.lifedesk.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lifedesk.app.ui.AppViewModel
import com.lifedesk.app.ui.ScanState
import com.lifedesk.app.ui.components.GlassCard
import com.lifedesk.app.ui.components.SectionHeader
import com.lifedesk.app.ui.components.viewfinder
import com.lifedesk.app.ui.theme.Mono
import com.lifedesk.app.ui.theme.Neon
import java.io.File

@Composable
fun ScanScreen(vm: AppViewModel, nav: NavHostController, replaceItemId: Long?) {
    val context = LocalContext.current
    val scan by vm.scan.collectAsStateWithLifecycle()
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    val goToReview: () -> Unit = { nav.navigate("edit") }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pendingPhoto
        pendingPhoto = null
        if (path == null) return@rememberLauncherForActivityResult
        val file = File(path)
        if (ok && file.length() > 0) vm.processImage(file, replaceItemId, goToReview) else file.delete()
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let { vm.processPickedImage(it, replaceItemId, goToReview) }
    }

    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { vm.processPdf(it, replaceItemId, goToReview) }
    }
    var pasteOpen by rememberSaveable { mutableStateOf(false) }

    fun launchCamera() {
        val file = vm.newPhotoFile()
        pendingPhoto = file.path
        takePicture.launch(FileProvider.getUriForFile(context, "${context.packageName}.files", file))
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("CAPTURE", style = MaterialTheme.typography.labelSmall, color = Neon.Cyan)
            Text(if (replaceItemId != null) "Upload new document" else "Scan & extract", style = MaterialTheme.typography.headlineMedium, color = Neon.Text)
            Text(
                "On-device AI reads the document offline: type, provider, amounts, every date and reference.",
                color = Neon.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            // Viewfinder hero — tap to open the camera.
            ScannerHero { launchCamera() }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SourceTile(Icons.Outlined.PhotoLibrary, "Gallery", "Photos & screenshots", Neon.Violet, Modifier.weight(1f)) {
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                SourceTile(Icons.Outlined.PictureAsPdf, "PDF", "E-invoices, statements", Neon.Pink, Modifier.weight(1f)) {
                    pickPdf.launch(arrayOf("application/pdf"))
                }
            }
            if (replaceItemId == null) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SourceTile(Icons.Outlined.MarkEmailRead, "Email / SMS", "Paste message text", Neon.Amber, Modifier.weight(1f)) { pasteOpen = true }
                    SourceTile(Icons.Outlined.EditNote, "Manual", "Type it in", Neon.Green, Modifier.weight(1f)) {
                        vm.startManual(); goToReview()
                    }
                }
            }

            SectionHeader("Pro tips", color = Neon.Violet)
            GlassCard(Modifier.fillMaxWidth(), glow = Neon.Stroke, padding = 14.dp) {
                listOf(
                    "Lay the document flat in good light and fill the frame.",
                    "The page with the expiry/due date and amount matters most.",
                    "From Gmail, WhatsApp or Files: Share → LifeDesk on any email, PDF or photo.",
                    "Tenancy contracts with cheque lists become one reminder per cheque.",
                ).forEachIndexed { i, tip ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text("0${i + 1}", fontFamily = Mono, color = Neon.Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(tip, color = Neon.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(90.dp))
        }

        (scan as? ScanState.Working)?.let { w ->
            Box(Modifier.fillMaxSize().background(Neon.Bg.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
                GlassCard(glow = Neon.Cyan, padding = 28.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ScanningAnimation()
                        Spacer(Modifier.height(16.dp))
                        Text(w.message, textAlign = TextAlign.Center, color = Neon.Text, style = MaterialTheme.typography.titleMedium)
                        Text("on-device · offline", style = MaterialTheme.typography.labelSmall, color = Neon.Muted, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
    if (pasteOpen) PasteDialog(onDismiss = { pasteOpen = false }) { t -> pasteOpen = false; vm.processText(t, goToReview) }
}

@Composable
private fun PasteDialog(onDismiss: () -> Unit, onRead: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paste an email or message") },
        text = {
            Column {
                Text("LifeDesk will pull out the dates, amounts and references.", style = MaterialTheme.typography.bodySmall)
                androidx.compose.material3.OutlinedTextField(
                    text, { text = it }, minLines = 5, maxLines = 10,
                    placeholder = { Text("e.g. “Your DEWA bill of AED 812.40 is due on 25/10/2026…”") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                androidx.compose.material3.TextButton(onClick = { clipboard.getText()?.text?.let { text = it } }) { Text("Paste from clipboard") }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onRead(text) }, enabled = text.isNotBlank()) { Text("Read it") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Big viewfinder with an animated scan line; tapping it opens the camera. */
@Composable
private fun ScannerHero(onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "scan")
    val line by t.animateFloat(0.08f, 0.92f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse), label = "line")
    val pulse by t.animateFloat(0.6f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "pulse")
    Box(
        Modifier.fillMaxWidth().aspectRatio(1.35f).clip(RoundedCornerShape(26.dp))
            .background(Brush.radialGradient(listOf(Neon.Cyan.copy(alpha = 0.10f), Neon.Surface.copy(alpha = 0.6f))))
            .clickable(onClick = onClick)
            .padding(18.dp)
            .viewfinder(Neon.Cyan.copy(alpha = pulse))
            .drawBehind {
                val y = size.height * line
                drawRect(
                    Brush.verticalGradient(listOf(Color.Transparent, Neon.Cyan.copy(alpha = 0.22f)), startY = y - 60f, endY = y),
                    topLeft = Offset(0f, (y - 60f).coerceAtLeast(0f)), size = Size(size.width, 60f.coerceAtMost(y)),
                )
                drawLine(Neon.Cyan, Offset(12f, y), Offset(size.width - 12f, y), strokeWidth = 2.5f)
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(Neon.Primary),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.CameraAlt, null, tint = Color(0xFF06101E), modifier = Modifier.size(34.dp)) }
            Text("Tap to scan a document", style = MaterialTheme.typography.titleMedium, color = Neon.Text, modifier = Modifier.padding(top = 12.dp))
            Text("BILL · POLICY · ID · WARRANTY · CONTRACT", style = MaterialTheme.typography.labelSmall, color = Neon.Muted, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun SourceTile(icon: ImageVector, title: String, subtitle: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier, glow = color, onClick = onClick, padding = 14.dp) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.15f))
                .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
        Text(title, style = MaterialTheme.typography.titleMedium, color = Neon.Text, modifier = Modifier.padding(top = 10.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Neon.Muted)
    }
}

/** Rotating dual ring shown while OCR runs. */
@Composable
private fun ScanningAnimation() {
    val t = rememberInfiniteTransition(label = "spin")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "angle")
    Canvas(Modifier.size(84.dp)) {
        val s = 6.dp.toPx()
        drawArc(Neon.Stroke, 0f, 360f, false, Offset(s / 2, s / 2), Size(size.width - s, size.height - s), style = Stroke(s))
        drawArc(
            Brush.sweepGradient(listOf(Color.Transparent, Neon.Cyan, Neon.Violet)), angle, 260f, false,
            Offset(s / 2, s / 2), Size(size.width - s, size.height - s), style = Stroke(s, cap = StrokeCap.Round),
        )
        val inner = size.width * 0.22f
        drawArc(Neon.Violet, -angle * 1.5f, 120f, false, Offset(inner, inner), Size(size.width - inner * 2, size.height - inner * 2),
            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
    }
}
