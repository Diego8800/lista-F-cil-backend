package com.listafacil.app.data.repository

import com.listafacil.app.data.local.SessionManager
import com.listafacil.app.data.remote.NeonAuthApi
import com.listafacil.app.data.remote.dto.ChangePasswordRequest
import com.listafacil.app.data.remote.dto.RequestResetRequest
import com.listafacil.app.data.remote.dto.SignInRequest
import com.listafacil.app.data.remote.dto.SignUpRequest
import com.listafacil.app.data.remote.dto.UpdateUserRequest
import com.listafacil.app.di.TokenProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Autenticação via Neon Auth. O token de sessão é persistido criptografado
 * e espelhado em memória para o interceptor HTTP.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApi: NeonAuthApi,
    private val sessionManager: SessionManager,
    private val tokenProvider: TokenProvider
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val isAuthenticated: Flow<Boolean> =
        sessionManager.token.flatMapLatest { token ->
            tokenProvider.token = token
            flowOf(!token.isNullOrBlank())
        }

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val response = authApi.signIn(SignInRequest(email.trim(), password))
        persist(response.token)
    }

    suspend fun signUp(name: String, email: String, password: String): Result<Unit> = runCatching {
        val response = authApi.signUp(SignUpRequest(name.trim(), email.trim(), password))
        persist(response.token)
    }

    suspend fun logout() {
        runCatching { authApi.signOut() }
        tokenProvider.token = null
        sessionManager.clearToken()
    }

    suspend fun requestPasswordReset(email: String): Result<Unit> = runCatching {
        authApi.requestPasswordReset(RequestResetRequest(email.trim()))
        Unit
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = runCatching {
        authApi.changePassword(ChangePasswordRequest(currentPassword, newPassword))
        Unit
    }

    suspend fun updateName(name: String): Result<Unit> = runCatching {
        authApi.updateUser(UpdateUserRequest(name.trim()))
        Unit
    }

    private suspend fun persist(token: String?) {
        require(!token.isNullOrBlank()) {
            "Resposta de autenticação sem token. Verifique NEON_AUTH_BASE_URL no build.gradle.kts."
        }
        tokenProvider.token = token
        sessionManager.saveToken(token)
    }
}
