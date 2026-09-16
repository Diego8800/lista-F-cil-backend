package com.listafacil.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listafacil.app.data.repository.ShoppingRepository
import com.listafacil.app.domain.model.ShoppingList
import com.listafacil.app.ui.ActiveListHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class DashboardUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val activeList: ShoppingList? = null,
    val recentFinished: List<ShoppingList> = emptyList(),
    val creatingList: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ShoppingRepository,
    private val activeListHolder: ActiveListHolder
) : ViewModel() {

    private val _ui = MutableStateFlow(DashboardUiState())
    val ui: StateFlow<DashboardUiState> = _ui

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            try {
                val active = repository.getActiveList()
                activeListHolder.set(active)
                val finished = repository.getLists("finished")
                _ui.value = _ui.value.copy(
                    loading = false,
                    activeList = active,
                    recentFinished = finished
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = messageFor(e))
            }
        }
    }

    fun createList(name: String, budgetCents: Long, onCreated: (String) -> Unit) {
        if (name.isBlank()) {
            _ui.value = _ui.value.copy(error = "Informe o nome da lista.")
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(creatingList = true, error = null)
            try {
                val list = repository.createList(name, budgetCents)
                activeListHolder.set(list)
                _ui.value = _ui.value.copy(creatingList = false, activeList = list)
                onCreated(list.id)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(creatingList = false, error = messageFor(e))
            }
        }
    }

    private fun messageFor(e: Exception): String = when (e) {
        is HttpException -> when (e.code()) {
            401, 403 -> "Sessão expirada. Faça login novamente."
            409 -> e.message() ?: "Conflito: já existe uma lista ativa."
            else -> "Erro (${e.code()}): ${e.message()}"
        }
        else -> e.message ?: "Erro de conexão. Verifique sua internet."
    }
}
