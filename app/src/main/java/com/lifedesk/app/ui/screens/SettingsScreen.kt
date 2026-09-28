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
fun SettingsScreen(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var name by remember { mutableStateOf(settings.name) }
    var confirmClear by remember { mutableStateOf(false) }
    var notificationsOn by remember { mutableStateOf(Reminders.canNotify(context)) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsOn = granted
        if (granted) vm.sendTestReminder()
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::exportTo) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::importFrom) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

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
            "Everything stays on this phone. Document text is read on-device — nothing is uploaded.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = { exportLauncher.launch("lifedesk-backup-${LocalDate.now()}.json") }, Modifier.fillMaxWidth()) { Text("Back up to a file") }
        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }, Modifier.fillMaxWidth()) { Text("Restore from backup") }
        OutlinedButton(onClick = vm::loadSampleData, Modifier.fillMaxWidth()) { Text("Load sample data") }
        TextButton(onClick = { confirmClear = true }, Modifier.fillMaxWidth()) { Text("Delete all data", color = MaterialTheme.colorScheme.error) }

        SectionTitle("About")
        Text("LifeDesk 1.0 — everything in your life that has a date, payment or deadline.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
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
