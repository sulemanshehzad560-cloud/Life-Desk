package com.lifedesk.app.domain

import com.lifedesk.app.data.Category
import com.lifedesk.app.data.DateKind
import com.lifedesk.app.data.LifeItem
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class Urgency { OVERDUE, URGENT, UPCOMING, MONITORED, NO_DATE }

fun LifeItem.daysLeft(today: LocalDate): Long? = dueDate?.let { ChronoUnit.DAYS.between(today, it) }

fun LifeItem.isSnoozed(today: LocalDate): Boolean = snoozedUntil?.isAfter(today) == true

fun LifeItem.urgency(today: LocalDate): Urgency {
    val d = daysLeft(today) ?: return Urgency.NO_DATE
    return when {
        d < 0 -> Urgency.OVERDUE
        d <= category.urgentDays -> Urgency.URGENT
        d <= category.upcomingDays -> Urgency.UPCOMING
        else -> Urgency.MONITORED
    }
}

/** Items that should appear in "needs attention": overdue, urgent or upcoming, and not snoozed. */
fun LifeItem.needsAttention(today: LocalDate): Boolean =
    !archived && !isSnoozed(today) && urgency(today) in setOf(Urgency.OVERDUE, Urgency.URGENT, Urgency.UPCOMING)

/** "in 12 days", "tomorrow", "in 7 months", "3 days ago". */
fun relativeDays(days: Long): String = when {
    days == 0L -> "today"
    days == 1L -> "tomorrow"
    days == -1L -> "yesterday"
    days < 0 -> "${-days} days ago"
    days < 60 -> "in $days days"
    days < 730 -> "in ${days / 30} months"
    else -> "in ${days / 365} years"
}

/** Short status line for list rows: "Expires in 12 days", "Payment due tomorrow", "Overdue by 2 days". */
fun LifeItem.countdown(today: LocalDate): String {
    val d = daysLeft(today) ?: return "No date set"
    if (d < 0) return "Overdue by ${-d} day" + if (d == -1L) "" else "s"
    return "${kind.label} ${relativeDays(d)}"
}

/** Full sentence for the detail screen: "Your Car insurance expires in 32 days." */
fun LifeItem.headline(today: LocalDate): String {
    val d = daysLeft(today) ?: return "$title has no date yet — add one so LifeDesk can remind you."
    return if (d < 0) "$title ${if (kind == DateKind.EXPIRY) "expired" else "was due"} ${relativeDays(d)}."
    else "$title ${kind.verb} ${relativeDays(d)}."
}

enum class ActionType { COMPLETE, SNOOZE, CONTACT, COMPARE, REVIEW_SUBSCRIPTION, MARK_USED, PLAN_RENEWAL, WARRANTY_CLAIM, NEW_DOCUMENT, ADD_TO_CALENDAR, SHARE }

/** Label for the main "finish the job" button, tuned per category. */
fun LifeItem.completeLabel(): String = when {
    recurrence != com.lifedesk.app.data.Recurrence.NONE && kind == DateKind.PAYMENT_DUE -> "Mark as paid"
    kind == DateKind.PAYMENT_DUE -> "Mark as paid"
    kind == DateKind.RENEWAL || kind == DateKind.EXPIRY -> "Mark as renewed"
    kind == DateKind.APPOINTMENT -> "Mark as done"
    kind == DateKind.WARRANTY_END -> "Archive warranty"
    else -> "Mark as done"
}

/** The context-aware actions shown on an item, most useful first. */
fun LifeItem.suggestedActions(): List<ActionType> {
    val list = mutableListOf<ActionType>()
    when (category) {
        Category.INSURANCE, Category.VEHICLE -> list += ActionType.COMPARE
        Category.ID_DOCUMENT -> list += ActionType.PLAN_RENEWAL
        Category.WARRANTY -> list += ActionType.WARRANTY_CLAIM
        Category.SUBSCRIPTION -> { list += ActionType.MARK_USED; list += ActionType.REVIEW_SUBSCRIPTION }
        else -> Unit
    }
    list += ActionType.COMPLETE
    list += ActionType.SNOOZE
    list += ActionType.CONTACT
    list += ActionType.NEW_DOCUMENT
    if (dueDate != null) list += ActionType.ADD_TO_CALENDAR
    list += ActionType.SHARE
    return list
}

/** Checklist shown for "Plan renewal" on ID documents (UAE-focused, generic elsewhere). */
fun renewalChecklist(item: LifeItem): List<String> {
    val t = (item.title + " " + (item.notes ?: "")).lowercase()
    return when {
        "passport" in t -> listOf(
            "Check your embassy/consulate appointment availability",
            "Passport photos (recent, white background)",
            "Copy of current passport and Emirates ID",
            "Residence visa copy",
            "Update your Emirates ID/visa records after renewal",
        )
        "emirates id" in t || "identity" in t -> listOf(
            "Apply via the ICP app or website (or a typing centre)",
            "Valid passport and residence visa",
            "Recent photo",
            "Book biometrics appointment if requested",
        )
        "visa" in t -> listOf(
            "Valid passport (6+ months validity)",
            "Medical fitness test",
            "Emirates ID renewal application",
            "Sponsor / employer documents",
        )
        "licen" in t -> listOf(
            "Eye test at an approved optician",
            "Valid Emirates ID",
            "Clear any outstanding traffic fines",
            "Renew via RTA/ADP app or service centre",
        )
        else -> listOf(
            "Find the renewal requirements",
            "Gather copies of your current document and ID",
            "Book an appointment if required",
            "Upload the new document to LifeDesk when done",
        )
    }
}

/**
 * Reminder cadence. Returns true if a notification should fire today for this item.
 * Long-lead documents get extra early nudges (e.g. passports 6 and 3 months out).
 */
fun LifeItem.reminderDueToday(today: LocalDate): Boolean {
    if (archived || isSnoozed(today)) return false
    val d = daysLeft(today) ?: return false
    if (d in -3..-1) return true
    val marks = mutableSetOf(0L, 1L, 3L, 7L, 14L, 30L)
    if (category.upcomingDays > 60) marks += setOf(60L, 90L, 180L)
    if (category.urgentDays > 7) marks += category.urgentDays.toLong()
    // A snooze that just ended should bring the item back immediately.
    if (snoozedUntil == today && d <= category.upcomingDays) return true
    return d in marks
}
