package com.lifedesk.app.domain

import com.lifedesk.app.data.Category
import com.lifedesk.app.data.DateKind
import com.lifedesk.app.data.LifeItem
import com.lifedesk.app.data.monthlyCost
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class QueryResult(val items: List<LifeItem>, val answer: String? = null)

/**
 * Understands everyday questions without an internet connection:
 * "When does my insurance expire?", "How much do I spend on subscriptions?",
 * "Show documents expiring this month", "What payments are due this week?"
 */
object QueryEngine {

    private val stopWords = setOf(
        "show", "me", "my", "the", "a", "an", "what", "which", "when", "does", "do", "is", "are", "i", "on", "for", "of", "in",
        "all", "list", "find", "how", "much", "many", "did", "spend", "spending", "spent", "pay", "paying", "payments", "payment",
        "due", "expire", "expires", "expiring", "expiry", "renew", "renewal", "this", "next", "week", "month", "year", "today",
        "tomorrow", "overdue", "late", "things", "items", "stuff", "to", "be", "will", "have", "has", "that", "there", "any",
        "documents", "document", "upcoming", "soon", "total", "cost", "costs", "per", "and", "or", "with", "get", "give", "whats",
        "what's", "need", "needs", "attention", "about", "at", "by", "coming", "up", "left", "until", "days", "day",
    )

    private val categoryWords: Map<String, Category> = buildMap {
        listOf("insurance", "policy", "insurances").forEach { put(it, Category.INSURANCE) }
        listOf("bill", "bills", "utility", "utilities", "electricity", "water", "internet", "mobile", "dewa", "addc", "du", "etisalat").forEach { put(it, Category.BILL) }
        listOf("card", "cards", "credit").forEach { put(it, Category.CREDIT_CARD) }
        listOf("loan", "loans", "mortgage", "emi").forEach { put(it, Category.LOAN) }
        listOf("subscription", "subscriptions", "subs", "streaming").forEach { put(it, Category.SUBSCRIPTION) }
        listOf("passport", "passports", "id", "ids", "visa", "licence", "license", "identity").forEach { put(it, Category.ID_DOCUMENT) }
        listOf("car", "vehicle", "vehicles", "registration", "mulkiya", "salik").forEach { put(it, Category.VEHICLE) }
        listOf("rent", "tenancy", "lease", "ejari").forEach { put(it, Category.RENT) }
        listOf("warranty", "warranties", "guarantee").forEach { put(it, Category.WARRANTY) }
        listOf("school", "tuition", "fees").forEach { put(it, Category.SCHOOL) }
        listOf("medical", "doctor", "appointment", "appointments", "clinic", "dentist").forEach { put(it, Category.MEDICAL) }
        listOf("flight", "flights", "travel", "trip", "hotel").forEach { put(it, Category.TRAVEL) }
        listOf("fine", "fines", "penalty", "parking").forEach { put(it, Category.FINE) }
    }

    fun run(query: String, items: List<LifeItem>, today: LocalDate = LocalDate.now(), currency: String = "AED"): QueryResult {
        val q = query.lowercase().replace(Regex("[?!.,]"), " ").trim()
        val active = items.filter { !it.archived }
        if (q.isBlank()) return QueryResult(active)
        val words = q.split(Regex("\\s+"))

        var list = active
        var range: ClosedRange<LocalDate>? = null
        var rangeLabel = ""
        when {
            "overdue" in words || "late" in words -> { list = list.filter { it.dueDate?.isBefore(today) == true }; rangeLabel = "overdue" }
            "today" in words -> { range = today..today; rangeLabel = "today" }
            "tomorrow" in words -> { range = today.plusDays(1)..today.plusDays(1); rangeLabel = "tomorrow" }
            q.contains("next week") -> {
                val start = today.with(TemporalAdjusters.next(java.time.DayOfWeek.MONDAY))
                range = start..start.plusDays(6); rangeLabel = "next week"
            }
            q.contains("this week") || q.contains("7 days") -> { range = today..today.plusDays(7); rangeLabel = "this week" }
            q.contains("next month") -> {
                val start = today.plusMonths(1).withDayOfMonth(1)
                range = start..start.with(TemporalAdjusters.lastDayOfMonth()); rangeLabel = "next month"
            }
            q.contains("this month") -> { range = today.withDayOfMonth(1)..today.with(TemporalAdjusters.lastDayOfMonth()); rangeLabel = "this month" }
            q.contains("this year") -> { range = today.withDayOfYear(1)..today.with(TemporalAdjusters.lastDayOfYear()); rangeLabel = "this year" }
            "soon" in words || "upcoming" in words || q.contains("coming up") -> { range = today..today.plusDays(30); rangeLabel = "in the next 30 days" }
        }
        range?.let { r -> list = list.filter { it.dueDate != null && it.dueDate in r } }

        val wantsExpiry = words.any { it.startsWith("expir") || it.startsWith("renew") }
        val wantsPayments = words.any { it == "due" || it.startsWith("pay") || it == "bills" }
        if (wantsExpiry) list = list.filter { it.kind in setOf(DateKind.EXPIRY, DateKind.RENEWAL, DateKind.WARRANTY_END) }
        else if (wantsPayments && !words.any { categoryWords[it] != null && categoryWords[it] != Category.BILL }) {
            list = list.filter { it.kind == DateKind.PAYMENT_DUE || it.category == Category.BILL }
        }
        if (words.any { it == "documents" || it == "document" } && !wantsPayments) {
            list = list.filter { it.imagePath != null || it.category == Category.ID_DOCUMENT || it.kind == DateKind.EXPIRY }
        }

        val cats = words.mapNotNull { categoryWords[it] }.toSet()
        if (cats.isNotEmpty()) list = list.filter { it.category in cats || cats.any { c -> c == Category.VEHICLE && it.asset != null && it.category == Category.INSURANCE } }

        val free = words.filter { it !in stopWords && it !in categoryWords && it.length > 1 }
        if (free.isNotEmpty()) {
            val byText = list.filter { item -> free.all { w -> item.searchText().contains(w) } }
            // Unknown words ("my car's", "Toyota") narrow the list only when they match something.
            if (byText.isNotEmpty() || cats.isEmpty() && range == null && rangeLabel.isEmpty() && !wantsExpiry && !wantsPayments) list = byText
        }
        list = list.sortedWith(compareBy(nullsLast()) { it.dueDate })

        return QueryResult(list, answer(q, words, list, today, currency, rangeLabel))
    }

    private fun LifeItem.searchText() =
        listOfNotNull(title, provider, notes, referenceNumber, asset, category.label, kind.label).joinToString(" ").lowercase()

    private fun answer(q: String, words: List<String>, list: List<LifeItem>, today: LocalDate, currency: String, rangeLabel: String): String? {
        if (list.isEmpty()) return "Nothing found${if (rangeLabel.isNotEmpty()) " $rangeLabel" else ""}."
        if (q.contains("how much") || words.any { it in setOf("spend", "spending", "spent", "total", "cost", "costs") }) {
            val monthly = list.mapNotNull { it.monthlyCost }
            if (monthly.isNotEmpty() && rangeLabel.isEmpty()) {
                val total = monthly.sum()
                return "You're paying ${money(total, currency)}/month (${money(total * 12, currency)}/year) across ${monthly.size} recurring item${if (monthly.size == 1) "" else "s"}."
            }
            val total = list.mapNotNull { it.amount }.sum()
            return "Total: ${money(total, currency)} across ${list.size} item${if (list.size == 1) "" else "s"}${if (rangeLabel.isNotEmpty()) " $rangeLabel" else ""}."
        }
        if (words.firstOrNull() == "when" || q.startsWith("when")) {
            val first = list.firstOrNull { it.dueDate != null } ?: return null
            return "${first.headline(today).removeSuffix(".")} — ${first.dueDate!!.pretty()}."
        }
        if (q.contains("how many")) return "${list.size} item${if (list.size == 1) "" else "s"}."
        val sum = list.mapNotNull { it.amount }.sum()
        return "${list.size} item${if (list.size == 1) "" else "s"}${if (rangeLabel.isNotEmpty()) " $rangeLabel" else ""}" +
            if (sum > 0 && list.all { it.kind == DateKind.PAYMENT_DUE }) " · ${money(sum, currency)} total" else ""
    }
}
