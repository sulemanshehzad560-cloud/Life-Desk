package com.lifedesk.app.domain

import com.lifedesk.app.data.Category
import com.lifedesk.app.data.DateKind
import com.lifedesk.app.data.Recurrence
import java.time.LocalDate

/** Everything LifeDesk could work out from a photo of a document. All fields are suggestions the user can edit. */
data class ParsedDocument(
    val category: Category,
    val kind: DateKind,
    val title: String,
    val provider: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val date: LocalDate? = null,
    val recurrence: Recurrence = Recurrence.NONE,
    val referenceNumber: String? = null,
    val asset: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val allDates: List<LocalDate> = emptyList(),
    /** 0..1 — how many of the key fields (type, date, amount/provider) were found. */
    val confidence: Double = 0.0,
)

/**
 * Turns messy OCR text into a structured [ParsedDocument] using keyword scoring and regexes.
 * Pure Kotlin (no Android types) so it is fully unit-testable. Dates are read day-first (UAE/UK style)
 * unless that is impossible.
 */
object DocumentParser {

    private fun kw(vararg words: String) = words.map { w -> Regex("(?<![\\p{L}0-9])" + Regex.escape(w) + "(?![\\p{L}0-9])") }

    private val categoryKeywords: Map<Category, List<Pair<Regex, Int>>> = mapOf(
        Category.ID_DOCUMENT to weighted(3, "passport", "emirates id", "identity card", "residence visa", "residence permit", "driving licence", "driving license", "driver's license", "federal authority for identity") +
            weighted(1, "visa", "nationality", "date of birth", "icp", "gdrfa", "id card", "place of birth"),
        Category.INSURANCE to weighted(3, "insurance", "takaful", "policy schedule", "policyholder", "sum insured") +
            weighted(1, "policy", "premium", "insured", "cover", "coverage", "insurer"),
        Category.VEHICLE to weighted(3, "vehicle registration", "mulkiya", "registration card", "vehicle licence", "salik") +
            weighted(1, "vehicle", "plate", "chassis", "rta", "traffic", "odometer", "car service", "engine no"),
        Category.FINE to weighted(3, "traffic fine", "parking fine", "violation", "penalty notice", "parking ticket") + weighted(1, "fine", "penalty"),
        Category.BILL to weighted(3, "dewa", "addc", "aadc", "sewa", "fewa", "etisalat", "e&", "electricity", "utility bill") +
            weighted(2, "du", "kwh", "broadband", "postpaid", "internet", "water", "telecom", "virgin mobile", "empower", "district cooling") +
            weighted(1, "bill", "billing period", "account number", "meter", "consumption", "mobile"),
        Category.CREDIT_CARD to weighted(3, "credit card", "card statement", "minimum amount due", "minimum payment due", "minimum due") +
            weighted(1, "statement balance", "card ending", "card number", "credit limit", "available limit"),
        Category.LOAN to weighted(3, "loan", "mortgage", "instalment", "installment", "emi") + weighted(1, "finance", "outstanding principal"),
        Category.SUBSCRIPTION to weighted(3, "subscription", "netflix", "spotify", "adobe", "youtube premium", "icloud", "google one", "amazon prime", "osn", "shahid", "disney", "chatgpt", "anghami", "starzplay", "microsoft 365", "membership") +
            weighted(2, "renews automatically", "auto-renew", "next billing date", "your plan", "gym") + weighted(1, "plan", "monthly"),
        Category.RENT to weighted(3, "tenancy", "ejari", "lease agreement", "landlord", "tenant") + weighted(1, "rent", "cheque", "annual rent"),
        Category.WARRANTY to weighted(3, "warranty", "guarantee card", "extended warranty") + weighted(1, "serial number", "model no", "purchase date", "serial no"),
        Category.SCHOOL to weighted(3, "school fee", "school fees", "tuition", "term fee") + weighted(1, "school", "academic year", "student", "term"),
        Category.MEDICAL to weighted(3, "appointment", "clinic", "hospital") + weighted(1, "doctor", "dr.", "patient", "medical", "dental"),
        Category.TRAVEL to weighted(3, "boarding pass", "booking reference", "pnr", "e-ticket", "itinerary") + weighted(1, "flight", "departure", "hotel", "check-in", "airline", "arrival"),
    )

    private fun weighted(weight: Int, vararg words: String) = kw(*words).map { it to weight }

    private data class Provider(val name: String, val patterns: List<Regex>, val category: Category? = null)

    private fun p(name: String, category: Category?, vararg words: String) = Provider(name, kw(*words), category)

    private val providers = listOf(
        p("Netflix", Category.SUBSCRIPTION, "netflix"), p("Spotify", Category.SUBSCRIPTION, "spotify"),
        p("Adobe", Category.SUBSCRIPTION, "adobe"), p("YouTube Premium", Category.SUBSCRIPTION, "youtube premium", "youtube"),
        p("Apple", Category.SUBSCRIPTION, "icloud", "apple.com/bill", "apple music", "apple one"),
        p("Google One", Category.SUBSCRIPTION, "google one"), p("Amazon Prime", Category.SUBSCRIPTION, "amazon prime", "prime video"),
        p("OSN+", Category.SUBSCRIPTION, "osn"), p("Shahid", Category.SUBSCRIPTION, "shahid"), p("Disney+", Category.SUBSCRIPTION, "disney"),
        p("ChatGPT", Category.SUBSCRIPTION, "chatgpt", "openai"), p("Anghami", Category.SUBSCRIPTION, "anghami"),
        p("STARZPLAY", Category.SUBSCRIPTION, "starzplay"), p("Microsoft 365", Category.SUBSCRIPTION, "microsoft 365", "office 365"),
        p("Fitness First", Category.SUBSCRIPTION, "fitness first"), p("GymNation", Category.SUBSCRIPTION, "gymnation"),
        p("DEWA", Category.BILL, "dewa", "dubai electricity"), p("ADDC", Category.BILL, "addc", "abu dhabi distribution"),
        p("AADC", Category.BILL, "aadc"), p("SEWA", Category.BILL, "sewa"), p("FEWA", Category.BILL, "fewa", "etihad water"),
        p("Etisalat by e&", Category.BILL, "etisalat", "e&"), p("du", Category.BILL, "du", "emirates integrated telecommunications"),
        p("Virgin Mobile", Category.BILL, "virgin mobile"), p("Empower", Category.BILL, "empower"),
        p("Salik", Category.VEHICLE, "salik"), p("RTA", Category.VEHICLE, "rta", "roads and transport authority"),
        p("AXA", Category.INSURANCE, "axa"), p("Sukoon", Category.INSURANCE, "sukoon", "oman insurance"),
        p("Orient Insurance", Category.INSURANCE, "orient insurance"), p("ADNIC", Category.INSURANCE, "adnic", "abu dhabi national insurance"),
        p("GIG Gulf", Category.INSURANCE, "gig gulf", "gig"), p("Tokio Marine", Category.INSURANCE, "tokio marine"),
        p("RSA", Category.INSURANCE, "rsa"), p("Daman", Category.INSURANCE, "daman"), p("Watania", Category.INSURANCE, "watania"),
        p("Dubai Insurance", Category.INSURANCE, "dubai insurance"), p("Al Wathba", Category.INSURANCE, "al wathba"),
        p("Emirates NBD", null, "emirates nbd", "enbd"), p("ADCB", null, "adcb", "abu dhabi commercial bank"),
        p("FAB", null, "first abu dhabi bank", "fab"), p("Mashreq", null, "mashreq"), p("Dubai Islamic Bank", null, "dubai islamic bank", "dib"),
        p("RAKBANK", null, "rakbank", "rak bank"), p("HSBC", null, "hsbc"), p("Citibank", null, "citibank", "citi"),
        p("CBD", null, "commercial bank of dubai", "cbd"), p("ADIB", null, "adib", "abu dhabi islamic bank"),
        p("flydubai", Category.TRAVEL, "flydubai"), p("Etihad Airways", Category.TRAVEL, "etihad airways"),
        p("Air Arabia", Category.TRAVEL, "air arabia"), p("Emirates", Category.TRAVEL, "emirates airline", "emirates.com"),
        p("ICP", Category.ID_DOCUMENT, "federal authority for identity", "icp"), p("GDRFA", Category.ID_DOCUMENT, "gdrfa"),
    )

    private val carMakes = listOf("Toyota", "Nissan", "Honda", "Lexus", "Mitsubishi", "Hyundai", "Kia", "BMW", "Mercedes", "Audi", "Ford",
        "Chevrolet", "Tesla", "Mazda", "Volkswagen", "Land Rover", "Range Rover", "Jeep", "GMC", "Porsche", "Infiniti", "Suzuki", "Renault", "Peugeot", "MG", "Geely")
    private val products = listOf("washing machine", "refrigerator", "fridge", "laptop", "iphone", "ipad", "macbook", "television", "tv",
        "air conditioner", "dishwasher", "oven", "microwave", "vacuum", "phone", "camera", "dryer", "water heater", "smart watch")

    private val months = mapOf("jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6, "jul" to 7, "aug" to 8,
        "sep" to 9, "sept" to 9, "oct" to 10, "nov" to 11, "dec" to 12)
    private const val MONTH = "(jan|feb|mar|apr|may|jun|jul|aug|sept|sep|oct|nov|dec)[a-z]*\\.?"
    private val isoDate = Regex("(?<!\\d)(\\d{4})[/\\-.](\\d{1,2})[/\\-.](\\d{1,2})(?!\\d)")
    private val numericDate = Regex("(?<!\\d)(\\d{1,2})[/\\-.](\\d{1,2})[/\\-.](\\d{4}|\\d{2})(?!\\d)")
    private val dayMonthName = Regex("(?<!\\d)(\\d{1,2})(?:st|nd|rd|th)?[\\s\\-/.]*$MONTH[\\s\\-/.,]*(\\d{4}|\\d{2})(?!\\d)", RegexOption.IGNORE_CASE)
    private val monthNameDay = Regex("(?<![a-z])$MONTH\\s+(\\d{1,2})(?:st|nd|rd|th)?,?\\s+(\\d{4})(?!\\d)", RegexOption.IGNORE_CASE)

    private enum class DateLabel { END, DUE, APPOINTMENT, START, BIRTH, NONE }

    private val labelRules: List<Pair<DateLabel, Regex>> = listOf(
        DateLabel.BIRTH to Regex("birth|\\bdob\\b", RegexOption.IGNORE_CASE),
        DateLabel.DUE to Regex("due|pay by|payment date|last date|next billing|next payment|billing date|renews on|will renew|charged on", RegexOption.IGNORE_CASE),
        DateLabel.END to Regex("expir|valid until|valid till|valid thru|valid to|\\bexp\\b|exp\\.|end date|period to|cover(age)? end|policy end|renewal|renew by|warranty (until|till|end|valid)|\\bto\\b|until|till", RegexOption.IGNORE_CASE),
        DateLabel.APPOINTMENT to Regex("appointment|departure|depart|check-in|check in|flight date|travel date|visit date|scheduled", RegexOption.IGNORE_CASE),
        DateLabel.START to Regex("issue|issued|invoice date|statement date|bill date|date of purchase|purchase date|purchased|from|start|commenc|registration date|inception", RegexOption.IGNORE_CASE),
    )

    private const val CUR = "(AED|DHS?|د\\.إ|USD|US\\$|\\$|SAR|SR|INR|RS\\.?|₹|GBP|£|EUR|€|QAR|OMR|KWD|BHD|PKR)"
    private const val NUM = "(\\d{1,3}(?:,\\d{3})+(?:\\.\\d{1,2})?|\\d+(?:\\.\\d{1,2})?)"
    private val curBefore = Regex("(?<![A-Za-z])$CUR\\s*$NUM", RegexOption.IGNORE_CASE)
    private val curAfter = Regex("$NUM\\s*$CUR(?![a-z])", RegexOption.IGNORE_CASE)
    private val bareAmount = Regex("(?<![\\d/.\\-])$NUM(?![\\d/\\-])")
    private val strongAmountLabel = Regex("total amount due|amount due|balance due|total due|grand total|net payable|amount payable|total payable|total premium|net premium|premium|total amount|new balance|statement balance|amount to pay|total", RegexOption.IGNORE_CASE)
    private val weakAmountLabel = Regex("amount|price|fee|charge|paid|cost|rent|payable", RegexOption.IGNORE_CASE)
    private val negativeAmountLabel = Regex("minimum|min\\.|previous|last payment|credit limit|available|vat no|trn|limit|kwh|consumption|discount", RegexOption.IGNORE_CASE)

    private val referenceRegex = Regex(
        "(policy|invoice|account|acct|contract|reference|ref|booking|pnr|passport|licen[cs]e|plate|certificate|membership|member|customer|card|document|serial|traffic file|id)\\s*(?:no\\.?|number|num|#|id)?\\s*[:#.\\-]?\\s*([A-Z0-9][A-Z0-9\\-/]{3,})",
        RegexOption.IGNORE_CASE,
    )
    private val emiratesId = Regex("784[\\-\\s]?\\d{4}[\\-\\s]?\\d{7}[\\-\\s]?\\d")
    private val phoneRegex = Regex("(\\+?971[\\s\\-]?\\d{1,2}[\\s\\-]?\\d{3}[\\s\\-]?\\d{4}|(?<!\\d)800[\\s\\-]?\\d{3,7}(?!\\d)|(?<!\\d)0\\d{1,2}[\\s\\-]\\d{3}[\\s\\-]?\\d{4}(?!\\d)|(?<!\\d)05\\d{8}(?!\\d))")
    private val emailRegex = Regex("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}")
    private val warrantyPeriod = Regex("(\\d{1,2})\\s*[- ]?(years?|yrs?|months?)\\s*(?:warranty|guarantee)|(?:warranty|guarantee)[^\\n\\d]{0,20}(\\d{1,2})\\s*(years?|yrs?|months?)", RegexOption.IGNORE_CASE)
    private val companyLine = Regex("\\b(llc|l\\.l\\.c|pjsc|p\\.j\\.s\\.c|fze|fzco|fz-llc|company|co\\.|insurance|bank|telecom|school|academy|clinic|hospital|motors|group|ltd|limited|inc)\\b", RegexOption.IGNORE_CASE)

    fun parse(rawText: String, today: LocalDate = LocalDate.now(), defaultCurrency: String = "AED"): ParsedDocument {
        val text = rawText.replace('\r', '\n')
        val lower = text.lowercase()
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val provider = detectProvider(lower, lines)
        val category = detectCategory(lower, provider)
        val subtype = detectSubtype(lower, category)
        val asset = detectAsset(text, lower, category)

        val dated = findDates(lines)
        val (date, label) = pickDate(dated, category, today, lower)
        val kind = when (label) {
            DateLabel.DUE -> if (category.defaultKind == DateKind.RENEWAL && category != Category.SUBSCRIPTION) DateKind.RENEWAL else DateKind.PAYMENT_DUE
            DateLabel.END -> if (category.defaultKind in setOf(DateKind.RENEWAL, DateKind.WARRANTY_END, DateKind.EXPIRY)) category.defaultKind else DateKind.EXPIRY
            DateLabel.APPOINTMENT -> DateKind.APPOINTMENT
            else -> category.defaultKind
        }

        val amount = findAmount(lines, defaultCurrency)
        val recurrence = detectRecurrence(lower, category)
        val reference = findReference(text, category)
        val phone = phoneRegex.find(text)?.value?.trim()
        val email = emailRegex.find(text)?.value
        val title = buildTitle(category, subtype, provider, asset)

        var score = 0.0
        if (category != Category.OTHER) score += 0.35
        if (date != null) score += 0.35
        if (amount != null || provider != null) score += 0.2
        if (reference != null) score += 0.1

        return ParsedDocument(
            category = category,
            kind = kind,
            title = title,
            provider = provider,
            amount = amount?.first,
            currency = amount?.second,
            date = date,
            recurrence = recurrence,
            referenceNumber = reference,
            asset = asset,
            phone = phone,
            email = email,
            allDates = dated.map { it.date }.distinct().sorted(),
            confidence = score,
        )
    }

    // ---------- category / provider ----------

    private fun detectProvider(lower: String, lines: List<String>): String? {
        val hits = providers.mapNotNull { pr ->
            val matches = pr.patterns.flatMap { it.findAll(lower).toList() }
            if (matches.isEmpty()) null else Triple(pr, matches.size, matches.minOf { it.range.first })
        }
        hits.maxWithOrNull(compareBy<Triple<Provider, Int, Int>> { it.second }.thenByDescending { it.third })?.let { return it.first.name }
        // Fall back to a company-looking line near the top of the document ("ABC Insurance Company LLC").
        return lines.take(12).firstOrNull { companyLine.containsMatchIn(it) && it.length in 3..60 && !it.contains(':') && it.count(Char::isDigit) < 3 }
            ?.let(::cleanCompany)
    }

    private fun cleanCompany(line: String): String =
        line.replace(Regex("\\s*\\b(l\\.?l\\.?c|p\\.?j\\.?s\\.?c|fze|fzco|fz-llc|ltd|limited|inc)\\b\\.?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ").trim(' ', ',', '.', '-')
            .split(' ').joinToString(" ") { w -> if (w.length > 3 && w == w.uppercase()) w.lowercase().replaceFirstChar(Char::uppercase) else w }

    private fun detectCategory(lower: String, provider: String?): Category {
        val scores = categoryKeywords.mapValues { (_, rules) -> rules.sumOf { (re, w) -> re.findAll(lower).count() * w } }.toMutableMap()
        providers.firstOrNull { it.name == provider }?.category?.let { scores[it] = (scores[it] ?: 0) + 3 }
        val ins = scores[Category.INSURANCE] ?: 0
        val fine = scores[Category.FINE] ?: 0
        // "Motor insurance" mentions the vehicle a lot but is an insurance policy; same for fines.
        if (ins >= 3) scores[Category.VEHICLE] = 0
        if (fine >= 3) scores[Category.VEHICLE] = 0
        if ((scores[Category.CREDIT_CARD] ?: 0) >= 3) scores[Category.BILL] = minOf(scores[Category.BILL] ?: 0, 1)
        if ((scores[Category.ID_DOCUMENT] ?: 0) >= 3) scores[Category.TRAVEL] = minOf(scores[Category.TRAVEL] ?: 0, 1)
        val best = scores.maxByOrNull { it.value }
        return if (best == null || best.value < 2) Category.OTHER else best.key
    }

    private fun has(lower: String, vararg words: String) = kw(*words).any { it.containsMatchIn(lower) }

    private fun detectSubtype(lower: String, category: Category): String? = when (category) {
        Category.ID_DOCUMENT -> when {
            has(lower, "passport") -> "Passport"
            has(lower, "emirates id", "identity card", "federal authority for identity") -> "Emirates ID"
            has(lower, "driving licence", "driving license", "driver's license") -> "Driving licence"
            has(lower, "visa", "residence permit") -> "Residence visa"
            else -> null
        }
        Category.INSURANCE -> when {
            has(lower, "motor", "vehicle", "car", "comprehensive motor", "third party liability", "plate", "chassis") -> "Car insurance"
            has(lower, "health", "medical") -> "Health insurance"
            has(lower, "travel") -> "Travel insurance"
            has(lower, "home", "contents") -> "Home insurance"
            has(lower, "life") -> "Life insurance"
            else -> "Insurance"
        }
        Category.VEHICLE -> when {
            has(lower, "salik") -> "Salik"
            has(lower, "service", "odometer", "oil change") -> "Car service"
            else -> "Vehicle registration"
        }
        Category.BILL -> when {
            has(lower, "electricity", "kwh", "water", "dewa", "addc", "sewa", "fewa") -> "Electricity & water bill"
            has(lower, "internet", "broadband", "fibre", "fiber", "home wireless") -> "Internet bill"
            has(lower, "mobile", "postpaid") -> "Mobile bill"
            has(lower, "district cooling", "empower", "chiller") -> "Cooling bill"
            else -> "Bill"
        }
        Category.CREDIT_CARD -> "Credit card"
        Category.LOAN -> if (has(lower, "mortgage")) "Mortgage" else if (has(lower, "car loan", "auto loan")) "Car loan" else "Loan"
        Category.RENT -> if (has(lower, "tenancy", "ejari", "lease")) "Tenancy contract" else "Rent"
        Category.SCHOOL -> "School fees"
        Category.MEDICAL -> if (has(lower, "dental", "dentist")) "Dentist appointment" else "Medical appointment"
        Category.TRAVEL -> if (has(lower, "hotel")) "Hotel booking" else "Flight"
        Category.FINE -> if (has(lower, "parking")) "Parking fine" else "Traffic fine"
        Category.WARRANTY, Category.SUBSCRIPTION, Category.OTHER -> null
    }

    private fun detectAsset(text: String, lower: String, category: Category): String? {
        if (category in setOf(Category.INSURANCE, Category.VEHICLE, Category.FINE, Category.LOAN)) {
            carMakes.firstOrNull { kw(it.lowercase()).first().containsMatchIn(lower) }?.let { make ->
                val model = Regex(Regex.escape(make) + "\\s+([A-Z][A-Za-z0-9\\-]{1,15})", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)
                return if (model != null && model.lowercase() !in setOf("model", "make", "year", "insurance", "motors")) "$make ${model.replaceFirstChar(Char::uppercase)}" else make
            }
        }
        if (category == Category.WARRANTY) {
            products.firstOrNull { kw(it).first().containsMatchIn(lower) }?.let { return it.replaceFirstChar(Char::uppercase).replace("Tv", "TV") }
        }
        return null
    }

    private fun buildTitle(category: Category, subtype: String?, provider: String?, asset: String?): String = when (category) {
        Category.SUBSCRIPTION -> provider ?: "Subscription"
        Category.BILL -> if (provider != null) "$provider ${(subtype ?: "bill").lowercase()}" else subtype ?: "Bill"
        Category.CREDIT_CARD, Category.LOAN -> if (provider != null) "$provider ${(subtype ?: category.label).lowercase()}" else subtype ?: category.label
        Category.WARRANTY -> if (asset != null) "$asset warranty" else if (provider != null) "$provider warranty" else "Warranty"
        Category.INSURANCE -> subtype ?: "Insurance"
        Category.OTHER -> provider ?: "Document"
        else -> subtype ?: provider ?: category.label
    }

    private fun detectRecurrence(lower: String, category: Category): Recurrence = when {
        has(lower, "monthly", "per month", "/month", "/mo", "every month", "month-to-month") -> Recurrence.MONTHLY
        has(lower, "annual", "annually", "yearly", "per year", "/year", "per annum", "12 months") && category != Category.WARRANTY -> Recurrence.YEARLY
        has(lower, "quarterly", "every 3 months") -> Recurrence.QUARTERLY
        has(lower, "weekly", "per week") -> Recurrence.WEEKLY
        else -> category.defaultRecurrence
    }

    // ---------- dates ----------

    private data class Dated(val date: LocalDate, val label: DateLabel, val line: Int)

    private fun safeDate(y: Int, m: Int, d: Int): LocalDate? = try {
        val year = if (y < 100) 2000 + y else y
        if (year !in 1900..2100) null else LocalDate.of(year, m, d)
    } catch (_: Exception) { null }

    private fun datesInLine(line: String): List<LocalDate> {
        val found = mutableListOf<Pair<Int, LocalDate>>()
        val taken = mutableListOf<IntRange>()
        fun add(m: MatchResult, date: LocalDate?) {
            if (date == null || taken.any { it.first <= m.range.last && m.range.first <= it.last }) return
            taken += m.range; found += m.range.first to date
        }
        isoDate.findAll(line).forEach { m -> val (y, mo, d) = m.destructured; add(m, safeDate(y.toInt(), mo.toInt(), d.toInt())) }
        dayMonthName.findAll(line).forEach { m ->
            val (d, mon, y) = m.destructured
            add(m, months[mon.lowercase().take(4).let { if (it == "sept") it else it.take(3) }]?.let { safeDate(y.toInt(), it, d.toInt()) })
        }
        monthNameDay.findAll(line).forEach { m ->
            val (mon, d, y) = m.destructured
            add(m, months[mon.lowercase().take(4).let { if (it == "sept") it else it.take(3) }]?.let { safeDate(y.toInt(), it, d.toInt()) })
        }
        numericDate.findAll(line).forEach { m ->
            val (a, b, y) = m.destructured
            val first = a.toInt(); val second = b.toInt()
            // Day-first by default; swap only when the day-first reading is impossible (e.g. 05/18/2027).
            val date = if (second > 12 && first <= 12) safeDate(y.toInt(), first, second) else safeDate(y.toInt(), second, first)
            add(m, date)
        }
        return found.sortedBy { it.first }.map { it.second }
    }

    private fun labelOf(text: String): DateLabel =
        labelRules.firstOrNull { (_, re) -> re.containsMatchIn(text) }?.first ?: DateLabel.NONE

    private fun findDates(lines: List<String>): List<Dated> {
        val out = mutableListOf<Dated>()
        lines.forEachIndexed { i, line ->
            val dates = datesInLine(line)
            if (dates.isEmpty()) return@forEachIndexed
            // Label text is whatever precedes the first date on the line, or the previous line when the date stands alone.
            val prefix = line.substringBefore(Regex("\\d").find(line)?.value ?: "").ifBlank { null }
            val ownLabel = labelOf(line.replace(Regex("\\d"), " "))
            val label = when {
                ownLabel != DateLabel.NONE -> ownLabel
                i > 0 -> labelOf(lines[i - 1].replace(Regex("\\d"), " "))
                else -> DateLabel.NONE
            }
            if (dates.size >= 2 && (prefix == null || label == DateLabel.NONE || label == DateLabel.START || label == DateLabel.END)) {
                // "01/10/2026 - 30/09/2027" or "From 01/10/2026 To 30/09/2027": the later one is the end of the period.
                val sorted = dates.sorted()
                sorted.dropLast(1).forEach { out += Dated(it, DateLabel.START, i) }
                out += Dated(sorted.last(), DateLabel.END, i)
            } else {
                dates.forEach { out += Dated(it, label, i) }
            }
        }
        return out
    }

    private fun pickDate(dated: List<Dated>, category: Category, today: LocalDate, lower: String): Pair<LocalDate?, DateLabel> {
        val candidates = dated.filter { it.label != DateLabel.BIRTH }
        if (category == Category.WARRANTY) {
            warrantyPeriod.find(lower)?.let { m ->
                val n = (m.groupValues[1].ifEmpty { m.groupValues[3] }).toIntOrNull()
                val unit = m.groupValues[2].ifEmpty { m.groupValues[4] }.lowercase()
                val purchase = candidates.firstOrNull { it.label == DateLabel.START }?.date ?: candidates.minOfOrNull { it.date }
                if (n != null && purchase != null) {
                    return (if (unit.startsWith("m")) purchase.plusMonths(n.toLong()) else purchase.plusYears(n.toLong())) to DateLabel.END
                }
            }
        }
        val preferred = when (category.defaultKind) {
            DateKind.PAYMENT_DUE -> listOf(DateLabel.DUE, DateLabel.END, DateLabel.APPOINTMENT)
            DateKind.APPOINTMENT -> listOf(DateLabel.APPOINTMENT, DateLabel.DUE, DateLabel.END)
            else -> listOf(DateLabel.END, DateLabel.DUE, DateLabel.APPOINTMENT)
        }
        for (label in preferred) {
            val ofLabel = candidates.filter { it.label == label }
            if (ofLabel.isNotEmpty()) return (ofLabel.filter { !it.date.isBefore(today) }.minByOrNull { it.date } ?: ofLabel.maxBy { it.date }).date to label
        }
        val unlabeled = candidates.filter { it.label == DateLabel.NONE }
        val future = unlabeled.filter { !it.date.isBefore(today) }
        if (future.isNotEmpty()) return future.maxBy { it.date }.date to DateLabel.NONE
        return (unlabeled.maxByOrNull { it.date }?.date) to DateLabel.NONE
    }

    // ---------- amounts / references ----------

    private fun normaliseCurrency(raw: String, default: String): String = when (raw.uppercase().trimEnd('.')) {
        "AED", "DHS", "DH", "د.إ" -> "AED"
        "$", "US$", "USD" -> "USD"
        "SAR", "SR" -> "SAR"
        "INR", "RS", "₹" -> "INR"
        "GBP", "£" -> "GBP"
        "EUR", "€" -> "EUR"
        "QAR", "OMR", "KWD", "BHD", "PKR" -> raw.uppercase()
        else -> default
    }

    private fun findAmount(lines: List<String>, defaultCurrency: String): Pair<Double, String>? {
        data class Cand(val value: Double, val currency: String, val score: Int)
        val cands = mutableListOf<Cand>()
        lines.forEachIndexed { i, line ->
            var ctx = line
            if (!strongAmountLabel.containsMatchIn(line) && i > 0 && strongAmountLabel.containsMatchIn(lines[i - 1]) && !lines[i - 1].any(Char::isDigit)) ctx = lines[i - 1] + " " + line
            var base = 0
            if (strongAmountLabel.containsMatchIn(ctx)) base += 10
            else if (weakAmountLabel.containsMatchIn(ctx)) base += 4
            if (negativeAmountLabel.containsMatchIn(ctx)) base -= 8
            var withCurrency = false
            for (re in listOf(curBefore, curAfter)) {
                re.findAll(line).forEach { m ->
                    val (cur, num) = if (re === curBefore) m.groupValues[1] to m.groupValues[2] else m.groupValues[2] to m.groupValues[1]
                    num.replace(",", "").toDoubleOrNull()?.takeIf { it > 0 }?.let {
                        withCurrency = true
                        cands += Cand(it, normaliseCurrency(cur, defaultCurrency), base + 5)
                    }
                }
            }
            if (!withCurrency && base >= 4 && datesInLine(line).isEmpty()) {
                bareAmount.findAll(line).forEach { m ->
                    val raw = m.groupValues[1]
                    val v = raw.replace(",", "").toDoubleOrNull() ?: return@forEach
                    val looksLikeMoney = raw.contains('.') || raw.contains(',')
                    val looksLikeYear = !looksLikeMoney && v in 1900.0..2100.0
                    if (v > 0 && !looksLikeYear && (looksLikeMoney || v >= 10)) cands += Cand(v, defaultCurrency, base)
                }
            }
        }
        val best = cands.maxWithOrNull(compareBy<Cand> { it.score }.thenBy { it.value }) ?: return null
        return best.value to best.currency
    }

    private fun findReference(text: String, category: Category): String? {
        if (category == Category.ID_DOCUMENT) emiratesId.find(text)?.let { return it.value.replace(" ", "-") }
        return referenceRegex.findAll(text)
            .map { it.groupValues[2].trimEnd('-', '/', '.') }
            .firstOrNull { ref -> ref.any(Char::isDigit) && ref.length >= 5 && datesInLine(ref).isEmpty() }
    }
}
