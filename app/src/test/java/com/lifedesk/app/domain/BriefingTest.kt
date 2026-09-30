package com.lifedesk.app.domain

import com.lifedesk.app.data.Category
import com.lifedesk.app.data.LifeItem
import com.lifedesk.app.data.PriceRecord
import com.lifedesk.app.data.Recurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BriefingTest {
    private val today = LocalDate.of(2026, 9, 28) // Monday

    @Test fun questionsVsCommands() {
        assertTrue(Briefing.isQuestion("When does my passport expire?"))
        assertTrue(Briefing.isQuestion("how much do I spend on subscriptions"))
        assertTrue(Briefing.isQuestion("show documents expiring this month"))
        assertFalse(Briefing.isQuestion("gym 250 monthly on the 1st"))
        assertFalse(Briefing.isQuestion("DEWA bill 450 dirhams due next Friday"))
    }

    @Test fun emptyDesk() {
        assertEquals(1, Briefing.build(emptyList(), emptyList(), today, "AED").size)
    }

    @Test fun summarisesWeekRenewalExpiryAndSavings() {
        val items = listOf(
            LifeItem(id = 1, title = "Credit card", category = Category.CREDIT_CARD, amount = 1240.0, dueDate = today.plusDays(3), recurrence = Recurrence.MONTHLY),
            LifeItem(id = 2, title = "DEWA", category = Category.BILL, amount = 300.0, dueDate = today.plusDays(1), recurrence = Recurrence.MONTHLY),
            LifeItem(id = 3, title = "Car insurance", category = Category.INSURANCE, amount = 2850.0, dueDate = today.plusDays(20), recurrence = Recurrence.YEARLY),
            LifeItem(id = 4, title = "Passport", category = Category.ID_DOCUMENT, dueDate = today.plusDays(120)),
            LifeItem(id = 5, title = "Gym", category = Category.SUBSCRIPTION, amount = 250.0, dueDate = today.plusDays(30),
                recurrence = Recurrence.MONTHLY, lastUsed = today.minusDays(50)),
        )
        val prices = listOf(
            PriceRecord(key = "k", itemId = 3, amount = 2640.0, currency = "AED", date = today.minusDays(40)),
            PriceRecord(key = "k", itemId = 3, amount = 2850.0, currency = "AED", date = today.minusDays(10)),
        )
        val lines = Briefing.build(items, prices, today, "AED")
        assertEquals(4, lines.size)
        assertTrue(lines[0], lines[0].startsWith("This week: 2 payments totalling AED 1,540"))
        assertTrue(lines[0], lines[0].contains("DEWA (tomorrow)"))
        assertTrue(lines[1], lines[1].contains("Car insurance renews in 20 days") && lines[1].contains("went up 8%"))
        assertTrue(lines[2], lines[2].startsWith("Passport expires in 4 months"))
        assertTrue(lines[3], lines[3].contains("AED 250 a month"))
    }
}
