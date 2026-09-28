package com.lifedesk.app.domain

import com.lifedesk.app.data.Category
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Understands a one-line reminder typed or spoken by the user, e.g.
 * "DEWA bill 450 dirhams due next Friday", "Renew car registration on 12 March 2027",
 * "Gym 250 monthly tomorrow", "Pay school fees in 2 weeks".
 */
object QuickAdd {
    private val weekdays = DayOfWeek.entries.associateBy { it.name.lowercase() }
    private val wordNumbers = mapOf("a" to 1, "an" to 1, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6,
        "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10, "twelve" to 12)

    private val relIn = Regex("(?i)\\bin\\s+(\\d+|a|an|one|two|three|four|five|six|seven|eight|nine|ten|twelve)\\s+(day|week|month|year)s?\\b")
    private val relNextWeekday = Regex("(?i)\\b(this|next|coming)?\\s*(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b")
    private val relNext = Regex("(?i)\\bnext\\s+(week|month|year)\\b")
    private val dayOfMonth = Regex("(?i)\\b(?:on\\s+)?(?:the\\s+)?(\\d{1,2})(st|nd|rd|th)\\b(?!\\s*(?:of\\s+)?(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec))")
    private val money = Regex("(?i)(\\d{1,3}(?:,\\d{3})+(?:\\.\\d{1,2})?|\\d+(?:\\.\\d{1,2})?)\\s*(aed|dhs?|dirhams?|usd|dollars?|\\$|sar|riyals?|inr|rupees?|gbp|pounds?|eur|euros?)?")
    private val filler = Regex(
        "(?i)\\b(remind me( to)?|reminder( to)?|don'?t forget( to)?|i need to|need to|please|due|on|by|before|at|pay|is|are|the|for|every|monthly|yearly|annually|weekly|quarterly|dirhams?|dhs?|aed|usd|dollars?|sar|riyals?|inr|rupees?|gbp|pounds?|eur|euros?|today|tonight|tomorrow|day after|end of( the)? month|this|next|coming|in)\\b",
    )

    fun relativeDate(text: String, today: LocalDate): LocalDate? {
        val t = text.lowercase()
        if ("day after tomorrow" in t) return today.plusDays(2)
        if (Regex("\\btomorrow\\b").containsMatchIn(t)) return today.plusDays(1)
        if (Regex("\\b(today|tonight)\\b").containsMatchIn(t)) return today
        if (Regex("\\bend of (the )?month\\b").containsMatchIn(t)) return today.with(TemporalAdjusters.lastDayOfMonth())
        relIn.find(t)?.let { m ->
            val n = m.groupValues[1].toIntOrNull() ?: wordNumbers[m.groupValues[1]] ?: 1
            return when (m.groupValues[2]) {
                "day" -> today.plusDays(n.toLong())
                "week" -> today.plusWeeks(n.toLong())
                "month" -> today.plusMonths(n.toLong())
                else -> today.plusYears(n.toLong())
            }
        }
        relNext.find(t)?.let { m ->
            return when (m.groupValues[1]) { "week" -> today.plusWeeks(1); "month" -> today.plusMonths(1); else -> today.plusYears(1) }
        }
        relNextWeekday.find(t)?.let { m ->
            val dow = weekdays.getValue(m.groupValues[2])
            return today.with(TemporalAdjusters.next(dow))
        }
        dayOfMonth.find(t)?.let { m ->
            val d = m.groupValues[1].toInt()
            if (d in 1..31) {
                val thisMonth = runCatching { today.withDayOfMonth(d) }.getOrNull()
                return if (thisMonth != null && !thisMonth.isBefore(today)) thisMonth
                else runCatching { today.plusMonths(1).withDayOfMonth(d) }.getOrElse { today.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()) }
            }
        }
        return null
    }

    fun parse(text: String, today: LocalDate = LocalDate.now(), currency: String = "AED"): ParsedDocument {
        val base = DocumentParser.parse(text, today, currency)
        val date = base.date?.takeIf { !it.isBefore(today.minusDays(1)) } ?: relativeDate(text, today) ?: base.date

        // Amount: prefer one with a currency word; ignore numbers that belong to dates or "in 2 weeks".
        val withoutDates = relIn.replace(dayOfMonth.replace(text, " "), " ")
            .replace(Regex("\\b\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2,4}\\b"), " ")
            .replace(Regex("(?i)\\b\\d{1,2}(st|nd|rd|th)?\\s+(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\.?\\s*\\d{0,4}"), " ")
        val amounts = money.findAll(withoutDates).mapNotNull { m ->
            val v = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: return@mapNotNull null
            if (m.groupValues[2].isEmpty() && v in 1900.0..2100.0) return@mapNotNull null
            v to m.groupValues[2].isNotEmpty()
        }.toList()
        val amount = base.amount ?: (amounts.firstOrNull { it.second } ?: amounts.firstOrNull())?.first

        val cleaned = withoutDates
            .replace(money, " ")
            .replace(relNextWeekday, " ")
            .replace(filler, " ")
            .replace(Regex("[^\\p{L}\\p{N}&' -]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .replaceFirstChar { it.uppercase() }
        val title = when {
            cleaned.length >= 3 -> cleaned
            base.category != Category.OTHER -> base.title
            else -> text.trim().take(40)
        }
        return base.copy(title = title, date = date, amount = amount, currency = base.currency ?: currency)
    }
}
