package com.lifedesk.app.domain

import com.lifedesk.app.data.Category
import com.lifedesk.app.data.DateKind
import com.lifedesk.app.data.LifeItem
import com.lifedesk.app.data.Recurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class QueryEngineTest {
    private val today = LocalDate.of(2026, 9, 28)
    private val items = listOf(
        LifeItem(id = 1, title = "Car insurance", category = Category.INSURANCE, provider = "ABC Insurance", amount = 2850.0, dueDate = today.plusDays(12), recurrence = Recurrence.YEARLY, asset = "Toyota"),
        LifeItem(id = 2, title = "Emirates NBD credit card", category = Category.CREDIT_CARD, amount = 1240.0, dueDate = today.plusDays(3), recurrence = Recurrence.MONTHLY),
        LifeItem(id = 3, title = "Passport", category = Category.ID_DOCUMENT, dueDate = today.plusMonths(7)),
        LifeItem(id = 4, title = "Netflix", category = Category.SUBSCRIPTION, amount = 49.0, dueDate = today.plusDays(20), recurrence = Recurrence.MONTHLY),
        LifeItem(id = 5, title = "Gym", category = Category.SUBSCRIPTION, amount = 250.0, dueDate = today.plusDays(2), recurrence = Recurrence.MONTHLY),
        LifeItem(id = 6, title = "Old fine", category = Category.FINE, amount = 300.0, dueDate = today.minusDays(2)),
    )

    @Test fun whenDoesInsuranceExpire() {
        val r = QueryEngine.run("When does my insurance expire?", items, today)
        assertEquals(listOf(1L), r.items.map { it.id })
        assertTrue(r.answer!!.contains("Car insurance renews in 12 days"))
    }

    @Test fun subscriptionSpend() {
        val r = QueryEngine.run("How much did I spend on subscriptions?", items, today)
        assertEquals(setOf(4L, 5L), r.items.map { it.id }.toSet())
        assertTrue(r.answer!!, r.answer!!.contains("AED 299/month"))
    }

    @Test fun paymentsDueThisWeek() {
        val r = QueryEngine.run("What payments are due this week?", items, today)
        assertEquals(listOf(5L, 2L), r.items.map { it.id })
    }

    @Test fun overdue() {
        assertEquals(listOf(6L), QueryEngine.run("overdue", items, today).items.map { it.id })
    }

    @Test fun freeTextSearch() {
        assertEquals(listOf(4L), QueryEngine.run("netflix", items, today).items.map { it.id })
        assertEquals(listOf(1L), QueryEngine.run("toyota", items, today).items.map { it.id })
    }

    @Test fun urgencyUsesCategoryWindows() {
        assertEquals(Urgency.URGENT, items[0].urgency(today)) // insurance turns red 14 days out
        assertEquals(Urgency.URGENT, items[1].urgency(today))
        assertEquals(Urgency.UPCOMING, items[2].urgency(today)) // passport: 7 months is inside its 240-day window
        assertEquals(Urgency.OVERDUE, items[5].urgency(today))
        assertEquals(DateKind.RENEWAL, items[0].kind)
    }
}
