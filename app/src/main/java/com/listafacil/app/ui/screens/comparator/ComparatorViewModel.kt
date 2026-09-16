package com.listafacil.app.ui.screens.comparator

import androidx.lifecycle.ViewModel
import com.listafacil.app.core.Money
import com.listafacil.app.domain.usecase.CompareProductsUseCase
import com.listafacil.app.domain.usecase.ComparisonInput
import com.listafacil.app.domain.usecase.ComparisonResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ComparatorProductInput(
    val name: String = "",
    val price: String = "",
    val quantity: String = "",
    val unit: String = "un"
)

data class ComparatorUiState(
    val products: List<ComparatorProductInput> = listOf(
        ComparatorProductInput(),
        ComparatorProductInput()
    ),
    val result: ComparisonResult? = null,
    val inputError: String? = null
)

@HiltViewModel
class ComparatorViewModel @Inject constructor(
    private val compareProducts: CompareProductsUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(ComparatorUiState())
    val ui: StateFlow<ComparatorUiState> = _ui

    fun update(index: Int, input: ComparatorProductInput) {
        val products = _ui.value.products.toMutableList()
        products[index] = input
        _ui.value = _ui.value.copy(products = products, inputError = null)
    }

    fun addProduct() {
        val products = _ui.value.products.toMutableList()
        products.add(ComparatorProductInput())
        _ui.value = _ui.value.copy(products = products)
    }

    fun removeAt(index: Int) {
        val products = _ui.value.products.toMutableList()
        if (products.size > 2) {
            products.removeAt(index)
            _ui.value = _ui.value.copy(products = products)
        }
    }

    fun compare() {
        val products = _ui.value.products
        if (products.any { it.price.isBlank() || it.quantity.isBlank() }) {
            _ui.value = _ui.value.copy(
                inputError = "Informe preço e quantidade de todos os produtos.",
                result = null
            )
            return
        }
        val inputs = products.map {
            ComparisonInput(
                name = it.name.ifBlank { "Produto" },
                priceCents = Money.parseToCents(it.price) ?: 0L,
                quantity = it.quantity.replace(",", ".").toDoubleOrNull() ?: 0.0,
                unit = it.unit
            )
        }
        if (inputs.any { it.priceCents <= 0L || it.quantity <= 0.0 }) {
            _ui.value = _ui.value.copy(
                inputError = "Preços e quantidades devem ser maiores que zero.",
                result = null
            )
            return
        }
        _ui.value = _ui.value.copy(result = compareProducts(inputs), inputError = null)
    }
}
