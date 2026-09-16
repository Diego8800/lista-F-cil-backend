package com.listafacil.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listafacil.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _ui.value = AuthUiState(error = "Informe e-mail e senha.")
            return
        }
        _ui.value = AuthUiState(loading = true)
        viewModelScope.launch {
            authRepository.login(email, password).onFailure {
                _ui.value = AuthUiState(error = it.message ?: "Falha no login")
            }
            // Sucesso: o fluxo de autenticação troca a árvore de navegação automaticamente.
        }
    }

    fun signUp(name: String, email: String, password: String, confirm: String) {
        when {
            name.isBlank() -> return fail("Informe seu nome.")
            email.isBlank() -> return fail("Informe seu e-mail.")
            password.length < 8 -> return fail("A senha deve ter pelo menos 8 caracteres.")
            password != confirm -> return fail("As senhas não coincidem.")
        }
        _ui.value = AuthUiState(loading = true)
        viewModelScope.launch {
            authRepository.signUp(name, email, password).onFailure {
                _ui.value = AuthUiState(error = it.message ?: "Falha no cadastro")
            }
        }
    }

    fun requestReset(email: String) {
        if (email.isBlank()) {
            _ui.value = AuthUiState(error = "Informe seu e-mail.")
            return
        }
        _ui.value = AuthUiState(loading = true)
        viewModelScope.launch {
            authRepository.requestPasswordReset(email)
                .onSuccess {
                    _ui.value = AuthUiState(
                        successMessage = "Se o e-mail estiver cadastrado, você receberá o link de recuperação."
                    )
                }
                .onFailure { _ui.value = AuthUiState(error = it.message ?: "Falha ao solicitar recuperação") }
        }
    }

    fun changePassword(current: String, new: String) {
        if (current.isBlank() || new.length < 8) {
            _ui.value = AuthUiState(error = "Informe a senha atual e uma nova senha (mín. 8 caracteres).")
            return
        }
        _ui.value = AuthUiState(loading = true)
        viewModelScope.launch {
            authRepository.changePassword(current, new)
                .onSuccess { _ui.value = AuthUiState(successMessage = "Senha alterada com sucesso.") }
                .onFailure { _ui.value = AuthUiState(error = it.message ?: "Falha ao alterar a senha") }
        }
    }

    fun updateName(name: String) {
        if (name.isBlank()) {
            _ui.value = AuthUiState(error = "Informe seu nome.")
            return
        }
        _ui.value = AuthUiState(loading = true)
        viewModelScope.launch {
            authRepository.updateName(name)
                .onSuccess { _ui.value = AuthUiState(successMessage = "Nome atualizado.") }
                .onFailure { _ui.value = AuthUiState(error = it.message ?: "Falha ao atualizar") }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    private fun fail(message: String) {
        _ui.value = AuthUiState(error = message)
    }
}
