package com.lifedesk.app.domain

import com.lifedesk.app.data.Category
import com.lifedesk.app.data.LifeItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class VoiceCommandsTest {
    @Test fun parsesCommands() {
        assertEquals(VoiceCommand.Add("remind me to pay DEWA 450 next Friday"), VoiceCommands.parse("Hey LifeDesk, remind me to pay DEWA 450 next Friday"))
        assertEquals(VoiceCommand.Complete("credit card"), VoiceCommands.parse("mark credit card as paid"))
        assertEquals(VoiceCommand.Complete("DEWA bill"), VoiceCommands.parse("life desk I paid the DEWA bill"))
        assertEquals(VoiceCommand.Snooze("car insurance", 3), VoiceCommands.parse("snooze car insurance"))
        assertEquals(VoiceCommand.Snooze("netflix", 7), VoiceCommands.parse("snooze my netflix for a week"))
        assertEquals(VoiceCommand.Snooze("gym", 2), VoiceCommands.parse("postpone gym by 2 days"))
        assertEquals(VoiceCommand.Ask("when does my passport expire?"), VoiceCommands.parse("ok lifedesk when does my passport expire?"))
        assertEquals(VoiceCommand.Briefing, VoiceCommands.parse("what's my briefing"))
        assertEquals(VoiceCommand.Briefing, VoiceCommands.parse("what's due this week"))
    }

    @Test fun findsItemByPartialName() {
        val today = LocalDate.of(2026, 9, 28)
        val items = listOf(
            LifeItem(id = 1, title = "Emirates NBD credit card", category = Category.CREDIT_CARD, dueDate = today.plusDays(3)),
            LifeItem(id = 2, title = "Car insurance", category = Category.INSURANCE, provider = "AXA", dueDate = today.plusDays(12)),
            LifeItem(id = 3, title = "DEWA electricity & water bill", category = Category.BILL, provider = "DEWA", dueDate = today.plusDays(5)),
        )
        assertEquals(1L, VoiceCommands.findItem("credit card", items)?.id)
        assertEquals(3L, VoiceCommands.findItem("DEWA bill", items)?.id)
        assertEquals(2L, VoiceCommands.findItem("axa", items)?.id)
        assertEquals(null, VoiceCommands.findItem("passport", items))
    }
}
