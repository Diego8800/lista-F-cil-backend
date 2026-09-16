package com.listafacil.app.ui.screens.establishments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listafacil.app.data.repository.ShoppingRepository
import com.listafacil.app.domain.model.Establishment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class EstablishmentsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val establishments: List<Establishment> = emptyList()
)

@HiltViewModel
class EstablishmentsViewModel @Inject constructor(
    private val repository: ShoppingRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(EstablishmentsUiState())
    val ui: StateFlow<EstablishmentsUiState> = _ui

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                val establishments = repository.getEstablishments()
                _ui.value = EstablishmentsUiState(loading = false, establishments = establishments)
            } catch (e: Exception) {
                _ui.value = EstablishmentsUiState(loading = false, error = e.message ?: "Erro ao carregar")
            }
        }
    }

    fun add(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val created = repository.createEstablishment(name)
                _ui.value = _ui.value.copy(
                    establishments = (_ui.value.establishments + created).sortedBy { it.name }
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(error = e.message ?: "Erro ao cadastrar")
            }
        }
    }

    fun rename(id: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                repository.updateEstablishment(id, name)
                load()
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(error = e.message ?: "Erro ao editar")
            }
        }
    }

    fun remove(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteEstablishment(id)
                _ui.value = _ui.value.copy(
                    establishments = _ui.value.establishments.filterNot { it.id == id }
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(error = e.message ?: "Erro ao excluir (pode estar em uso)")
            }
        }
    }

    fun messageShown() {
        _ui.value = _ui.value.copy(error = null)
    }
}
