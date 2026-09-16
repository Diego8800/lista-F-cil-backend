package com.listafacil.app.domain.model

data class ShoppingList(
    val id: String,
    val name: String,
    val budgetCents: Long,
    val status: String,
    val finishedAt: String?,
    val createdAt: String,
    val spentCents: Long
)

data class ShoppingItem(
    val id: String,
    val listId: String,
    val productId: String,
    val productName: String,
    val categoryId: String,
    val categoryName: String,
    val establishmentId: String?,
    val establishmentName: String?,
    val quantity: Double,
    val unit: String,
    val previousPriceCents: Long?,
    val currentPriceCents: Long?,
    val note: String?,
    val purchased: Boolean
)

data class Category(val id: String, val name: String)

data class Establishment(val id: String, val name: String)

data class PriceStats(
    val currentCents: Long?,
    val previousCents: Long?,
    val minCents: Long?,
    val maxCents: Long?,
    val avgCents: Double?
)

data class PriceRecord(
    val id: String,
    val priceCents: Long,
    val quantity: Double,
    val unit: String,
    val establishmentName: String?,
    val recordedAt: String,
    val listId: String?
)

data class ProductHistory(
    val productId: String,
    val productName: String,
    val stats: PriceStats,
    val history: List<PriceRecord>
)

// ---------- Relatórios ----------

data class ReportRow(val label: String, val valueCents: Long?)

data class TopChange(
    val productName: String,
    val unit: String,
    val firstCents: Long,
    val lastCents: Long,
    val changePct: Double
)

data class Evolution(
    val productName: String,
    val points: List<PriceRecord>
)
