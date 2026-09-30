package com.lifedesk.app.voice

import android.content.Context
import com.lifedesk.app.LifeDeskApp
import com.lifedesk.app.data.LifeItem
import com.lifedesk.app.data.Recurrence
import com.lifedesk.app.domain.Briefing
import com.lifedesk.app.domain.QueryEngine
import com.lifedesk.app.domain.QuickAdd
import com.lifedesk.app.domain.VoiceCommand
import com.lifedesk.app.domain.VoiceCommands
import com.lifedesk.app.domain.money
import com.lifedesk.app.domain.pretty
import com.lifedesk.app.widget.DueWidget
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Runs a spoken command against the database without the main app being open, and returns what to say back. */
object CommandProcessor {

    suspend fun handle(context: Context, text: String): String {
        val app = context.applicationContext as LifeDeskApp
        val repo = app.repository
        val settings = app.prefs.settings.value
        val today = LocalDate.now()
        val items = repo.activeItems()
        // With app lock on, spoken commands may still add or update, but never read your data out loud.
        val locked = settings.appLock
        val lockedReply = "LifeDesk is locked. Open the app to hear the details."

        val reply = when (val cmd = VoiceCommands.parse(text)) {
            is VoiceCommand.Briefing ->
                if (locked) lockedReply
                else Briefing.build(items, repo.prices.first(), today, settings.currency).joinToString(" ")
                    .ifBlank { "You're all clear. Nothing needs attention." }
            is VoiceCommand.Ask ->
                if (locked) lockedReply
                else QueryEngine.run(cmd.question, items, today, settings.currency).let { r ->
                    r.answer ?: if (r.items.isEmpty()) "I couldn't find anything for that."
                    else "I found " + r.items.take(3).joinToString(", ") { it.title } + "."
                }
            is VoiceCommand.Complete -> {
                val item = VoiceCommands.findItem(cmd.target, items)
                if (item == null) "I couldn't find ${cmd.target} in your LifeDesk."
                else {
                    repo.complete(item)
                    if (item.recurrence == Recurrence.NONE) "Done. ${item.title} is marked complete."
                    else "Done. ${item.title} is paid, and I've scheduled the next one."
                }
            }
            is VoiceCommand.Snooze -> {
                val item = VoiceCommands.findItem(cmd.target, items)
                if (item == null) "I couldn't find ${cmd.target} in your LifeDesk."
                else {
                    repo.snooze(item, cmd.days)
                    "Okay, I'll remind you about ${item.title} in ${if (cmd.days == 7L) "a week" else "${cmd.days} day" + if (cmd.days == 1L) "" else "s"}."
                }
            }
            is VoiceCommand.Add -> {
                if (cmd.text.isBlank()) return "Sorry, I didn't catch that."
                val p = QuickAdd.parse(cmd.text, today, settings.currency)
                val item = LifeItem(
                    title = p.title, category = p.category, kind = p.kind, provider = p.provider, amount = p.amount,
                    currency = p.currency ?: settings.currency, dueDate = p.date, recurrence = p.recurrence,
                    referenceNumber = p.referenceNumber, asset = p.asset,
                )
                repo.save(item)
                "Reminder set: ${item.title}" +
                    (item.dueDate?.let { " on ${it.pretty()}" } ?: ", with no date yet") +
                    (item.amount?.let { " for ${money(it, item.currency)}" } ?: "") + "."
            }
        }
        DueWidget.refresh(app)
        if (app.accounts.account.value != null && settings.autoBackup) {
            runCatching { app.cloud.backup() }.onSuccess { app.prefs.update { s -> s.copy(lastBackupAt = System.currentTimeMillis()) } }
        }
        return reply
    }
}
