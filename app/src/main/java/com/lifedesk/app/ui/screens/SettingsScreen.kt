package com.lifedesk.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.lifedesk.app.notify.Reminders
import com.lifedesk.app.ui.AppViewModel
import com.lifedesk.app.ui.components.Dropdown
import com.lifedesk.app.ui.components.GlassCard
import com.lifedesk.app.ui.components.GradientButton
import com.lifedesk.app.ui.components.OptionRow
import com.lifedesk.app.ui.components.SectionHeader
import com.lifedesk.app.ui.components.Tag
import com.lifedesk.app.ui.components.neonFieldColors
import com.lifedesk.app.ui.theme.Mono
import com.lifedesk.app.ui.theme.Neon
import java.time.LocalDate

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavHostController) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val account by vm.account.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var name by remember { mutableStateOf(settings.name) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmDeleteAccount by remember { mutableStateOf(false) }
    var notificationsOn by remember { mutableStateOf(Reminders.canNotify(context)) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsOn = granted
        if (granted) vm.sendTestReminder()
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let { vm.exportTo(it) } }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.importFrom(it) } }
    val switchColors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF06101E), checkedTrackColor = Neon.Cyan, uncheckedTrackColor = Neon.Surface2)

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("SYSTEM", style = MaterialTheme.typography.labelSmall, color = Neon.Cyan)
        Text("Control panel", style = MaterialTheme.typography.headlineMedium, color = Neon.Text)

        // ------------------------------------------------ profile / account
        val a = account
        GlassCard(Modifier.fillMaxWidth().padding(top = 16.dp), glow = if (a != null) Neon.Green else Neon.Violet, padding = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val initials = (a?.name ?: settings.name).trim().split(" ").filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1).uppercase() }.ifEmpty { "LD" }
                Box(
                    Modifier.size(58.dp).clip(CircleShape).background(Neon.Primary),
                    contentAlignment = Alignment.Center,
                ) { Text(initials, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF06101E)) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(a?.name ?: settings.name.ifBlank { "Guest" }, style = MaterialTheme.typography.titleLarge, color = Neon.Text)
                    Text(a?.email ?: "Offline mode · not signed in", style = MaterialTheme.typography.bodySmall, color = Neon.Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                        if (a != null) {
                            Tag(if (a.provider == "password") "Email" else "Google", Neon.Cyan)
                            Tag(if (a.emailVerified) "Verified" else "Unverified", if (a.emailVerified) Neon.Green else Neon.Amber)
                        } else Tag("Local only", Neon.Faint)
                        Tag("${items.size} items", Neon.Violet)
                    }
                }
            }
            if (a == null) {
                GradientButton("Sign in or create account", { nav.navigate("auth") }, Modifier.fillMaxWidth().padding(top = 16.dp))
                Text("Back up your reminders and restore them on any phone. Sign in with Gmail or any email.",
                    style = MaterialTheme.typography.bodySmall, color = Neon.Muted, modifier = Modifier.padding(top = 8.dp))
            }
        }

        if (a != null && a.provider == com.lifedesk.app.ui.DEVICE_GOOGLE) {
            SectionHeader("Account", color = Neon.Green)
            GlassCard(Modifier.fillMaxWidth(), glow = Neon.Green, padding = 10.dp) {
                OptionRow(Icons.Outlined.Verified, "Google account on this phone", a.email, Neon.Green)
                OptionRow(Icons.Outlined.CloudSync, "Cloud backup", "Available once LifeDesk cloud (Firebase) is connected", Neon.Faint)
                OptionRow(Icons.AutoMirrored.Outlined.Logout, "Sign out", "Your data stays on this phone", Neon.Muted, onClick = { vm.signOut() })
            }
        }

        if (a != null && a.provider != com.lifedesk.app.ui.DEVICE_GOOGLE) {
            SectionHeader("Cloud", color = Neon.Green)
            GlassCard(Modifier.fillMaxWidth(), glow = Neon.Green, padding = 10.dp) {
                if (!a.emailVerified) {
                    OptionRow(Icons.Outlined.MarkEmailUnread, "Verify your email", "Tap the link we sent, then refresh", Neon.Amber, onClick = { vm.resendVerification() }) {
                        TextButton(onClick = { vm.refreshAccount() }) { Text("Refresh", color = Neon.Cyan) }
                    }
                }
                OptionRow(
                    Icons.Outlined.CloudSync, "Back up now",
                    if (settings.lastBackupAt > 0) "Last: " + java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
                        .format(java.util.Date(settings.lastBackupAt)) else "No backup yet",
                    Neon.Green, onClick = { if (!busy) vm.backupNow() },
                ) { if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Neon.Green) }
                OptionRow(Icons.Outlined.CloudDownload, "Restore from cloud", "Replace this phone's list with your backup", Neon.Cyan, onClick = { if (!busy) vm.restoreFromCloud() })
                OptionRow(Icons.Outlined.Verified, "Automatic backup", "After every change and daily", Neon.Violet) {
                    Switch(settings.autoBackup, { v -> vm.updateSettings { it.copy(autoBackup = v) } }, colors = switchColors)
                }
                OptionRow(Icons.AutoMirrored.Outlined.Logout, "Sign out", "Your data stays on this phone", Neon.Muted, onClick = { vm.signOut() })
                OptionRow(Icons.Outlined.PersonRemove, "Delete account", "Removes the account and cloud backup", Neon.Red, onClick = { confirmDeleteAccount = true })
            }
        }

        // ------------------------------------------------ security & reminders
        SectionHeader("Security", color = Neon.Violet)
        GlassCard(Modifier.fillMaxWidth(), glow = Neon.Violet, padding = 10.dp) {
            OptionRow(Icons.Outlined.Fingerprint, "App lock", "Fingerprint, face or phone PIN", Neon.Violet) {
                Switch(settings.appLock, { v -> vm.updateSettings { it.copy(appLock = v) } }, colors = switchColors)
            }
        }

        SectionHeader("Reminders", color = Neon.Amber)
        GlassCard(Modifier.fillMaxWidth(), glow = Neon.Amber, padding = 14.dp) {
            Dropdown("Daily reminder time", settings.reminderHour, (6..22).toList(), { h -> "%02d:00".format(h) },
                { h -> vm.updateSettings { s -> s.copy(reminderHour = h) } }, Modifier.fillMaxWidth())
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("30d", "14d", "7d", "3d", "1d", "0d").forEach { Tag(it, Neon.Amber) }
            }
            Text("Nudges before each date — months ahead for passports, IDs and visas.",
                style = MaterialTheme.typography.bodySmall, color = Neon.Muted, modifier = Modifier.padding(top = 8.dp))
            if (!notificationsOn && Build.VERSION.SDK_INT >= 33) {
                GradientButton("Turn on notifications", { permission.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    Modifier.fillMaxWidth().padding(top = 12.dp), brush = Neon.Warm, icon = Icons.Outlined.NotificationsActive)
            } else {
                OptionRow(Icons.Outlined.NotificationsActive, "Send a test reminder", "Check notifications work", Neon.Amber, onClick = { vm.sendTestReminder() })
                OptionRow(Icons.Outlined.NotificationsActive, "Pop-up style", "Make sure “Pop on screen” is on for reminders", Neon.Cyan, onClick = {
                    context.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, Reminders.CHANNEL)
                    )
                })
            }
        }

        SectionHeader("Profile", color = Neon.Cyan)
        GlassCard(Modifier.fillMaxWidth(), padding = 14.dp) {
            OutlinedTextField(
                name, { name = it; vm.updateSettings { s -> s.copy(name = it) } }, label = { Text("Your first name") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, colors = neonFieldColors(), shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(10.dp))
            Dropdown("Default currency", settings.currency, currencies, { it }, { c -> vm.updateSettings { s -> s.copy(currency = c) } }, Modifier.fillMaxWidth())
        }

        // ------------------------------------------------ data
        SectionHeader("Data", color = Neon.Blue)
        GlassCard(Modifier.fillMaxWidth(), glow = Neon.Blue, padding = 10.dp) {
            OptionRow(Icons.Outlined.FileDownload, "Export backup file", "JSON · everything except photos", Neon.Blue,
                onClick = { exportLauncher.launch("lifedesk-backup-${LocalDate.now()}.json") })
            OptionRow(Icons.Outlined.FileUpload, "Import backup file", "Restore from a JSON export", Neon.Cyan,
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) })
            OptionRow(Icons.Outlined.DeleteForever, "Delete all data", "Wipe this phone's LifeDesk", Neon.Red, onClick = { confirmClear = true })
        }

        // ------------------------------------------------ hands-free voice
        SectionHeader("Hands-free voice", color = Neon.Violet)
        val voiceContext = androidx.compose.ui.platform.LocalContext.current
        GlassCard(Modifier.fillMaxWidth(), glow = Neon.Violet, padding = 10.dp) {
            OptionRow(Icons.Outlined.Mic, "Try a voice command", "“Remind me to pay DEWA 450 on Friday”", Neon.Violet, onClick = {
                voiceContext.startActivity(android.content.Intent(voiceContext, com.lifedesk.app.voice.VoiceCommandActivity::class.java))
            })
            OptionRow(Icons.Outlined.Mic, "“Hey Google, open LifeDesk Voice”", "Works even when LifeDesk is closed", Neon.Cyan)
            OptionRow(Icons.Outlined.Mic, "Quick Settings tile", "Swipe down → edit tiles → add “LifeDesk voice”", Neon.Cyan)
            OptionRow(Icons.Outlined.Mic, "More ways in", "Widget mic · long-press app icon → Voice command · headset button", Neon.Muted)
        }

        SectionHeader("Power tips", color = Neon.Pink)
        GlassCard(Modifier.fillMaxWidth(), glow = Neon.Pink, padding = 10.dp) {
            OptionRow(Icons.Outlined.DataObject, "Share to LifeDesk", "From Gmail, WhatsApp, Files: any email, PDF or photo", Neon.Pink)
            OptionRow(Icons.Outlined.Widgets, "Home-screen widget", "Long-press home → Widgets → LifeDesk", Neon.Cyan)
        }

        val adPrivacyRequired by com.lifedesk.app.ads.Ads.privacyOptionsRequired.collectAsState()
        if (adPrivacyRequired) {
            SectionHeader("Ads", color = Neon.Faint)
            GlassCard(Modifier.fillMaxWidth(), padding = 10.dp) {
                OptionRow(Icons.Outlined.Info, "Ad privacy choices", "Change your consent for personalised ads", Neon.Muted, onClick = {
                    (context as? android.app.Activity)?.let { com.lifedesk.app.ads.Ads.showPrivacyOptions(it) }
                })
            }
        }

        SectionHeader("About", color = Neon.Faint)
        OptionRow(Icons.Outlined.Info, "LifeDesk 4.2", "Everything in your life that has a date, payment or deadline", Neon.Muted)
        Text(
            "Documents are read on-device. Photos never leave your phone; with an account only your reminder list is backed up, privately.",
            style = MaterialTheme.typography.bodySmall, color = Neon.Faint, fontFamily = Mono, fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(110.dp))
    }

    if (confirmDeleteAccount) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAccount = false },
            containerColor = Neon.Surface2,
            title = { Text("Delete your account?") },
            text = { Text("Your account and its cloud backup are deleted permanently. Reminders on this phone are kept.") },
            confirmButton = { TextButton(onClick = { confirmDeleteAccount = false; vm.deleteAccount() }) { Text("Delete", color = Neon.Red) } },
            dismissButton = { TextButton(onClick = { confirmDeleteAccount = false }) { Text("Cancel") } },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = Neon.Surface2,
            title = { Text("Delete everything?") },
            text = { Text("All items, reminders, price history and document photos will be removed from this phone. This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; vm.clearAll() }) { Text("Delete all", color = Neon.Red) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}
