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

interface NeonAuthApi {

    @POST("auth/sign-up/email")
    suspend fun signUp(@Body body: SignUpRequest): AuthResponseDto

    @POST("auth/sign-in/email")
    suspend fun signIn(@Body body: SignInRequest): AuthResponseDto

    @POST("auth/sign-out")
    suspend fun signOut(): Map<String, Boolean>

    @POST("auth/request-password-reset")
    suspend fun requestPasswordReset(@Body body: RequestResetRequest): Map<String, Boolean>

    @POST("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Map<String, Boolean>

    @POST("auth/update-user")
    suspend fun updateUser(@Body body: UpdateUserRequest): UserDto
}
