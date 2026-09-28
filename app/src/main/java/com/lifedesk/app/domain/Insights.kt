package com.lifedesk.app.domain

import com.lifedesk.app.data.Category
import com.lifedesk.app.data.DateKind
import com.lifedesk.app.data.LifeItem
import com.lifedesk.app.data.PriceRecord
import com.lifedesk.app.data.monthlyCost
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

const val UNUSED_AFTER_DAYS = 30L

fun LifeItem.daysSinceUsed(today: LocalDate): Long {
    val since = lastUsed ?: Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
    return ChronoUnit.DAYS.between(since, today)
}

fun LifeItem.isUnusedSubscription(today: LocalDate) =
    category == Category.SUBSCRIPTION && recurrence != com.lifedesk.app.data.Recurrence.NONE && daysSinceUsed(today) >= UNUSED_AFTER_DAYS

data class PriceChange(val item: LifeItem, val old: Double, val new: Double) {
    val percent: Int get() = percentChange(old, new)
}

data class Overview(
    val attention: List<LifeItem>,
    val urgentCount: Int,
    val upcomingCount: Int,
    val monitoredCount: Int,
    val dueThisMonth: Double,
    val subscriptionsMonthly: Double,
    val unusedSubscriptions: List<LifeItem>,
    val potentialSavings: Double,
    val priceIncreases: List<PriceChange>,
    val documents: Int,
    val documentsExpiringSoon: Int,
    val assets: Map<String, List<LifeItem>>,
)

fun overview(items: List<LifeItem>, prices: List<PriceRecord>, today: LocalDate): Overview {
    val active = items.filter { !it.archived }
    val byUrgency = active.groupBy { if (it.isSnoozed(today)) Urgency.MONITORED else it.urgency(today) }
    val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
    val subs = active.filter { it.category == Category.SUBSCRIPTION }
    val unused = subs.filter { it.isUnusedSubscription(today) }

    // Price increases: compare the two most recent records of each item, only if the latest is recent.
    val increases = prices.groupBy { it.itemId }.mapNotNull { (itemId, recs) ->
        val sorted = recs.sortedWith(compareByDescending<PriceRecord> { it.date }.thenByDescending { it.id })
        val latest = sorted.getOrNull(0) ?: return@mapNotNull null
        val previous = sorted.getOrNull(1) ?: return@mapNotNull null
        val item = active.firstOrNull { it.id == itemId } ?: return@mapNotNull null
        if (latest.amount > previous.amount && ChronoUnit.DAYS.between(latest.date, today) <= 60) PriceChange(item, previous.amount, latest.amount) else null
    }.sortedByDescending { it.percent }

    return Overview(
        attention = active.filter { it.needsAttention(today) }.sortedBy { it.dueDate },
        urgentCount = (byUrgency[Urgency.OVERDUE]?.size ?: 0) + (byUrgency[Urgency.URGENT]?.size ?: 0),
        upcomingCount = byUrgency[Urgency.UPCOMING]?.size ?: 0,
        monitoredCount = (byUrgency[Urgency.MONITORED]?.size ?: 0) + (byUrgency[Urgency.NO_DATE]?.size ?: 0),
        dueThisMonth = active.filter { it.dueDate != null && !it.dueDate.isAfter(monthEnd) && it.kind in setOf(DateKind.PAYMENT_DUE, DateKind.RENEWAL) }
            .mapNotNull { it.amount }.sum(),
        subscriptionsMonthly = subs.mapNotNull { it.monthlyCost }.sum(),
        unusedSubscriptions = unused,
        potentialSavings = unused.mapNotNull { it.monthlyCost }.sum(),
        priceIncreases = increases,
        documents = active.count { it.imagePath != null || it.category == Category.ID_DOCUMENT },
        documentsExpiringSoon = active.count {
            it.kind in setOf(DateKind.EXPIRY, DateKind.WARRANTY_END) && it.urgency(today) in setOf(Urgency.OVERDUE, Urgency.URGENT, Urgency.UPCOMING)
        },
        assets = active.filter { !it.asset.isNullOrBlank() }.groupBy { it.asset!!.trim() },
    )
}

data class MonthForecast(val month: java.time.YearMonth, val total: Double, val byCategory: Map<Category, Double>)

/**
 * Money due in each of the next [months] months, expanding recurring items (a monthly bill appears 12 times,
 * a yearly renewal once). Overdue items are counted in the current month.
 */
fun forecast(items: List<LifeItem>, today: LocalDate, months: Int = 12): List<MonthForecast> {
    val start = java.time.YearMonth.from(today)
    val end = start.plusMonths(months.toLong())
    val buckets = (0 until months).associate { start.plusMonths(it.toLong()) to mutableMapOf<Category, Double>() }
    for (item in items) {
        if (item.archived) continue
        val amount = item.amount ?: continue
        var date = item.dueDate ?: continue
        if (java.time.YearMonth.from(date).isBefore(start)) {
            if (item.recurrence == com.lifedesk.app.data.Recurrence.NONE) date = today
            else while (java.time.YearMonth.from(date).isBefore(start)) date = item.recurrence.next(date)
        }
        var guard = 0
        while (java.time.YearMonth.from(date).isBefore(end) && guard++ < 60) {
            buckets[java.time.YearMonth.from(date)]?.merge(item.category, amount, Double::plus)
            if (item.recurrence == com.lifedesk.app.data.Recurrence.NONE) break
            date = item.recurrence.next(date)
        }
    }
    return buckets.entries.sortedBy { it.key }.map { (m, cats) -> MonthForecast(m, cats.values.sum(), cats) }
}
