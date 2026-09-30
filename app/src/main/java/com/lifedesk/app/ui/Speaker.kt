package com.lifedesk.app.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Reads briefings and assistant answers aloud using the phone's text-to-speech engine. */
object Speaker {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null

    fun speak(context: Context, text: String) {
        val engine = tts
        if (engine == null) {
            pending = text
            tts = TextToSpeech(context.applicationContext) { status ->
                ready = status == TextToSpeech.SUCCESS
                if (ready) {
                    tts?.language = Locale.UK
                    tts?.setSpeechRate(1.02f)
                    pending?.let { say(it) }
                    pending = null
                }
            }
        } else if (ready) {
            say(text)
        } else {
            pending = text
        }
    }

    private fun say(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lifedesk")
    }

    fun stop() {
        tts?.stop()
    }
}
