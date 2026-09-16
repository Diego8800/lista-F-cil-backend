package com.listafacil.app.ui.screens.lists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listafacil.app.data.repository.ShoppingRepository
import com.listafacil.app.domain.model.ShoppingItem
import com.listafacil.app.domain.model.ShoppingList
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class FinishedListsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val lists: List<ShoppingList> = emptyList()
)

@HiltViewModel
class FinishedListsViewModel @Inject constructor(
    private val repository: ShoppingRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(FinishedListsUiState())
    val ui: StateFlow<FinishedListsUiState> = _ui

    init {
        viewModelScope.launch {
            try {
                val lists = repository.getLists("finished")
                _ui.value = FinishedListsUiState(loading = false, lists = lists)
            } catch (e: Exception) {
                _ui.value = FinishedListsUiState(loading = false, error = e.message ?: "Erro ao carregar")
            }
        }
    }
}

data class FinishedDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val list: ShoppingList? = null,
    val items: List<ShoppingItem> = emptyList()
)

@HiltViewModel
class FinishedDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ShoppingRepository
) : ViewModel() {

    private val listId: String = checkNotNull(savedStateHandle["listId"])

    private val _ui = MutableStateFlow(FinishedDetailUiState())
    val ui: StateFlow<FinishedDetailUiState> = _ui

    init {
        viewModelScope.launch {
            try {
                val (list, items) = repository.getListDetail(listId)
                _ui.value = FinishedDetailUiState(loading = false, list = list, items = items)
            } catch (e: Exception) {
                _ui.value = FinishedDetailUiState(loading = false, error = e.message ?: "Erro ao carregar")
            }
        }
    }
}
