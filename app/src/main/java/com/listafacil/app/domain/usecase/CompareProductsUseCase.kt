package com.listafacil.app.domain.usecase

import com.listafacil.app.core.UnitGroup
import com.listafacil.app.core.normalizeQuantity
import com.listafacil.app.core.unitGroup
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

data class ComparisonInput(
    val name: String,
    val priceCents: Long,
    val quantity: Double,
    val unit: String
)

data class UnitPrice(
    val name: String,
    val priceCents: Long,
    val quantity: Double,
    val unit: String,
    val normalizedQuantity: Double,
    val baseUnit: String,
    val pricePerBase: Double // centavos por unidade base
)

data class ComparisonResult(
    val compatible: Boolean,
    val products: List<UnitPrice>,
    val bestIndex: Int?,
    val message: String?
)

/**
 * Comparador de custo proporcional: normaliza kg→g, L→ml e calcula o preço
 * por unidade base. Só compara produtos cujas unidades sejam do mesmo grupo
 * físico (massa × massa, volume × volume, unidade × unidade) — nunca força
 * conversões fisicamente inválidas (ex.: kg vs. L).
 */
@Singleton
class CompareProductsUseCase @Inject constructor() {

    operator fun invoke(items: List<ComparisonInput>): ComparisonResult {
        if (items.size < 2) {
            return ComparisonResult(false, emptyList(), null, "Informe pelo menos dois produtos.")
        }
        if (items.any { it.priceCents < 0 || it.quantity <= 0 }) {
            return ComparisonResult(false, emptyList(), null, "Preços e quantidades devem ser maiores que zero.")
        }

        val unitPrices = items.map { item ->
            val (normalized, baseUnit) = normalizeQuantity(item.quantity, item.unit)
            UnitPrice(
                name = item.name,
                priceCents = item.priceCents,
                quantity = item.quantity,
                unit = item.unit,
                normalizedQuantity = normalized,
                baseUnit = baseUnit,
                pricePerBase = if (normalized > 0) item.priceCents / normalized else Double.MAX_VALUE
            )
        }

        val groups = unitPrices.map { unitGroup(it.unit) }.distinct()
        if (groups.size > 1) {
            return ComparisonResult(
                compatible = false,
                products = unitPrices,
                bestIndex = null,
                message = "Unidades incompatíveis: compare apenas produtos do mesmo tipo " +
                    "(massa com massa, volume com volume ou unidade com unidade)."
            )
        }

        val bestIndex = unitPrices.indices.minByOrNull { unitPrices[it].pricePerBase }
        return ComparisonResult(true, unitPrices, bestIndex, null)
    }

    fun pricePerBaseDisplay(up: UnitPrice): String {
        val value = up.pricePerBase
        val cents = (value * 100).roundToLong() / 100.0
        return "%.4f".format(cents).trimEnd('0').trimEnd('.') + " centavos/${up.baseUnit}"
    }
}
