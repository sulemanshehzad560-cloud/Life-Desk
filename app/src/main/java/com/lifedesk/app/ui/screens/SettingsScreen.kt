package com.lifedesk.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifedesk.app.notify.Reminders
import com.lifedesk.app.ui.AppViewModel
import com.lifedesk.app.ui.components.Dropdown
import com.lifedesk.app.ui.components.SectionTitle
import java.time.LocalDate

@Composable
fun SettingsScreen(vm: AppViewModel, nav: androidx.navigation.NavHostController) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var name by remember { mutableStateOf(settings.name) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmDeleteAccount by remember { mutableStateOf(false) }
    val account by vm.account.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var notificationsOn by remember { mutableStateOf(Reminders.canNotify(context)) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsOn = granted
        if (granted) vm.sendTestReminder()
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::exportTo) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::importFrom) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        SectionTitle("Account")
        androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val a = account
                if (a == null) {
                    Text("Not signed in", style = MaterialTheme.typography.titleMedium)
                    Text("Create a free account with your email or Google to back up your reminders and restore them on a new phone.",
                        style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { nav.navigate("auth") }, Modifier.fillMaxWidth()) { Text("Sign in or create account") }
                } else {
                    Text(a.name ?: a.email ?: "Signed in", style = MaterialTheme.typography.titleMedium)
                    Text("${a.email ?: ""} · ${if (a.provider == "google") "Google account" else "Email account"}", style = MaterialTheme.typography.bodySmall)
                    if (!a.emailVerified) {
                        Text("⚠️ Email not verified yet — tap the link we sent you.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = vm::resendVerification, enabled = !busy) { Text("Resend email") }
                            OutlinedButton(onClick = { vm.refreshAccount() }) { Text("I've verified") }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = vm::backupNow, enabled = !busy) { Text("Back up now") }
                        OutlinedButton(onClick = vm::restoreFromCloud, enabled = !busy) { Text("Restore") }
                    }
                    Text(
                        if (settings.lastBackupAt > 0) "Last backup: " + java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
                            .format(java.util.Date(settings.lastBackupAt)) else "No backup yet",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Automatic backup", Modifier.weight(1f))
                        Switch(checked = settings.autoBackup, onCheckedChange = { v -> vm.updateSettings { it.copy(autoBackup = v) } })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = vm::signOut) { Text("Sign out") }
                        TextButton(onClick = { confirmDeleteAccount = true }) { Text("Delete account", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }

        SectionTitle("Privacy & security")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("App lock", style = MaterialTheme.typography.titleMedium)
                Text("Ask for fingerprint, face or phone PIN when opening LifeDesk.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = settings.appLock, onCheckedChange = { v -> vm.updateSettings { it.copy(appLock = v) } })
        }

        SectionTitle("You")
        OutlinedTextField(name, { name = it; vm.updateSettings { s -> s.copy(name = it) } }, label = { Text("Your first name") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        Dropdown("Default currency", settings.currency, currencies, { it }, { c -> vm.updateSettings { s -> s.copy(currency = c) } }, Modifier.fillMaxWidth())

        SectionTitle("Reminders")
        Dropdown("Daily reminder time", settings.reminderHour, (6..22).toList(), { h -> "%02d:00".format(h) },
            { h -> vm.updateSettings { s -> s.copy(reminderHour = h) } }, Modifier.fillMaxWidth())
        Text(
            "LifeDesk reminds you 30, 14, 7, 3 and 1 day before a date (and months ahead for passports, IDs and visas).",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!notificationsOn && Build.VERSION.SDK_INT >= 33) {
            Button(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }, Modifier.fillMaxWidth()) { Text("Turn on notifications") }
        } else {
            OutlinedButton(onClick = vm::sendTestReminder, Modifier.fillMaxWidth()) { Text("Send a test reminder") }
        }

        SectionTitle("Your data")
        Text(
            "Documents are read on-device. Photos never leave your phone; with an account, only your reminder list is backed up (privately, to you).",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = { exportLauncher.launch("lifedesk-backup-${LocalDate.now()}.json") }, Modifier.fillMaxWidth()) { Text("Back up to a file") }
        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }, Modifier.fillMaxWidth()) { Text("Restore from backup") }
        OutlinedButton(onClick = vm::loadSampleData, Modifier.fillMaxWidth()) { Text("Load sample data") }
        TextButton(onClick = { confirmClear = true }, Modifier.fillMaxWidth()) { Text("Delete all data", color = MaterialTheme.colorScheme.error) }

        SectionTitle("Tips")
        listOf(
            "📥 Share any bill email, PDF or photo from Gmail/WhatsApp to LifeDesk — it pulls out every date, amount and reference.",
            "🎙️ Use quick add on Home: “DEWA bill 450 dirhams due next Friday”.",
            "👉 Swipe an item right to mark it paid, left to snooze.",
            "📱 Long-press your home screen → Widgets → LifeDesk to see what's coming up.",
        ).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }

        SectionTitle("About")
        Text("LifeDesk 2.0 — everything in your life that has a date, payment or deadline.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
    }

    if (confirmDeleteAccount) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAccount = false },
            title = { Text("Delete your account?") },
            text = { Text("Your account and its cloud backup are deleted permanently. Reminders on this phone are kept.") },
            confirmButton = { TextButton(onClick = { confirmDeleteAccount = false; vm.deleteAccount() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDeleteAccount = false }) { Text("Cancel") } },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Delete everything?") },
            text = { Text("All items, reminders, price history and document photos will be removed from this phone. This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; vm.clearAll() }) { Text("Delete all", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}
