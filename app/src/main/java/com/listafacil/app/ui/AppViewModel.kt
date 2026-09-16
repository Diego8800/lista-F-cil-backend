package com.listafacil.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listafacil.app.data.local.SessionManager
import com.listafacil.app.di.TokenProvider
import com.listafacil.app.domain.model.ShoppingList
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AppUiState(
    val loading: Boolean = true,
    val authenticated: Boolean = false,
    val darkTheme: Boolean = false
)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val tokenProvider: TokenProvider,
    activeListHolder: ActiveListHolder
) : ViewModel() {

    /** Lista ativa atual — usada pelo menu lateral contextual (requisito §13). */
    val activeList: StateFlow<ShoppingList?> = activeListHolder.activeList

    private val _ui = MutableStateFlow(AppUiState())
    val ui: StateFlow<AppUiState> = _ui

    init {
        viewModelScope.launch {
            // Recoloca o token em memória (interceptor HTTP) a cada inicialização.
            tokenProvider.token = sessionManager.token.first()
            combine(sessionManager.token, sessionManager.theme) { token, theme ->
                AppUiState(
                    loading = false,
                    authenticated = !token.isNullOrBlank(),
                    darkTheme = theme == "DARK"
                )
            }.collect { _ui.value = it }
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { sessionManager.saveTheme(if (enabled) "DARK" else "LIGHT") }
    }
}
