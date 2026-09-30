package com.lifedesk.app.domain

import com.lifedesk.app.data.LifeItem

/** What a spoken or typed command asks LifeDesk to do. */
sealed interface VoiceCommand {
    data class Ask(val question: String) : VoiceCommand
    data class Complete(val target: String) : VoiceCommand
    data class Snooze(val target: String, val days: Long) : VoiceCommand
    data class Add(val text: String) : VoiceCommand
    data object Briefing : VoiceCommand
}

/**
 * Turns an utterance into a [VoiceCommand]. Pure Kotlin (unit-tested).
 * Examples: "Hey LifeDesk, remind me to pay DEWA 450 next Friday" → Add,
 * "mark credit card as paid" → Complete, "snooze car insurance for a week" → Snooze,
 * "when does my passport expire" → Ask, "what's my briefing" → Briefing.
 */
object VoiceCommands {
    private val wake = Regex("(?i)^\\s*((hey|hi|ok|okay)\\s+)?(life\\s?desk)[,.!\\s]*")
    private val complete = Regex("(?i)^(mark|set)\\s+(.+?)\\s+(as\\s+)?(paid|done|complete|completed|renewed)\\s*$|^(i\\s+)?(paid|renewed|finished|completed)\\s+(the\\s+|my\\s+)?(.+)$")
    private val snooze = Regex("(?i)^(snooze|postpone|delay|remind me later about)\\s+(the\\s+|my\\s+)?(.+?)(\\s+(for|by)\\s+(a|an|one|two|three|\\d+)\\s+(day|days|week|weeks))?\\s*$")
    private val briefing = Regex("(?i)^(what'?s|give me|read|tell me)?\\s*(my|the|today'?s)?\\s*(briefing|summary|update|agenda)\\b|^what(\\s+is|'s)?\\s+(due|coming up)\\s*(today|this week)?\\??$")
    private val numbers = mapOf("a" to 1, "an" to 1, "one" to 1, "two" to 2, "three" to 3)

    fun stripWakeWord(text: String): String = wake.replace(text, "").trim().trimEnd('.', '!')

    fun parse(raw: String): VoiceCommand {
        val text = stripWakeWord(raw)
        if (briefing.containsMatchIn(text)) return VoiceCommand.Briefing
        complete.matchEntire(text)?.let { m ->
            val target = m.groupValues[2].ifBlank { m.groupValues[8] }
            return VoiceCommand.Complete(target.trim())
        }
        snooze.matchEntire(text)?.let { m ->
            val n = m.groupValues[6].let { it.toIntOrNull() ?: numbers[it.lowercase()] } ?: 1
            val days = if (m.groupValues[4].isBlank()) 3L else if (m.groupValues[7].startsWith("week")) n * 7L else n.toLong()
            return VoiceCommand.Snooze(m.groupValues[3].trim(), days)
        }
        if (Briefing.isQuestion(text)) return VoiceCommand.Ask(text)
        return VoiceCommand.Add(text)
    }

    /** Best item for a spoken name ("credit card" → "Emirates NBD credit card"), or null if nothing fits. */
    fun findItem(target: String, items: List<LifeItem>): LifeItem? {
        val words = target.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length > 1 && it !in setOf("my", "the", "bill") }
        if (words.isEmpty()) return null
        return items.filter { !it.archived }.map { item ->
            val hay = listOfNotNull(item.title, item.provider, item.asset).joinToString(" ").lowercase()
            item to words.count { it in hay }
        }.filter { it.second > 0 }
            .maxWithOrNull(compareBy<Pair<LifeItem, Int>> { it.second }.thenBy { -(it.first.dueDate?.toEpochDay() ?: Long.MAX_VALUE) })
            ?.first
    }
}
