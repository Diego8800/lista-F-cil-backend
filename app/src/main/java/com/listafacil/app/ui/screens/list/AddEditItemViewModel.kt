package com.listafacil.app.ui.screens.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listafacil.app.core.SUPPORTED_UNITS
import com.listafacil.app.data.repository.ShoppingRepository
import com.listafacil.app.domain.model.Category
import com.listafacil.app.domain.model.Establishment
import com.listafacil.app.domain.model.ShoppingItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.HttpException

data class AddEditItemUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val isEditing: Boolean = false,
    val item: ShoppingItem? = null,
    val categories: List<Category> = emptyList(),
    val establishments: List<Establishment> = emptyList()
)

@HiltViewModel
class AddEditItemViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ShoppingRepository
) : ViewModel() {

    private val listId: String = checkNotNull(savedStateHandle["listId"])
    private val itemId: String? = savedStateHandle["itemId"]

    private val _ui = MutableStateFlow(AddEditItemUiState(isEditing = itemId != null))
    val ui: StateFlow<AddEditItemUiState> = _ui

    init {
        viewModelScope.launch {
            try {
                repository.seedCategories()
                val categories = repository.getCategories()
                val establishments = repository.getEstablishments()
                var item: ShoppingItem? = null
                if (itemId != null) {
                    item = repository.getListDetail(listId).second.firstOrNull { it.id == itemId }
                }
                _ui.value = _ui.value.copy(
                    loading = false,
                    categories = categories,
                    establishments = establishments,
                    item = item
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = messageFor(e))
            }
        }
    }

    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val category = repository.createCategory(name)
                _ui.value = _ui.value.copy(
                    categories = (_ui.value.categories + category).sortedBy { it.name }
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(error = messageFor(e))
            }
        }
    }

    fun save(
        productName: String,
        categoryId: String?,
        quantity: Double,
        unit: String,
        currentPriceCents: Long?,
        note: String?,
        establishmentId: String?
    ) {
        if (productName.isBlank()) {
            _ui.value = _ui.value.copy(error = "Informe o nome do produto.")
            return
        }
        if (categoryId == null) {
            _ui.value = _ui.value.copy(error = "Selecione uma categoria.")
            return
        }
        if (quantity <= 0) {
            _ui.value = _ui.value.copy(error = "Quantidade deve ser maior que zero.")
            return
        }
        _ui.value = _ui.value.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                if (itemId == null) {
                    repository.addItem(
                        listId, productName, categoryId, quantity, unit,
                        currentPriceCents, note?.takeIf { it.isNotBlank() }, establishmentId
                    )
                } else {
                    repository.updateItem(
                        listId, itemId, quantity = quantity, unit = unit,
                        currentPriceCents = currentPriceCents,
                        note = note?.takeIf { it.isNotBlank() },
                        establishmentId = establishmentId,
                        categoryId = categoryId
                    )
                }
                _ui.value = _ui.value.copy(saving = false, saved = true)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(saving = false, error = messageFor(e))
            }
        }
    }

    fun messageShown() {
        _ui.value = _ui.value.copy(error = null)
    }

    private fun messageFor(e: Exception): String = when (e) {
        is HttpException -> when (e.code()) {
            401, 403 -> "Sessão expirada. Faça login novamente."
            409 -> "Este produto já está na lista."
            else -> "Erro (${e.code()})"
        }
        else -> e.message ?: "Erro de conexão."
    }
}
