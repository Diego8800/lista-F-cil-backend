package com.listafacil.app.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listafacil.app.data.repository.ShoppingRepository
import com.listafacil.app.domain.model.Evolution
import com.listafacil.app.domain.model.ReportRow
import com.listafacil.app.domain.model.TopChange
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class ReportType(val label: String) {
    SPENT_BY_LIST("Total gasto por lista"),
    SPENT_BY_CATEGORY("Gastos por categoria"),
    SPENT_BY_PERIOD("Gastos por período"),
    PRICE_EVOLUTION("Evolução dos preços"),
    TOP_INCREASES("Produtos que mais aumentaram"),
    TOP_DECREASES("Produtos que mais reduziram"),
    SAVINGS("Economia com menor preço")
}

data class ReportsUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val selected: ReportType? = null,
    val rows: List<ReportRow> = emptyList(),
    val topChanges: List<TopChange> = emptyList(),
    val evolutions: List<Evolution> = emptyList(),
    val totalCents: Long? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val repository: ShoppingRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(ReportsUiState())
    val ui: StateFlow<ReportsUiState> = _ui

    fun select(type: ReportType) {
        _ui.value = ReportsUiState(loading = true, selected = type)
        viewModelScope.launch {
            try {
                val state = when (type) {
                    ReportType.SPENT_BY_LIST ->
                        ReportsUiState(selected = type, rows = repository.reportSpentByList())
                    ReportType.SPENT_BY_CATEGORY ->
                        ReportsUiState(selected = type, rows = repository.reportSpentByCategory())
                    ReportType.SPENT_BY_PERIOD ->
                        ReportsUiState(selected = type, rows = repository.reportSpentByPeriod())
                    ReportType.PRICE_EVOLUTION ->
                        ReportsUiState(selected = type, evolutions = repository.reportPriceEvolution())
                    ReportType.TOP_INCREASES ->
                        ReportsUiState(selected = type, topChanges = repository.reportTopIncreases())
                    ReportType.TOP_DECREASES ->
                        ReportsUiState(selected = type, topChanges = repository.reportTopDecreases())
                    ReportType.SAVINGS -> {
                        val (rows, total) = repository.reportSavings()
                        ReportsUiState(selected = type, rows = rows, totalCents = total)
                    }
                }
                _ui.value = state
            } catch (e: Exception) {
                _ui.value = ReportsUiState(selected = type, error = e.message ?: "Erro ao gerar relatório")
            }
        }
    }
}
