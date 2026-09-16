package com.listafacil.app.data.remote

import com.listafacil.app.data.remote.dto.AddItemRequest
import com.listafacil.app.data.remote.dto.CategoriesResponse
import com.listafacil.app.data.remote.dto.CategoryDto
import com.listafacil.app.data.remote.dto.CreateListRequest
import com.listafacil.app.data.remote.dto.EstablishmentDto
import com.listafacil.app.data.remote.dto.EstablishmentsResponse
import com.listafacil.app.data.remote.dto.EvolutionRowDto
import com.listafacil.app.data.remote.dto.FinishRequest
import com.listafacil.app.data.remote.dto.FinishResponse
import com.listafacil.app.data.remote.dto.ItemDto
import com.listafacil.app.data.remote.dto.ListDetailDto
import com.listafacil.app.data.remote.dto.ListDto
import com.listafacil.app.data.remote.dto.ListsResponse
import com.listafacil.app.data.remote.dto.NameRequest
import com.listafacil.app.data.remote.dto.ProductHistoryDto
import com.listafacil.app.data.remote.dto.RowsResponse
import com.listafacil.app.data.remote.dto.SavingsResponse
import com.listafacil.app.data.remote.dto.SpentByCategoryRow
import com.listafacil.app.data.remote.dto.SpentByListRow
import com.listafacil.app.data.remote.dto.SpentByPeriodRow
import com.listafacil.app.data.remote.dto.TopChangeRow
import com.listafacil.app.data.remote.dto.UpdateItemRequest
import com.listafacil.app.data.remote.dto.UpdateListRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BackendApi {

    // ---------- Listas ----------
    @GET("lists")
    suspend fun getLists(@Query("status") status: String? = null): ListsResponse

    @POST("lists")
    suspend fun createList(@Body body: CreateListRequest): ListDto

    @GET("lists/{id}")
    suspend fun getList(@Path("id") id: String): ListDetailDto

    @PATCH("lists/{id}")
    suspend fun updateList(@Path("id") id: String, @Body body: UpdateListRequest): ListDto

    @POST("lists/{id}/finish")
    suspend fun finishList(@Path("id") id: String, @Body body: FinishRequest): FinishResponse

    // ---------- Itens ----------
    @POST("lists/{listId}/items")
    suspend fun addItem(@Path("listId") listId: String, @Body body: AddItemRequest): ItemDto

    @PATCH("lists/{listId}/items/{itemId}")
    suspend fun updateItem(
        @Path("listId") listId: String,
        @Path("itemId") itemId: String,
        @Body body: UpdateItemRequest
    ): ItemDto

    @DELETE("lists/{listId}/items/{itemId}")
    suspend fun deleteItem(@Path("listId") listId: String, @Path("itemId") itemId: String)

    // ---------- Histórico de produto ----------
    @GET("products/{id}/history")
    suspend fun getProductHistory(@Path("id") productId: String): ProductHistoryDto

    // ---------- Catálogos ----------
    @GET("categories")
    suspend fun getCategories(): CategoriesResponse

    @POST("categories")
    suspend fun createCategory(@Body body: NameRequest): CategoryDto

    @PATCH("categories/{id}")
    suspend fun updateCategory(@Path("id") id: String, @Body body: NameRequest): CategoryDto

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String)

    @POST("categories/seed")
    suspend fun seedCategories(): Map<String, Int>

    @GET("establishments")
    suspend fun getEstablishments(): EstablishmentsResponse

    @POST("establishments")
    suspend fun createEstablishment(@Body body: NameRequest): EstablishmentDto

    @PATCH("establishments/{id}")
    suspend fun updateEstablishment(@Path("id") id: String, @Body body: NameRequest): EstablishmentDto

    @DELETE("establishments/{id}")
    suspend fun deleteEstablishment(@Path("id") id: String)

    // ---------- Relatórios ----------
    @GET("reports/spent-by-list")
    suspend fun reportSpentByList(): RowsResponse<SpentByListRow>

    @GET("reports/spent-by-category")
    suspend fun reportSpentByCategory(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): RowsResponse<SpentByCategoryRow>

    @GET("reports/spent-by-period")
    suspend fun reportSpentByPeriod(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): RowsResponse<SpentByPeriodRow>

    @GET("reports/price-evolution")
    suspend fun reportPriceEvolution(
        @Query("product_id") productId: String? = null
    ): RowsResponse<EvolutionRowDto>

    @GET("reports/top-increases")
    suspend fun reportTopIncreases(): RowsResponse<TopChangeRow>

    @GET("reports/top-decreases")
    suspend fun reportTopDecreases(): RowsResponse<TopChangeRow>

    @GET("reports/savings")
    suspend fun reportSavings(): SavingsResponse
}
