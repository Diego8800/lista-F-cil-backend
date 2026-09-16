package com.listafacil.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String = "",
    val name: String = "",
    val email: String = ""
)

@Serializable
data class AuthResponseDto(
    val user: UserDto? = null,
    val token: String? = null
)

@Serializable
data class SignUpRequest(
    val name: String,
    val email: String,
    val password: String
)

@Serializable
data class SignInRequest(
    val email: String,
    val password: String
)

@Serializable
data class RequestResetRequest(
    val email: String,
    val redirectTo: String? = null
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
    val revokeOtherSessions: Boolean = true
)

@Serializable
data class UpdateUserRequest(
    val name: String
)
