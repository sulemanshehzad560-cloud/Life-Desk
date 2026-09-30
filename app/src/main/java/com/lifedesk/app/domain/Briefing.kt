package com.lifedesk.app.domain

import com.lifedesk.app.data.DateKind
import com.lifedesk.app.data.LifeItem
import com.lifedesk.app.data.PriceRecord
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * The daily AI-style briefing: a few plain sentences about what matters now, generated from the user's items.
 * Pure Kotlin so it can be unit-tested; the UI shows it and can read it aloud.
 */
object Briefing {

    private val questionStart = Regex(
        "(?i)^(when|what|what's|whats|how|which|who|where|is|are|do|does|did|show|list|any|find|tell me|give me)\\b",
    )

    /** Distinguishes "When does my passport expire?" (ask) from "gym 250 monthly on the 1st" (add). */
    fun isQuestion(text: String): Boolean {
        val t = text.trim()
        return t.endsWith("?") || questionStart.containsMatchIn(t)
    }

    fun build(items: List<LifeItem>, prices: List<PriceRecord>, today: LocalDate, currency: String): List<String> {
        val active = items.filter { !it.archived }
        if (active.isEmpty()) return listOf("Your desk is empty. Scan a bill, policy or ID and I'll start watching the dates for you.")
        val lines = mutableListOf<String>()
        val o = overview(active, prices, today)

        val overdue = active.filter { it.urgency(today) == Urgency.OVERDUE }.sortedBy { it.dueDate }
        if (overdue.isNotEmpty()) {
            lines += "${overdue.size} item${if (overdue.size == 1) " is" else "s are"} overdue: ${names(overdue)}. Handle ${if (overdue.size == 1) "it" else "these"} first."
        }

        val week = active.filter { i ->
            val d = i.daysLeft(today)
            d != null && d in 0..7 && i.kind in setOf(DateKind.PAYMENT_DUE, DateKind.RENEWAL) && !i.isSnoozed(today)
        }.sortedBy { it.dueDate }
        if (week.isNotEmpty()) {
            val total = week.mapNotNull { it.amount }.sum()
            val when_ = week.take(3).joinToString(", ") { "${it.title} (${day(it.dueDate!!, today)})" }
            lines += "This week: ${week.size} payment${if (week.size == 1) "" else "s"}" +
                (if (total > 0) " totalling ${money(total, currency)}" else "") + " — $when_${if (week.size > 3) " and more" else ""}."
        } else if (overdue.isEmpty()) {
            lines += "Nothing to pay in the next 7 days."
        }

        active.filter { i ->
            val d = i.daysLeft(today)
            d != null && d in 8..45 && i.kind == DateKind.RENEWAL
        }.minByOrNull { it.dueDate!! }?.let { r ->
            val rise = o.priceIncreases.firstOrNull { it.item.id == r.id }
            lines += "${r.title} renews ${relativeDays(r.daysLeft(today)!!)}" +
                (r.amount?.let { " (${money(it, r.currency)})" } ?: "") +
                if (rise != null) ". It went up ${rise.percent}% last time — worth comparing quotes." else ". A good moment to compare quotes."
        }

        active.filter { i ->
            i.kind == DateKind.EXPIRY && i.urgency(today) in setOf(Urgency.URGENT, Urgency.UPCOMING)
        }.minByOrNull { it.dueDate!! }?.let { e ->
            lines += "${e.title} expires ${relativeDays(e.daysLeft(today)!!)} — start planning the renewal."
        }

        if (o.potentialSavings > 0) {
            val n = o.unusedSubscriptions.size
            lines += "You could save ${money(o.potentialSavings, currency)} a month by cancelling $n unused subscription${if (n == 1) "" else "s"}."
        }
        return lines.take(4)
    }

    private fun names(list: List<LifeItem>): String =
        list.take(2).joinToString(" and ") { it.title } + if (list.size > 2) " (+${list.size - 2} more)" else ""

    private fun day(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "today"
        today.plusDays(1) -> "tomorrow"
        else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    }
}
