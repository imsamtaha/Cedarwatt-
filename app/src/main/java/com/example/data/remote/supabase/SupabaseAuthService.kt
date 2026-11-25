package com.example.data.remote.supabase

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface SupabaseAuthService {

    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body request: SupabaseSignUpRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body request: SupabaseLoginRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/logout")
    suspend fun signOut(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String
    ): Response<Unit>

    @GET("auth/v1/user")
    suspend fun getCurrentUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String
    ): Response<SupabaseUser>

    @POST("auth/v1/recover")
    suspend fun recoverPassword(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body request: SupabaseRecoverRequest
    ): Response<Unit>

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshToken(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body request: SupabaseRefreshTokenRequest
    ): Response<SupabaseAuthResponse>
}
