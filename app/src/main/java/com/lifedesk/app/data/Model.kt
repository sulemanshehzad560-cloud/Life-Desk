package com.lifedesk.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** What kind of date an item is tracking. [verb] completes the sentence "Your passport ___ in 7 months." */
enum class DateKind(val label: String, val verb: String) {
    EXPIRY("Expires", "expires"),
    PAYMENT_DUE("Payment due", "is due"),
    RENEWAL("Renewal", "renews"),
    WARRANTY_END("Warranty ends", "warranty ends"),
    APPOINTMENT("Appointment", "is scheduled"),
    DEADLINE("Deadline", "is due"),
}

enum class Recurrence(val label: String, val perMonth: Double) {
    NONE("One-off", 0.0),
    WEEKLY("Weekly", 52.0 / 12.0),
    MONTHLY("Monthly", 1.0),
    QUARTERLY("Quarterly", 1.0 / 3.0),
    YEARLY("Yearly", 1.0 / 12.0);

    fun next(date: LocalDate): LocalDate = when (this) {
        NONE -> date
        WEEKLY -> date.plusWeeks(1)
        MONTHLY -> date.plusMonths(1)
        QUARTERLY -> date.plusMonths(3)
        YEARLY -> date.plusYears(1)
    }
}

/**
 * Category of a life-admin item. [urgentDays]/[upcomingDays] control when an item turns
 * red/orange: a passport needs attention months ahead, a credit card only days ahead.
 */
enum class Category(
    val label: String,
    val emoji: String,
    val defaultKind: DateKind,
    val defaultRecurrence: Recurrence,
    val urgentDays: Int,
    val upcomingDays: Int,
) {
    INSURANCE("Insurance", "🛡️", DateKind.RENEWAL, Recurrence.YEARLY, 14, 45),
    BILL("Bills & utilities", "🧾", DateKind.PAYMENT_DUE, Recurrence.MONTHLY, 3, 14),
    CREDIT_CARD("Credit card", "💳", DateKind.PAYMENT_DUE, Recurrence.MONTHLY, 3, 14),
    LOAN("Loan", "🏦", DateKind.PAYMENT_DUE, Recurrence.MONTHLY, 3, 14),
    SUBSCRIPTION("Subscription", "🔁", DateKind.PAYMENT_DUE, Recurrence.MONTHLY, 3, 10),
    ID_DOCUMENT("ID & documents", "🪪", DateKind.EXPIRY, Recurrence.NONE, 30, 240),
    VEHICLE("Vehicle", "🚗", DateKind.RENEWAL, Recurrence.YEARLY, 14, 45),
    RENT("Rent & tenancy", "🏠", DateKind.PAYMENT_DUE, Recurrence.NONE, 7, 45),
    WARRANTY("Warranty", "🧰", DateKind.WARRANTY_END, Recurrence.NONE, 14, 60),
    SCHOOL("School fees", "🎓", DateKind.PAYMENT_DUE, Recurrence.NONE, 7, 30),
    MEDICAL("Medical", "🩺", DateKind.APPOINTMENT, Recurrence.NONE, 1, 14),
    TRAVEL("Travel", "✈️", DateKind.APPOINTMENT, Recurrence.NONE, 3, 30),
    FINE("Fines", "🚨", DateKind.PAYMENT_DUE, Recurrence.NONE, 7, 30),
    OTHER("Other", "📌", DateKind.DEADLINE, Recurrence.NONE, 3, 30),
}

@Entity(tableName = "items")
data class LifeItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: Category,
    val kind: DateKind = category.defaultKind,
    val provider: String? = null,
    val amount: Double? = null,
    val currency: String = "AED",
    val dueDate: LocalDate? = null,
    val recurrence: Recurrence = Recurrence.NONE,
    val referenceNumber: String? = null,
    /** Groups items that belong to the same thing, e.g. "Toyota Camry" or "Washing machine". */
    val asset: String? = null,
    val notes: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val imagePath: String? = null,
    val rawText: String? = null,
    /** Subscriptions only: last time the user said they used it. */
    val lastUsed: LocalDate? = null,
    val snoozedUntil: LocalDate? = null,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Key used to compare prices between invoices of the same provider. */
fun priceKeyOf(category: Category, provider: String?): String? =
    provider?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }?.let { "${category.name}:$it" }

val LifeItem.priceKey: String? get() = priceKeyOf(category, provider)

/** Amount normalised to a monthly figure for recurring items, null for one-off items. */
val LifeItem.monthlyCost: Double?
    get() = amount?.let { a -> if (recurrence == Recurrence.NONE) null else a * recurrence.perMonth }

/** One observed amount for a provider — used to detect "Netflix went from AED 39 → AED 49". */
@Entity(tableName = "prices", indices = [Index("key"), Index("itemId")])
data class PriceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val itemId: Long,
    val amount: Double,
    val currency: String,
    val date: LocalDate,
)
