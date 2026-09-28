package com.lifedesk.app.domain

import com.lifedesk.app.data.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExtractionTest {
    private val today = LocalDate.of(2026, 9, 28) // a Monday

    @Test fun tenancyChequeScheduleAndFields() {
        val text = """
            TENANCY CONTRACT
            Ejari No: 0123456789
            Landlord: Al Noor Properties LLC
            Property   Apartment 1204, Marina Heights
            Annual Rent: AED 85,000
            Cheque 1   01/10/2026   AED 21,250.00
            Cheque 2   01/01/2027   AED 21,250.00
            Cheque 3   01/04/2027   AED 21,250.00
            Cheque 4   01/07/2027   AED 21,250.00
            Contract End Date: 30/09/2027
        """.trimIndent()
        val r = DocumentParser.parse(text, today)
        assertEquals(Category.RENT, r.category)
        assertEquals(4, r.schedule.size)
        assertEquals(LocalDate.of(2026, 10, 1) to 21250.0, r.schedule.first())
        assertEquals(LocalDate.of(2027, 7, 1), r.schedule.last().first)
        val fields = r.fields.toMap()
        assertEquals("0123456789", fields["Ejari No"])
        assertEquals("Apartment 1204, Marina Heights", fields["Property"])
        assertEquals("Al Noor Properties LLC", fields["Landlord"])
        assertEquals(85000.0, r.allAmounts.first().first, 0.001)
    }

    @Test fun noScheduleForSingleDate() {
        val r = DocumentParser.parse("Netflix\nAED 49.00\nNext billing date: 21/10/2026", today)
        assertTrue(r.schedule.isEmpty())
    }

    @Test fun quickAddBillWithRelativeWeekday() {
        val r = QuickAdd.parse("DEWA bill 450 dirhams due next Friday", today)
        assertEquals("DEWA bill", r.title)
        assertEquals(450.0, r.amount!!, 0.001)
        assertEquals(LocalDate.of(2026, 10, 2), r.date)
        assertEquals(Category.BILL, r.category)
    }

    @Test fun quickAddAbsoluteDate() {
        val r = QuickAdd.parse("Renew car registration on 12 March 2027", today)
        assertEquals(LocalDate.of(2027, 3, 12), r.date)
        assertEquals("Renew car registration", r.title)
        assertEquals(null, r.amount)
    }

    @Test fun quickAddRelativeDurations() {
        assertEquals(today.plusWeeks(2), QuickAdd.parse("Pay school fees in 2 weeks", today).date)
        assertEquals(today.plusDays(1), QuickAdd.parse("Gym 250 monthly tomorrow", today).date)
        assertEquals(250.0, QuickAdd.parse("Gym 250 monthly tomorrow", today).amount!!, 0.001)
        assertEquals(LocalDate.of(2026, 10, 25), QuickAdd.parse("Credit card on the 25th", today).date)
        assertEquals(LocalDate.of(2026, 9, 30), QuickAdd.parse("Salik top up end of month", today).date)
    }
}
