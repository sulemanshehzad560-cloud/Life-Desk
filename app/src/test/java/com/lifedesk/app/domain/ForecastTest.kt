package com.lifedesk.app.domain

import com.lifedesk.app.data.Category
import com.lifedesk.app.data.LifeItem
import com.lifedesk.app.data.Recurrence
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ForecastTest {
    private val today = LocalDate.of(2026, 9, 28)

    @Test fun expandsRecurringItems() {
        val items = listOf(
            LifeItem(title = "Netflix", category = Category.SUBSCRIPTION, amount = 49.0, dueDate = LocalDate.of(2026, 10, 5), recurrence = Recurrence.MONTHLY),
            LifeItem(title = "Car insurance", category = Category.INSURANCE, amount = 2850.0, dueDate = LocalDate.of(2027, 2, 1), recurrence = Recurrence.YEARLY),
            LifeItem(title = "Old fine", category = Category.FINE, amount = 300.0, dueDate = LocalDate.of(2026, 8, 1)),
            LifeItem(title = "Past monthly", category = Category.BILL, amount = 100.0, dueDate = LocalDate.of(2026, 6, 15), recurrence = Recurrence.MONTHLY),
        )
        val f = forecast(items, today)
        assertEquals(12, f.size)
        assertEquals(YearMonth.of(2026, 9), f[0].month)
        // September: overdue fine + the monthly bill rolled forward to 15 Sep.
        assertEquals(400.0, f[0].total, 0.001)
        // October: Netflix + bill
        assertEquals(149.0, f[1].total, 0.001)
        // February: Netflix + bill + insurance
        assertEquals(2999.0, f[5].total, 0.001)
        assertEquals(2850.0, f[5].byCategory[Category.INSURANCE]!!, 0.001)
        // Netflix appears Oct..Aug = 11 times
        assertEquals(11 * 49.0, f.sumOf { it.byCategory[Category.SUBSCRIPTION] ?: 0.0 }, 0.001)
    }
}
