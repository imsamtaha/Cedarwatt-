package com.example.data.remote.supabase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseSignUpRequest(
    val email: String,
    val password: String,
    val data: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseLoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class SupabaseRecoverRequest(
    val email: String
)

@JsonClass(generateAdapter = true)
data class SupabaseRefreshTokenRequest(
    @Json(name = "refresh_token")
    val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class SupabaseUser(
    val id: String,
    val email: String? = null,
    val role: String? = null,
    @Json(name = "user_metadata")
    val userMetadata: Map<String, String>? = null,
    @Json(name = "created_at")
    val createdAt: String? = null,
    val phone: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthResponse(
    @Json(name = "access_token")
    val accessToken: String? = null,
    @Json(name = "token_type")
    val tokenType: String? = null,
    @Json(name = "expires_in")
    val expiresIn: Long? = null,
    @Json(name = "refresh_token")
    val refreshToken: String? = null,
    val user: SupabaseUser? = null,
    val id: String? = null,
    val email: String? = null,
    @Json(name = "error_description")
    val errorDescription: String? = null,
    val error: String? = null,
    val msg: String? = null,
    val message: String? = null
)

data class AuthUser(
    val id: String,
    val email: String,
    val fullName: String,
    val phone: String = "",
    val location: String = "Achrafieh, Beirut",
    val municipality: String = "Municipality of Beirut",
    val token: String = "",
    val isGuest: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

sealed class AuthState {
    data object Initializing : AuthState()
    data object Unauthenticated : AuthState()
    data class Authenticated(val user: AuthUser) : AuthState()
    data class Loading(val message: String = "Authenticating with Supabase...") : AuthState()
}

sealed class AuthResult {
    data class Success(val user: AuthUser, val message: String) : AuthResult()
    data class NeedsEmailConfirmation(val email: String, val message: String) : AuthResult()
    data class Error(val errorMessage: String) : AuthResult()
}
