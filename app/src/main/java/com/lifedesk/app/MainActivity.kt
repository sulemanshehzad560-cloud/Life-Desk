package com.lifedesk.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.lifedesk.app.notify.Reminders
import com.lifedesk.app.ui.AppViewModel
import com.lifedesk.app.ui.LifeDeskRoot
import com.lifedesk.app.ui.theme.LifeDeskTheme

/** FragmentActivity (still a ComponentActivity) because the biometric app lock needs it. */
class MainActivity : FragmentActivity() {
    private val vm: AppViewModel by viewModels()
    private var backgroundedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            LifeDeskTheme { LifeDeskRoot(vm) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) backgroundedAt = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        // Re-lock after 30 seconds in the background (not when briefly opening the camera or file picker).
        if (backgroundedAt > 0 && SystemClock.elapsedRealtime() - backgroundedAt > 30_000) vm.lock()
        vm.refreshAccount()
    }

    @Suppress("DEPRECATION")
    private fun streamUri(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else intent.getParcelableExtra(Intent.EXTRA_STREAM)

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val itemId = intent.getLongExtra(Reminders.EXTRA_ITEM_ID, -1L)
        if (itemId > 0) vm.openItem(itemId)
        val type = intent.type.orEmpty()
        when (intent.action) {
            Intent.ACTION_SEND -> when {
                type.startsWith("image/") -> streamUri(intent)?.let(vm::importSharedImage)
                type == "application/pdf" -> streamUri(intent)?.let(vm::importSharedPdf)
                type.startsWith("text/") -> {
                    val text = listOfNotNull(intent.getStringExtra(Intent.EXTRA_SUBJECT), intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString())
                        .joinToString("\n")
                    if (text.isNotBlank()) vm.importSharedText(text) else streamUri(intent)?.let(vm::importSharedPdf)
                }
            }
            Intent.ACTION_VIEW -> if (type == "application/pdf") intent.data?.let(vm::importSharedPdf)
        }
    }
}
