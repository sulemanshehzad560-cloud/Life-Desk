package com.lifedesk.app.data

import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

class LifeRepository(private val dao: LifeDao) {
    val items: Flow<List<LifeItem>> = dao.observeActive()
    val prices: Flow<List<PriceRecord>> = dao.observePrices()

    fun observeItem(id: Long): Flow<LifeItem?> = dao.observeItem(id)
    suspend fun item(id: Long): LifeItem? = dao.item(id)
    suspend fun activeItems(): List<LifeItem> = dao.activeItems()
    suspend fun pricesForItem(id: Long): List<PriceRecord> = dao.pricesForItem(id)

    /** Saves the item and records its amount in the price history when it changed. */
    suspend fun save(item: LifeItem, today: LocalDate = LocalDate.now()): Long {
        val id = dao.save(item)
        recordPrice(item.copy(id = id), today)
        return id
    }

    private suspend fun recordPrice(item: LifeItem, date: LocalDate) {
        val key = item.priceKey ?: return
        val amount = item.amount ?: return
        val latest = dao.pricesForItem(item.id).firstOrNull()
        if (latest == null || latest.amount != amount || latest.key != key) {
            dao.insertPrice(PriceRecord(key = key, itemId = item.id, amount = amount, currency = item.currency, date = date))
        }
    }

    /** Most recent amount seen for this provider (any item), for "18% higher than last time" checks. */
    suspend fun previousPrice(category: Category, provider: String?): PriceRecord? =
        priceKeyOf(category, provider)?.let { dao.pricesForKey(it).firstOrNull() }

    /** Paid / renewed: recurring items roll forward to the next date, one-off items are archived. */
    suspend fun complete(item: LifeItem, today: LocalDate = LocalDate.now()) {
        val due = item.dueDate
        if (item.recurrence != Recurrence.NONE && due != null) {
            var next = item.recurrence.next(due)
            while (!next.isAfter(today)) next = item.recurrence.next(next)
            dao.save(item.copy(dueDate = next, snoozedUntil = null))
        } else {
            dao.save(item.copy(archived = true, snoozedUntil = null))
        }
    }

    suspend fun snooze(item: LifeItem, days: Long, today: LocalDate = LocalDate.now()) =
        dao.save(item.copy(snoozedUntil = today.plusDays(days)))

    suspend fun markUsed(item: LifeItem, today: LocalDate = LocalDate.now()) = dao.save(item.copy(lastUsed = today))

    suspend fun delete(item: LifeItem) {
        item.imagePath?.let { File(it).delete() }
        dao.deletePricesFor(item.id)
        dao.delete(item)
    }

    suspend fun clearAll(docsDir: File) {
        dao.deleteAllPrices()
        dao.deleteAllItems()
        docsDir.listFiles()?.forEach { it.delete() }
    }

    // ---------- backup ----------

    suspend fun exportJson(): String {
        val items = JSONArray()
        dao.allItems().forEach { i ->
            items.put(JSONObject().apply {
                put("id", i.id); put("title", i.title); put("category", i.category.name); put("kind", i.kind.name)
                put("provider", i.provider); put("amount", i.amount); put("currency", i.currency)
                put("dueDate", i.dueDate?.toString()); put("recurrence", i.recurrence.name)
                put("referenceNumber", i.referenceNumber); put("asset", i.asset); put("notes", i.notes)
                put("contactPhone", i.contactPhone); put("contactEmail", i.contactEmail); put("rawText", i.rawText)
                put("lastUsed", i.lastUsed?.toString()); put("archived", i.archived); put("createdAt", i.createdAt)
            })
        }
        val prices = JSONArray()
        dao.allPrices().forEach { p ->
            prices.put(JSONObject().apply {
                put("key", p.key); put("itemId", p.itemId); put("amount", p.amount); put("currency", p.currency); put("date", p.date.toString())
            })
        }
        return JSONObject().put("app", "LifeDesk").put("version", 1).put("items", items).put("prices", prices).toString(2)
    }

    /** Replaces all data with a backup. Document photos are not part of the backup. Returns the number of items imported. */
    suspend fun importJson(json: String, docsDir: File): Int {
        val root = JSONObject(json)
        require(root.optString("app") == "LifeDesk") { "Not a LifeDesk backup" }
        clearAll(docsDir)
        fun JSONObject.str(k: String): String? = if (isNull(k) || !has(k)) null else getString(k)
        fun JSONObject.dbl(k: String): Double? = if (isNull(k) || !has(k)) null else getDouble(k)
        val items = root.getJSONArray("items")
        for (n in 0 until items.length()) {
            val o = items.getJSONObject(n)
            dao.save(
                LifeItem(
                    id = o.getLong("id"), title = o.getString("title"),
                    category = runCatching { Category.valueOf(o.getString("category")) }.getOrDefault(Category.OTHER),
                    kind = runCatching { DateKind.valueOf(o.getString("kind")) }.getOrDefault(DateKind.DEADLINE),
                    provider = o.str("provider"), amount = o.dbl("amount"), currency = o.str("currency") ?: "AED",
                    dueDate = o.str("dueDate")?.let(LocalDate::parse),
                    recurrence = runCatching { Recurrence.valueOf(o.getString("recurrence")) }.getOrDefault(Recurrence.NONE),
                    referenceNumber = o.str("referenceNumber"), asset = o.str("asset"), notes = o.str("notes"),
                    contactPhone = o.str("contactPhone"), contactEmail = o.str("contactEmail"), rawText = o.str("rawText"),
                    lastUsed = o.str("lastUsed")?.let(LocalDate::parse), archived = o.optBoolean("archived"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                )
            )
        }
        val prices = root.optJSONArray("prices") ?: JSONArray()
        for (n in 0 until prices.length()) {
            val o = prices.getJSONObject(n)
            dao.insertPrice(PriceRecord(key = o.getString("key"), itemId = o.getLong("itemId"), amount = o.getDouble("amount"),
                currency = o.getString("currency"), date = LocalDate.parse(o.getString("date"))))
        }
        return items.length()
    }

    // ---------- demo ----------

    suspend fun loadSampleData(currency: String, today: LocalDate = LocalDate.now()) {
        suspend fun add(item: LifeItem, history: List<Pair<Long, Double>> = emptyList()): Long {
            val id = dao.save(item)
            val key = item.copy(id = id).priceKey
            if (key != null) {
                history.forEach { (daysAgo, amt) -> dao.insertPrice(PriceRecord(key = key, itemId = id, amount = amt, currency = currency, date = today.minusDays(daysAgo))) }
                item.amount?.let { dao.insertPrice(PriceRecord(key = key, itemId = id, amount = it, currency = currency, date = today)) }
            }
            return id
        }
        add(LifeItem(title = "Car insurance", category = Category.INSURANCE, provider = "ABC Insurance", amount = 2850.0, currency = currency,
            dueDate = today.plusDays(12), recurrence = Recurrence.YEARLY, referenceNumber = "MTR-2025-884211", asset = "Toyota Camry",
            contactPhone = "800 4567"), listOf(365L to 2640.0))
        add(LifeItem(title = "Emirates NBD credit card", category = Category.CREDIT_CARD, provider = "Emirates NBD", amount = 1240.0,
            currency = currency, dueDate = today.plusDays(3), recurrence = Recurrence.MONTHLY))
        add(LifeItem(title = "Passport", category = Category.ID_DOCUMENT, dueDate = today.plusMonths(7), referenceNumber = "AB1234567"))
        add(LifeItem(title = "Emirates ID", category = Category.ID_DOCUMENT, dueDate = today.plusMonths(14), referenceNumber = "784-1990-1234567-1"))
        add(LifeItem(title = "Vehicle registration", category = Category.VEHICLE, provider = "RTA", amount = 420.0, currency = currency,
            dueDate = today.plusDays(61), recurrence = Recurrence.YEARLY, asset = "Toyota Camry"))
        add(LifeItem(title = "Netflix", category = Category.SUBSCRIPTION, provider = "Netflix", amount = 49.0, currency = currency,
            dueDate = today.plusDays(9), recurrence = Recurrence.MONTHLY, lastUsed = today.minusDays(21)), listOf(35L to 39.0))
        add(LifeItem(title = "Adobe", category = Category.SUBSCRIPTION, provider = "Adobe", amount = 86.0, currency = currency,
            dueDate = today.plusDays(17), recurrence = Recurrence.MONTHLY, lastUsed = today.minusDays(4)))
        add(LifeItem(title = "Gym membership", category = Category.SUBSCRIPTION, provider = "GymNation", amount = 250.0, currency = currency,
            dueDate = today.plusDays(5), recurrence = Recurrence.MONTHLY, lastUsed = today.minusDays(47)))
        add(LifeItem(title = "Cloud storage", category = Category.SUBSCRIPTION, provider = "Google One", amount = 39.0, currency = currency,
            dueDate = today.plusDays(23), recurrence = Recurrence.MONTHLY, lastUsed = today.minusDays(3)))
        add(LifeItem(title = "Streaming bundle", category = Category.SUBSCRIPTION, provider = "OSN+", amount = 35.0, currency = currency,
            dueDate = today.plusDays(14), recurrence = Recurrence.MONTHLY, lastUsed = today.minusDays(64)))
        add(LifeItem(title = "DEWA electricity & water bill", category = Category.BILL, provider = "DEWA", amount = 812.4, currency = currency,
            dueDate = today.plusDays(8), recurrence = Recurrence.MONTHLY, referenceNumber = "2012345678"), listOf(30L to 690.0))
        add(LifeItem(title = "Washing machine warranty", category = Category.WARRANTY, provider = "SuperTech Electronics",
            dueDate = today.plusDays(43), asset = "Washing machine"))
    }
}
