package com.listafacil.app.core

/** Unidades suportadas (espelha o CHECK constraint do banco). */
val SUPPORTED_UNITS = listOf("un", "kg", "g", "L", "ml")

/** Grupo físico da unidade — produtos só são comparáveis no mesmo grupo. */
enum class UnitGroup { COUNT, MASS, VOLUME }

fun unitGroup(unit: String): UnitGroup = when (unit) {
    "un" -> UnitGroup.COUNT
    "kg", "g" -> UnitGroup.MASS
    "L", "ml" -> UnitGroup.VOLUME
    else -> UnitGroup.COUNT
}

/**
 * Normaliza uma quantidade para a unidade base do grupo:
 * massa -> gramas, volume -> mililitros, contagem -> unidades.
 */
fun normalizeQuantity(quantity: Double, unit: String): Pair<Double, String> = when (unit) {
    "kg" -> quantity * 1000.0 to "g"
    "g" -> quantity to "g"
    "L" -> quantity * 1000.0 to "ml"
    "ml" -> quantity to "ml"
    else -> quantity to "un"
}
