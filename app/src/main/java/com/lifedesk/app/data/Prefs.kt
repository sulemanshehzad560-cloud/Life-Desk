package com.lifedesk.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Settings(
    val name: String = "",
    val currency: String = "AED",
    val reminderHour: Int = 9,
    val onboarded: Boolean = false,
)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("lifedesk", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private fun read() = Settings(
        name = sp.getString("name", "") ?: "",
        currency = sp.getString("currency", "AED") ?: "AED",
        reminderHour = sp.getInt("reminderHour", 9),
        onboarded = sp.getBoolean("onboarded", false),
    )

    fun update(transform: (Settings) -> Settings) {
        val s = transform(_settings.value)
        sp.edit()
            .putString("name", s.name)
            .putString("currency", s.currency)
            .putInt("reminderHour", s.reminderHour)
            .putBoolean("onboarded", s.onboarded)
            .apply()
        _settings.value = s
    }
}
