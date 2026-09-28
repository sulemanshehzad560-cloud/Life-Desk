package com.lifedesk.app.ui.screens

import android.app.Activity
import android.util.Patterns
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifedesk.app.ui.AppViewModel

/** Password strength 0..4 for the meter under the password field. */
fun passwordStrength(p: String): Int {
    var s = 0
    if (p.length >= 8) s++
    if (p.length >= 12) s++
    if (p.any(Char::isDigit) && p.any(Char::isLetter)) s++
    if (p.any { !it.isLetterOrDigit() } || (p.any(Char::isUpperCase) && p.any(Char::isLowerCase))) s++
    return s
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(vm: AppViewModel, onDone: () -> Unit, skipLabel: String? = null) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    val account by vm.account.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var createMode by rememberSaveable { mutableStateOf(skipLabel != null) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var forgotOpen by rememberSaveable { mutableStateOf(false) }

    // Signed in (by any method) → continue.
    LaunchedEffect(account?.uid) { if (account != null) onDone() }

    val emailOk = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val strength = passwordStrength(password)
    val canSubmit = !busy && emailOk && password.length >= 6 && (!createMode || name.isNotBlank())

    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        com.lifedesk.app.ui.components.RingGauge(0.78f, size = 72.dp, stroke = 5.dp) {
            Icon(Icons.Outlined.Lock, null, tint = com.lifedesk.app.ui.theme.Neon.Cyan)
        }
        Text("SECURE ACCOUNT", style = MaterialTheme.typography.labelSmall, color = com.lifedesk.app.ui.theme.Neon.Cyan)
        AnimatedContent(createMode, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "title") { create ->
            Column {
                Text(if (create) "Create your LifeDesk account" else "Welcome back", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (create) "Back up your reminders and restore them on any phone." else "Sign in to restore your reminders.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!vm.accountsAvailable) {
            Text(
                "Accounts aren't switched on in this build yet (Firebase isn't configured). You can use LifeDesk fully offline.",
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (vm.googleSignInAvailable) {
            OutlinedButton(
                onClick = { (context as? Activity)?.let { vm.signInWithGoogle(it) {} } },
                enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
            ) {
                Text("G", fontWeight = FontWeight.Black, color = Color(0xFF4285F4), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(12.dp))
                Text("Continue with Google")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f))
                Text("  or with email  ", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(Modifier.weight(1f))
            }
        }

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = !createMode, onClick = { createMode = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Sign in") }
            SegmentedButton(selected = createMode, onClick = { createMode = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Create account") }
        }

        if (createMode) {
            OutlinedTextField(name, { name = it }, label = { Text("Your name") }, singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Person, null) }, modifier = Modifier.fillMaxWidth())
        }
        OutlinedTextField(
            email, { email = it }, label = { Text("Email (e.g. you@gmail.com)") }, singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Email, null) },
            isError = email.isNotBlank() && !emailOk,
            supportingText = { if (email.isNotBlank() && !emailOk) Text("Enter a valid email address") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            password, { password = it }, label = { Text("Password") }, singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (showPassword) "Hide" else "Show")
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(),
        )
        if (createMode && password.isNotEmpty()) {
            val (label, color) = when (strength) {
                0, 1 -> "Weak" to MaterialTheme.colorScheme.error
                2 -> "Okay" to Color(0xFFE08600)
                3 -> "Good" to Color(0xFF2E7D32)
                else -> "Strong" to Color(0xFF2E7D32)
            }
            LinearProgressIndicator(progress = { (strength + 1) / 5f }, color = color, modifier = Modifier.fillMaxWidth())
            Text("Password strength: $label", color = color, style = MaterialTheme.typography.bodySmall)
        }

        com.lifedesk.app.ui.components.GradientButton(
            if (busy) "Please wait…" else if (createMode) "Create account" else "Sign in",
            { if (createMode) vm.signUp(name, email, password) {} else vm.signIn(email, password) {} },
            Modifier.fillMaxWidth(), enabled = canSubmit,
        )
        if (!createMode) {
            TextButton(onClick = { forgotOpen = true }, modifier = Modifier.align(Alignment.End)) { Text("Forgot password?") }
        } else {
            Text(
                "We'll email you a link to verify your address. Your reminders stay on your phone; the account adds a private cloud backup.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        skipLabel?.let {
            TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(it) }
        }
    }

    if (forgotOpen) {
        var resetEmail by rememberSaveable { mutableStateOf(email) }
        AlertDialog(
            onDismissRequest = { forgotOpen = false },
            title = { Text("Reset your password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your account email. We'll send a link to choose a new password.")
                    OutlinedTextField(resetEmail, { resetEmail = it }, singleLine = true, label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { vm.sendPasswordReset(resetEmail); forgotOpen = false },
                    enabled = Patterns.EMAIL_ADDRESS.matcher(resetEmail.trim()).matches(),
                ) { Text("Send link") }
            },
            dismissButton = { TextButton(onClick = { forgotOpen = false }) { Text("Cancel") } },
        )
    }
}

/** Shown over everything while the app is locked. */
@Composable
fun LockScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    fun prompt() {
        if (activity == null) { vm.unlock(); return }
        val authenticators = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
            androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (androidx.biometric.BiometricManager.from(context).canAuthenticate(authenticators) != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
            // No screen lock set on the phone: nothing to verify against.
            vm.unlock(); return
        }
        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock LifeDesk")
            .setSubtitle("Use your fingerprint, face or phone PIN")
            .setAllowedAuthenticators(authenticators)
            .build()
        androidx.biometric.BiometricPrompt(activity, androidx.core.content.ContextCompat.getMainExecutor(context),
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) = vm.unlock()
            }).authenticate(promptInfo)
    }
    LaunchedEffect(Unit) { prompt() }
    androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("🔒", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(12.dp))
            Text("LifeDesk is locked", style = MaterialTheme.typography.titleLarge)
            Text("Your documents and reminders are protected.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Button(onClick = { prompt() }) { Text("Unlock") }
        }
    }
}
