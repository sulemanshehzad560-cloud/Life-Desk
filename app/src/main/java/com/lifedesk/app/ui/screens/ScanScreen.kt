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
import com.lifedesk.app.ui.AppViewModel
import com.lifedesk.app.ui.ScanState
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
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(if (replaceItemId != null) "Upload new document" else "Add to LifeDesk", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Photograph any bill, policy, ID, warranty or contract. We'll find what it is, who it's with, the amount and the dates that matter — on your phone, offline.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            )
            BigAction(Icons.Outlined.CameraAlt, "Take a photo", "Best for paper documents and cards", primary = true) { launchCamera() }
            BigAction(Icons.Outlined.PhotoLibrary, "Choose from gallery", "Screenshots of emails, e-invoices, WhatsApp images") {
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            BigAction(Icons.Outlined.PictureAsPdf, "Import a PDF", "E-invoices, bank statements, policies, contracts") {
                pickPdf.launch(arrayOf("application/pdf"))
            }
            if (replaceItemId == null) {
                BigAction(Icons.Outlined.MarkEmailRead, "Paste an email or SMS", "Copy the text of a bill or booking email and paste it") {
                    pasteOpen = true
                }
                BigAction(Icons.Outlined.EditNote, "Enter manually", "Add a reminder without a document") {
                    vm.startManual(); goToReview()
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Tips", style = MaterialTheme.typography.titleMedium)
            listOf(
                "Lay the document flat in good light and fill the frame.",
                "The page with the expiry/due date and amount matters most.",
                "In Gmail, WhatsApp or Files: tap Share → LifeDesk on any email, PDF or photo.",
            ).forEach { Text("•  $it", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
        }

        (scan as? ScanState.Working)?.let { w ->
            Box(Modifier.fillMaxSize().padding(0.dp), contentAlignment = Alignment.Center) {
                Card(shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(8.dp)) {
                    Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(w.message, textAlign = TextAlign.Center)
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

@Composable
private fun BigAction(icon: ImageVector, title: String, subtitle: String, primary: Boolean = false, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
            Icon(icon, null, Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
