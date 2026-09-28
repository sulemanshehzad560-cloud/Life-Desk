package com.lifedesk.app.domain

import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val whole = DecimalFormat("#,##0")
private val decimal = DecimalFormat("#,##0.00")
private val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

fun money(amount: Double, currency: String): String {
    val rounded = Math.round(amount * 100) / 100.0
    val text = if (rounded % 1.0 == 0.0) whole.format(rounded) else decimal.format(rounded)
    return "$currency $text"
}

fun LocalDate.pretty(): String = format(dateFmt)

fun percentChange(old: Double, new: Double): Int = if (old == 0.0) 0 else Math.round((new - old) / old * 100).toInt()
