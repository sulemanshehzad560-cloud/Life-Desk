package com.lifedesk.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifedesk.app.ui.AppViewModel

@Composable
fun WelcomeScreen(vm: AppViewModel, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    fun finish() {
        vm.updateSettings { it.copy(name = name.trim(), onboarded = true) }
        onDone()
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { finish() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Spacer(Modifier.height(32.dp))
        Text("🗂️", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text("LifeDesk", style = MaterialTheme.typography.displaySmall)
        Text("Never forget a payment, renewal, expiry or warranty again.", style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(24.dp))
        listOf(
            "📸" to "Snap any bill, policy, ID or warranty — LifeDesk reads it for you.",
            "⏰" to "Get reminded at the right time: days ahead for bills, months ahead for passports.",
            "💸" to "See where your money goes and which subscriptions you don't use.",
            "🔒" to "Private by design: everything stays on your phone.",
        ).forEach { (emoji, text) ->
            Row(Modifier.padding(vertical = 8.dp)) {
                Text(emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(12.dp))
                Text(text, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(name, { name = it }, label = { Text("What should we call you?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else finish() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Get started") }
    }
}
