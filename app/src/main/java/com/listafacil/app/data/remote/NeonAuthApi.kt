package com.listafacil.app.data.remote

import com.listafacil.app.data.remote.dto.AuthResponseDto
import com.listafacil.app.data.remote.dto.ChangePasswordRequest
import com.listafacil.app.data.remote.dto.RequestResetRequest
import com.listafacil.app.data.remote.dto.SignInRequest
import com.listafacil.app.data.remote.dto.SignUpRequest
import com.listafacil.app.data.remote.dto.UpdateUserRequest
import com.listafacil.app.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Endpoints REST do Neon Auth (Managed Better Auth).
 * A base URL é configurada em BuildConfig.NEON_AUTH_BASE_URL.
 *
 * NOTA: confira os caminhos exatos no console do Neon (Auth) — Better Auth
 * expõe /sign-up/email, /sign-in/email, /sign-out, /request-password-reset,
 * /change-password e /update-user.
 */
interface NeonAuthApi {

    @POST("sign-up/email")
    suspend fun signUp(@Body body: SignUpRequest): AuthResponseDto

    @POST("sign-in/email")
    suspend fun signIn(@Body body: SignInRequest): AuthResponseDto

    @POST("sign-out")
    suspend fun signOut(): Map<String, Boolean>

    @POST("request-password-reset")
    suspend fun requestPasswordReset(@Body body: RequestResetRequest): Map<String, Boolean>

    @POST("change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Map<String, Boolean>

    @POST("update-user")
    suspend fun updateUser(@Body body: UpdateUserRequest): UserDto
}
