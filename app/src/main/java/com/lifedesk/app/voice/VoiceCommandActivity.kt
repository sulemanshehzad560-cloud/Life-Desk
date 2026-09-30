package com.lifedesk.app.voice

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.lifedesk.app.MainActivity
import com.lifedesk.app.notify.Reminders
import com.lifedesk.app.ui.Speaker
import com.lifedesk.app.ui.components.GlassCard
import com.lifedesk.app.ui.components.RadarSweep
import com.lifedesk.app.ui.components.TypewriterText
import com.lifedesk.app.ui.theme.LifeDeskTheme
import com.lifedesk.app.ui.theme.Neon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Hands-free entry point: "Hey Google, open LifeDesk Voice", the Quick Settings tile, the widget mic,
 * the "Voice command" app shortcut and the headset voice button all land here. It listens once,
 * runs the command, answers out loud and closes, without opening the full app.
 */
class VoiceCommandActivity : ComponentActivity() {

    private var status by mutableStateOf("Listening…")
    private var heard by mutableStateOf("")
    private var answer by mutableStateOf("")

    private val recognizer = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (result.resultCode != Activity.RESULT_OK || text.isNullOrBlank()) finish() else execute(text)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LifeDeskTheme {
                Box(
                    Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { finish() },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    GlassCard(
                        // Taps on the card itself must not fall through to the dismiss-on-outside-tap backdrop.
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)
                            .clickable(remember { MutableInteractionSource() }, null) {},
                        glow = Neon.Violet,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadarSweep(Modifier.size(40.dp), color = if (answer.isEmpty()) Neon.Cyan else Neon.Green)
                            Text(
                                "LIFEDESK VOICE · $status", style = MaterialTheme.typography.labelSmall, color = Neon.Cyan,
                                modifier = Modifier.padding(start = 12.dp).weight(1f),
                            )
                        }
                        if (heard.isNotEmpty()) Text("“$heard”", color = Neon.Muted, modifier = Modifier.padding(top = 12.dp))
                        if (answer.isNotEmpty()) TypewriterText(answer, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyLarge)
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = {
                                startActivity(Intent(this@VoiceCommandActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                finish()
                            }) { Text("Open LifeDesk", color = Neon.Violet) }
                            TextButton(onClick = { finish() }) { Text("Close", color = Neon.Muted) }
                        }
                    }
                }
            }
        }
        if (savedInstanceState == null) {
            // Text may arrive pre-transcribed (e.g. from an assistant); otherwise listen now.
            intent.getStringExtra(EXTRA_TEXT)?.takeIf { it.isNotBlank() }?.let { execute(it) } ?: listen()
        }
    }

    private fun listen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a command — e.g. “remind me to pay DEWA 450 on Friday”")
        try {
            recognizer.launch(intent)
        } catch (_: ActivityNotFoundException) {
            status = "Unavailable"
            answer = "Speech recognition isn't available on this phone."
        }
    }

    private fun execute(text: String) {
        heard = text
        status = "Working…"
        lifecycleScope.launch {
            val reply = runCatching { withContext(Dispatchers.IO) { CommandProcessor.handle(applicationContext, text) } }
                .getOrElse { "Sorry, something went wrong with that command." }
            answer = reply
            status = "Done"
            Speaker.speak(applicationContext, reply)
            Reminders.notify(applicationContext, NOTIFICATION_ID, "LifeDesk: $text", reply, itemId = null, channel = Reminders.CHANNEL_VOICE)
            delay(4_000L + reply.length * 55L)
            finish()
        }
    }

    companion object {
        const val EXTRA_TEXT = "com.lifedesk.app.extra.VOICE_TEXT"
        private const val NOTIFICATION_ID = 7_001
    }
}
