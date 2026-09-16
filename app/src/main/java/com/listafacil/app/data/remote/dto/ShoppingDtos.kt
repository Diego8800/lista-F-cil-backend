package com.listafacil.app.data.remote.dto

import kotlinx.serialization.Serializable

// ---------- Listas ----------

@Serializable
data class ListDto(
    val id: String = "",
    val name: String = "",
    val budget_cents: Long = 0,
    val status: String = "active",
    val finished_at: String? = null,
    val created_at: String = "",
    val spent_cents: Long = 0
)

@Serializable
data class ListsResponse(
    val lists: List<ListDto> = emptyList()
)

@Serializable
data class ListDetailDto(
    val list: ListDto,
    val items: List<ItemDto> = emptyList()
)

@Serializable
data class CreateListRequest(
    val name: String,
    val budget_cents: Long
)

@Serializable
data class UpdateListRequest(
    val name: String? = null,
    val budget_cents: Long? = null
)

@Serializable
data class FinishRequest(
    val mode: String? = null,
    val new_list_name: String? = null
)

@Serializable
data class FinishResponse(
    val finished_list_id: String = "",
    val new_list_id: String? = null,
    val non_purchased_count: Int = 0
)

// ---------- Itens ----------

@Serializable
data class ItemDto(
    val id: String = "",
    val list_id: String = "",
    val product_id: String = "",
    val product_name: String = "",
    val category_id: String = "",
    val category_name: String = "",
    val establishment_id: String? = null,
    val establishment_name: String? = null,
    val quantity: Double = 0.0,
    val unit: String = "un",
    val previous_price_cents: Long? = null,
    val current_price_cents: Long? = null,
    val note: String? = null,
    val purchased: Boolean = false
)

@Serializable
data class AddItemRequest(
    val product_name: String,
    val category_id: String,
    val quantity: Double,
    val unit: String,
    val current_price_cents: Long? = null,
    val note: String? = null,
    val establishment_id: String? = null
)

@Serializable
data class UpdateItemRequest(
    val quantity: Double? = null,
    val unit: String? = null,
    val current_price_cents: Long? = null,
    val note: String? = null,
    val establishment_id: String? = null,
    val purchased: Boolean? = null,
    val category_id: String? = null
)

// ---------- Catálogos ----------

@Serializable
data class CategoryDto(val id: String = "", val name: String = "")

@Serializable
data class CategoriesResponse(val categories: List<CategoryDto> = emptyList())

@Serializable
data class EstablishmentDto(val id: String = "", val name: String = "")

@Serializable
data class EstablishmentsResponse(val establishments: List<EstablishmentDto> = emptyList())

@Serializable
data class NameRequest(val name: String)

// ---------- Histórico de preços ----------

@Serializable
data class ProductStatsDto(
    val current_cents: Long? = null,
    val previous_cents: Long? = null,
    val min_cents: Long? = null,
    val max_cents: Long? = null,
    val avg_cents: Double? = null
)

@Serializable
data class HistoryPointDto(
    val id: String = "",
    val price_cents: Long = 0,
    val quantity: Double = 0.0,
    val unit: String = "un",
    val establishment_name: String? = null,
    val recorded_at: String = "",
    val list_id: String? = null
)

@Serializable
data class ProductHistoryDto(
    val product_id: String = "",
    val product_name: String = "",
    val stats: ProductStatsDto = ProductStatsDto(),
    val history: List<HistoryPointDto> = emptyList()
)

// ---------- Relatórios ----------

@Serializable
data class RowsResponse<T>(val rows: List<T> = emptyList())

@Serializable
data class SpentByListRow(
    val id: String = "",
    val name: String = "",
    val finished_at: String? = null,
    val total_cents: Long = 0
)

@Serializable
data class SpentByCategoryRow(val category_name: String = "", val total_cents: Long = 0)

@Serializable
data class SpentByPeriodRow(val day: String = "", val total_cents: Long = 0)

@Serializable
data class EvolutionPointDto(
    val recorded_at: String = "",
    val price_cents: Long = 0,
    val unit: String = "un",
    val establishment_name: String? = null
)

@Serializable
data class EvolutionRowDto(
    val product_id: String = "",
    val product_name: String = "",
    val points: List<EvolutionPointDto> = emptyList()
)

@Serializable
data class TopChangeRow(
    val product_name: String = "",
    val unit: String = "un",
    val first_cents: Long = 0,
    val last_cents: Long = 0,
    val change_pct: Double = 0.0
)

@Serializable
data class SavingsItemDto(val product_name: String = "", val savings_cents: Long = 0)

@Serializable
data class SavingsResponse(
    val items: List<SavingsItemDto> = emptyList(),
    val total_cents: Long = 0
)
