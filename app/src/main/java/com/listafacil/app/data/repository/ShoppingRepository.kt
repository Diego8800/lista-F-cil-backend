package com.listafacil.app.data.repository

import com.listafacil.app.data.remote.BackendApi
import com.listafacil.app.data.remote.dto.AddItemRequest
import com.listafacil.app.data.remote.dto.CreateListRequest
import com.listafacil.app.data.remote.dto.FinishRequest
import com.listafacil.app.data.remote.dto.NameRequest
import com.listafacil.app.data.remote.dto.UpdateItemRequest
import com.listafacil.app.data.remote.dto.UpdateListRequest
import com.listafacil.app.domain.model.Category
import com.listafacil.app.domain.model.Establishment
import com.listafacil.app.domain.model.Evolution
import com.listafacil.app.domain.model.PriceRecord
import com.listafacil.app.domain.model.PriceStats
import com.listafacil.app.domain.model.ProductHistory
import com.listafacil.app.domain.model.ReportRow
import com.listafacil.app.domain.model.ShoppingItem
import com.listafacil.app.domain.model.ShoppingList
import com.listafacil.app.domain.model.TopChange
import javax.inject.Inject
import javax.inject.Singleton

/** Erro de negócio vindo do backend (ex.: produtos não comprados na finalização). */
class ApiException(val statusCode: Int, message: String) : Exception(message)

@Singleton
class ShoppingRepository @Inject constructor(
    private val api: BackendApi
) {

    // ---------- Listas ----------

    suspend fun getLists(status: String? = null): List<ShoppingList> =
        api.getLists(status).lists.map { it.toModel() }

    suspend fun getActiveList(): ShoppingList? = getLists("active").firstOrNull()

    suspend fun createList(name: String, budgetCents: Long): ShoppingList =
        api.createList(CreateListRequest(name, budgetCents)).toModel()

    suspend fun getListDetail(id: String): Pair<ShoppingList, List<ShoppingItem>> {
        val detail = api.getList(id)
        return detail.list.toModel() to detail.items.map { it.toModel() }
    }

    suspend fun updateList(id: String, name: String? = null, budgetCents: Long? = null): ShoppingList =
        api.updateList(id, UpdateListRequest(name, budgetCents)).toModel()

    /** @return id da nova lista quando mode == "transfer" */
    suspend fun finishList(listId: String, mode: String?, newListName: String? = null): String? {
        val response = api.finishList(listId, FinishRequest(mode, newListName))
        return response.new_list_id
    }

    // ---------- Itens ----------

    suspend fun addItem(
        listId: String,
        productName: String,
        categoryId: String,
        quantity: Double,
        unit: String,
        currentPriceCents: Long?,
        note: String?,
        establishmentId: String?
    ): ShoppingItem = api.addItem(
        listId,
        AddItemRequest(productName, categoryId, quantity, unit, currentPriceCents, note, establishmentId)
    ).toModel()

    suspend fun updateItem(
        listId: String,
        itemId: String,
        quantity: Double? = null,
        unit: String? = null,
        currentPriceCents: Long? = null,
        note: String? = null,
        establishmentId: String? = null,
        purchased: Boolean? = null,
        categoryId: String? = null
    ): ShoppingItem = api.updateItem(
        listId, itemId,
        UpdateItemRequest(quantity, unit, currentPriceCents, note, establishmentId, purchased, categoryId)
    ).toModel()

    suspend fun deleteItem(listId: String, itemId: String) =
        api.deleteItem(listId, itemId)

    // ---------- Histórico ----------

    suspend fun getProductHistory(productId: String): ProductHistory {
        val dto = api.getProductHistory(productId)
        return ProductHistory(
            productId = dto.product_id,
            productName = dto.product_name,
            stats = PriceStats(
                currentCents = dto.stats.current_cents,
                previousCents = dto.stats.previous_cents,
                minCents = dto.stats.min_cents,
                maxCents = dto.stats.max_cents,
                avgCents = dto.stats.avg_cents
            ),
            history = dto.history.map {
                PriceRecord(it.id, it.price_cents, it.quantity, it.unit, it.establishment_name, it.recorded_at, it.list_id)
            }
        )
    }

    // ---------- Catálogos ----------

    suspend fun getCategories(): List<Category> =
        api.getCategories().categories.map { Category(it.id, it.name) }

    suspend fun seedCategories() = api.seedCategories()

    suspend fun createCategory(name: String): Category {
        val dto = api.createCategory(NameRequest(name))
        return Category(dto.id, dto.name)
    }

    suspend fun getEstablishments(): List<Establishment> =
        api.getEstablishments().establishments.map { Establishment(it.id, it.name) }

        suspend fun searchProducts(q: String): List<String> =
    api.searchProducts(q).products.map { it.name }

    suspend fun createEstablishment(name: String): Establishment {
        val dto = api.createEstablishment(NameRequest(name))
        return Establishment(dto.id, dto.name)
    }

    suspend fun updateEstablishment(id: String, name: String) {
        api.updateEstablishment(id, NameRequest(name))
    }

    suspend fun deleteEstablishment(id: String) = api.deleteEstablishment(id)

    // ---------- Relatórios ----------

    suspend fun reportSpentByList(): List<ReportRow> =
        api.reportSpentByList().rows.map { ReportRow(it.name, it.total_cents) }

    suspend fun reportSpentByCategory(): List<ReportRow> =
        api.reportSpentByCategory().rows.map { ReportRow(it.category_name, it.total_cents) }

    suspend fun reportSpentByPeriod(): List<ReportRow> =
        api.reportSpentByPeriod().rows.map { ReportRow(it.day, it.total_cents) }

    suspend fun reportPriceEvolution(): List<Evolution> =
        api.reportPriceEvolution().rows.map { row ->
            Evolution(
                productName = row.product_name,
                points = row.points.map {
                    PriceRecord("", it.price_cents, 0.0, it.unit, it.establishment_name, it.recorded_at, null)
                }
            )
        }

    suspend fun reportTopIncreases(): List<TopChange> =
        api.reportTopIncreases().rows.map {
            TopChange(it.product_name, it.unit, it.first_cents, it.last_cents, it.change_pct)
        }

    suspend fun reportTopDecreases(): List<TopChange> =
        api.reportTopDecreases().rows.map {
            TopChange(it.product_name, it.unit, it.first_cents, it.last_cents, it.change_pct)
        }

    suspend fun reportSavings(): Pair<List<ReportRow>, Long> {
        val response = api.reportSavings()
        return response.items.map { ReportRow(it.product_name, it.savings_cents) } to response.total_cents
    }

    // ---------- Mappers ----------

    private fun com.listafacil.app.data.remote.dto.ListDto.toModel() = ShoppingList(
        id = id, name = name, budgetCents = budget_cents, status = status,
        finishedAt = finished_at, createdAt = created_at, spentCents = spent_cents
    )

    private fun com.listafacil.app.data.remote.dto.ItemDto.toModel() = ShoppingItem(
        id = id, listId = list_id, productId = product_id, productName = product_name,
        categoryId = category_id, categoryName = category_name,
        establishmentId = establishment_id, establishmentName = establishment_name,
        quantity = quantity, unit = unit,
        previousPriceCents = previous_price_cents, currentPriceCents = current_price_cents,
        note = note, purchased = purchased
    )
}
