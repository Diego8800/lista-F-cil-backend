package com.listafacil.app.core

import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

/** Dinheiro trafega em centavos (Long). */
object Money {

    fun format(cents: Long?): String =
        NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format((cents ?: 0L) / 100.0)

    /**
     * Interpreta entradas do usuário em pt-BR:
     * "10" -> 1000 | "10,50" -> 1050 | "10.50" -> 1050 | "R$ 1.234,56" -> 123456
     */
    fun parseToCents(input: String): Long? {
        val cleaned = input
            .replace("R$", "")
            .replace(" ", "")
            .trim()
        if (cleaned.isEmpty()) return null
        val normalized = if (cleaned.contains(",")) {
            cleaned.replace(".", "").replace(",", ".")
        } else {
            cleaned
        }
        val value = normalized.toDoubleOrNull() ?: return null
        if (value < 0) return null
        return (value * 100).toBigDecimal().setScale(0, RoundingMode.HALF_UP).toLong()
    }

    fun formatQuantity(quantity: Double): String =
        if (quantity == quantity.toLong().toDouble()) {
            quantity.toLong().toString()
        } else {
            quantity.toString()
        }

    /** "2026-09-13T18:30:00.000Z" -> "13/09/2026" (fallback: devolve o original). */
    fun formatDate(iso: String?): String {
        if (iso == null) return ""
        return try {
            val datePart = iso.substringBefore("T")
            val parts = datePart.split("-")
            if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else iso
        } catch (e: Exception) {
            iso
        }
    }

    fun formatPct(value: Double): String {
        val rounded = (value * 10).roundToLong() / 10.0
        return "${if (rounded > 0) "+" else ""}$rounded%"
    }
}
