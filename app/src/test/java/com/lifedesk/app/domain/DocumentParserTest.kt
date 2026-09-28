package com.lifedesk.app.domain

import com.lifedesk.app.data.Category
import com.lifedesk.app.data.DateKind
import com.lifedesk.app.data.Recurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DocumentParserTest {
    private val today = LocalDate.of(2026, 9, 28)

    @Test fun carInsurancePolicy() {
        val text = """
            ABC Insurance Company LLC
            MOTOR INSURANCE POLICY SCHEDULE
            Policy No: MTR-2025-884211
            Vehicle: Toyota Camry 2022   Plate: Dubai K 12345
            Period of insurance: 10/10/2025 - 09/10/2026
            Total Premium: AED 2,850.00
            Customer care 800 4567
        """.trimIndent()
        val r = DocumentParser.parse(text, today)
        assertEquals(Category.INSURANCE, r.category)
        assertEquals("Car insurance", r.title)
        assertEquals("ABC Insurance Company", r.provider)
        assertEquals(LocalDate.of(2026, 10, 9), r.date)
        assertEquals(DateKind.RENEWAL, r.kind)
        assertEquals(2850.0, r.amount!!, 0.001)
        assertEquals("AED", r.currency)
        assertEquals("MTR-2025-884211", r.referenceNumber)
        assertEquals("Toyota Camry", r.asset)
        assertEquals(Recurrence.YEARLY, r.recurrence)
    }

    @Test fun creditCardStatementPrefersTotalOverMinimum() {
        val text = """
            Emirates NBD
            Credit Card Statement
            Statement Date: 15/09/2026
            Card ending 4421
            Minimum Amount Due AED 124.00
            Total Amount Due AED 1,240.00
            Payment Due Date: 01 Oct 2026
        """.trimIndent()
        val r = DocumentParser.parse(text, today)
        assertEquals(Category.CREDIT_CARD, r.category)
        assertEquals("Emirates NBD credit card", r.title)
        assertEquals(1240.0, r.amount!!, 0.001)
        assertEquals(LocalDate.of(2026, 10, 1), r.date)
        assertEquals(DateKind.PAYMENT_DUE, r.kind)
    }

    @Test fun passportIgnoresBirthDate() {
        val text = """
            PASSPORT
            Surname: KHAN
            Date of Birth 14 MAR 1990
            Date of Issue 18 MAY 2017
            Date of Expiry 18 MAY 2027
            Passport No. AB1234567
        """.trimIndent()
        val r = DocumentParser.parse(text, today)
        assertEquals(Category.ID_DOCUMENT, r.category)
        assertEquals("Passport", r.title)
        assertEquals(LocalDate.of(2027, 5, 18), r.date)
        assertEquals(DateKind.EXPIRY, r.kind)
        assertEquals("AB1234567", r.referenceNumber)
    }

    @Test fun emiratesIdNumber() {
        val text = "United Arab Emirates\nFederal Authority for Identity and Citizenship\nResident Identity Card\nID Number 784-1990-1234567-1\nExpiry Date 2028/03/02"
        val r = DocumentParser.parse(text, today)
        assertEquals(Category.ID_DOCUMENT, r.category)
        assertEquals("Emirates ID", r.title)
        assertEquals("784-1990-1234567-1", r.referenceNumber)
        assertEquals(LocalDate.of(2028, 3, 2), r.date)
    }

    @Test fun netflixSubscription() {
        val text = "Netflix\nYour monthly membership\nPremium plan  AED 49.00\nNext billing date: October 21, 2026"
        val r = DocumentParser.parse(text, today)
        assertEquals(Category.SUBSCRIPTION, r.category)
        assertEquals("Netflix", r.title)
        assertEquals(49.0, r.amount!!, 0.001)
        assertEquals(LocalDate.of(2026, 10, 21), r.date)
        assertEquals(Recurrence.MONTHLY, r.recurrence)
    }

    @Test fun dewaBill() {
        val text = """
            Dubai Electricity & Water Authority
            DEWA
            Account Number 2012345678
            Bill Date 05/09/2026
            Electricity consumption 1,245 kWh
            Total due AED 812.40
            Please pay by 25/09/2026
        """.trimIndent()
        val r = DocumentParser.parse(text, today)
        assertEquals(Category.BILL, r.category)
        assertEquals("DEWA", r.provider)
        assertEquals("DEWA electricity & water bill", r.title)
        assertEquals(812.4, r.amount!!, 0.001)
        assertEquals(LocalDate.of(2026, 9, 25), r.date)
        assertEquals("2012345678", r.referenceNumber)
    }

    @Test fun warrantyComputesEndFromPeriod() {
        val text = "SuperTech Electronics LLC\nTax Invoice\nInvoice Date: 12/11/2025\nSamsung Washing Machine WW90\n2 Years Warranty\nTotal AED 1,899"
        val r = DocumentParser.parse(text, today)
        assertEquals(Category.WARRANTY, r.category)
        assertEquals("Washing machine warranty", r.title)
        assertEquals(LocalDate.of(2027, 11, 12), r.date)
        assertEquals(DateKind.WARRANTY_END, r.kind)
    }

    @Test fun usStyleDateIsSwappedWhenDayFirstImpossible() {
        val r = DocumentParser.parse("Tenancy contract\nEjari\nEnd date 05/18/2027\nAnnual rent AED 85,000", today)
        assertEquals(Category.RENT, r.category)
        assertEquals(LocalDate.of(2027, 5, 18), r.date)
        assertEquals(85000.0, r.amount!!, 0.001)
    }

    @Test fun unknownTextStillReturnsSomething() {
        val r = DocumentParser.parse("hello world", today)
        assertEquals(Category.OTHER, r.category)
        assertNotNull(r.title)
        assertTrue(r.confidence < 0.5)
    }
}
