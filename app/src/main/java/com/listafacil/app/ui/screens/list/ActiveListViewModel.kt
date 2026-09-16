package com.listafacil.app.ui.screens.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listafacil.app.data.repository.ShoppingRepository
import com.listafacil.app.domain.model.ShoppingItem
import com.listafacil.app.domain.model.ShoppingList
import com.listafacil.app.ui.ActiveListHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.HttpException

data class ActiveListUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val list: ShoppingList? = null,
    val items: List<ShoppingItem> = emptyList(),
    val finishing: Boolean = false,
    val showNonPurchasedDialog: Boolean = false,
    val nonPurchasedCount: Int = 0,
    val finishedMessage: String? = null
)

@HiltViewModel
class ActiveListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ShoppingRepository,
    private val activeListHolder: ActiveListHolder
) : ViewModel() {

    private val listId: String = checkNotNull(savedStateHandle["listId"])

    private val _ui = MutableStateFlow(ActiveListUiState())
    val ui: StateFlow<ActiveListUiState> = _ui

    /** Valor restante do budget: budget - soma dos preços atuais dos comprados. */
    val remainingCents: Long
        get() {
            val s = _ui.value
            val spent = s.items.filter { it.purchased }
                .sumOf { it.currentPriceCents ?: 0L }
            return (s.list?.budgetCents ?: 0L) - spent
        }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            try {
                val (list, items) = repository.getListDetail(listId)
                activeListHolder.set(list)
                _ui.value = _ui.value.copy(loading = false, list = list, items = items)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = messageFor(e))
            }
        }
    }

    fun togglePurchased(item: ShoppingItem) {
        updateItem(item.id, purchased = !item.purchased)
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            try {
                repository.deleteItem(listId, item.id)
                refresh()
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(error = messageFor(e))
            }
        }
    }

    private fun updateItem(
        itemId: String,
        purchased: Boolean? = null,
        currentPriceCents: Long? = null
    ) {
        viewModelScope.launch {
            try {
                if (currentPriceCents != null) {
                    repository.updateItem(listId, itemId, currentPriceCents = currentPriceCents, purchased = purchased)
                } else {
                    repository.updateItem(listId, itemId, purchased = purchased)
                }
                refresh()
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(error = messageFor(e))
            }
        }
    }

    /** mode = null (sem não comprados), "delete" ou "transfer". */
    fun finish(mode: String?) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(finishing = true, error = null)
            try {
                val newListId = repository.finishList(listId, mode)
                activeListHolder.set(null)
                val message = if (newListId != null) {
                    "Lista finalizada! Não comprados transferidos para uma nova lista."
                } else {
                    "Lista finalizada!"
                }
                _ui.value = _ui.value.copy(finishing = false, finishedMessage = message)
            } catch (e: HttpException) {
                if (e.code() == 422) {
                    // Backend sinaliza que existem produtos não comprados.
                    _ui.value = _ui.value.copy(
                        finishing = false,
                        showNonPurchasedDialog = true
                    )
                } else {
                    _ui.value = _ui.value.copy(finishing = false, error = messageFor(e))
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(finishing = false, error = messageFor(e))
            }
        }
    }

    fun dismissNonPurchasedDialog() {
        _ui.value = _ui.value.copy(showNonPurchasedDialog = false)
    }

    fun consumeFinishedMessage() {
        _ui.value = _ui.value.copy(finishedMessage = null)
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
