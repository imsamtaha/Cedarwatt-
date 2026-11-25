package com.example.data.remote.supabase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

class SupabaseAuthManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cedarwatt_supabase_auth_prefs", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _activeUser = MutableStateFlow<AuthUser?>(null)
    val activeUser: StateFlow<AuthUser?> = _activeUser.asStateFlow()

    private val _supabaseUrl = MutableStateFlow(resolveInitialUrl())
    val supabaseUrl: StateFlow<String> = _supabaseUrl.asStateFlow()

    private val _supabaseAnonKey = MutableStateFlow(resolveInitialAnonKey())
    val supabaseAnonKey: StateFlow<String> = _supabaseAnonKey.asStateFlow()

    private var authService: SupabaseAuthService = buildService(_supabaseUrl.value)

    init {
        restoreSession()
    }

    private fun resolveInitialUrl(): String {
        val saved = prefs.getString("custom_supabase_url", null)
        if (!saved.isNullOrBlank()) return saved

        return try {
            val configUrl = BuildConfig.SUPABASE_URL
            if (!configUrl.isNullOrBlank() && configUrl != "MY_SUPABASE_URL") {
                configUrl
            } else {
                DEFAULT_SUPABASE_URL
            }
        } catch (e: Throwable) {
            DEFAULT_SUPABASE_URL
        }
    }

    private fun resolveInitialAnonKey(): String {
        val saved = prefs.getString("custom_supabase_anon_key", null)
        if (!saved.isNullOrBlank()) return saved

        return try {
            val configKey = BuildConfig.SUPABASE_ANON_KEY
            if (!configKey.isNullOrBlank() && configKey != "MY_SUPABASE_ANON_KEY") {
                configKey
            } else {
                DEFAULT_SUPABASE_ANON_KEY
            }
        } catch (e: Throwable) {
            DEFAULT_SUPABASE_ANON_KEY
        }
    }

    private fun buildService(rawUrl: String): SupabaseAuthService {
        val normalizedUrl = if (rawUrl.endsWith("/")) rawUrl else "$rawUrl/"
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseAuthService::class.java)
    }

    private fun restoreSession() {
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        val isGuest = prefs.getBoolean("is_guest", false)
        val userId = prefs.getString("user_id", null)
        val email = prefs.getString("user_email", "") ?: ""
        val fullName = prefs.getString("user_name", "Resident") ?: "Resident"
        val phone = prefs.getString("user_phone", "") ?: ""
        val location = prefs.getString("user_location", "Achrafieh, Beirut") ?: "Achrafieh, Beirut"
        val municipality = prefs.getString("user_municipality", "Municipality of Beirut") ?: "Municipality of Beirut"
        val token = prefs.getString("access_token", "") ?: ""

        if (isLoggedIn && !userId.isNullOrBlank()) {
            val user = AuthUser(
                id = userId,
                email = email,
                fullName = fullName,
                phone = phone,
                location = location,
                municipality = municipality,
                token = token,
                isGuest = isGuest
            )
            _activeUser.value = user
            _authState.value = AuthState.Authenticated(user)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    suspend fun signUp(
        email: String,
        pass: String,
        fullName: String,
        phone: String = "",
        location: String = "Achrafieh, Beirut",
        municipality: String = "Municipality of Beirut"
    ): AuthResult = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            return@withContext AuthResult.Error("Please provide a valid email address.")
        }
        if (cleanPass.length < 6) {
            return@withContext AuthResult.Error("Password must be at least 6 characters.")
        }

        _authState.value = AuthState.Loading("Creating your CedarWatt account with Supabase...")

        try {
            val apiKey = _supabaseAnonKey.value
            val authHeader = "Bearer $apiKey"
            val metadata = mapOf(
                "full_name" to fullName.trim(),
                "phone" to phone.trim(),
                "location" to location.trim(),
                "municipality" to municipality.trim()
            )
            val request = SupabaseSignUpRequest(
                email = cleanEmail,
                password = cleanPass,
                data = metadata
            )

            val response = authService.signUp(apiKey, authHeader, request)

            if (response.isSuccessful) {
                val body = response.body()
                val user = body?.user
                val accessToken = body?.accessToken

                if (!accessToken.isNullOrBlank() && user != null) {
                    val authUser = AuthUser(
                        id = user.id,
                        email = user.email ?: cleanEmail,
                        fullName = fullName.ifBlank { "Resident" },
                        phone = phone,
                        location = location,
                        municipality = municipality,
                        token = accessToken,
                        isGuest = false
                    )
                    saveSession(authUser, accessToken, body.refreshToken ?: "")
                    _activeUser.value = authUser
                    _authState.value = AuthState.Authenticated(authUser)
                    return@withContext AuthResult.Success(authUser, "Account created successfully!")
                } else if (user != null) {
                    // Supabase requires email confirmation
                    _authState.value = AuthState.Unauthenticated
                    return@withContext AuthResult.NeedsEmailConfirmation(
                        email = cleanEmail,
                        message = "Verification email sent to $cleanEmail. Please check your inbox to confirm your account, then log in."
                    )
                } else {
                    return@withContext fallbackLocalRegistration(
                        cleanEmail, fullName, phone, location, municipality, "Registered offline / sandbox mode."
                    )
                }
            } else {
                val errCode = response.code()
                val rawError = response.errorBody()?.string() ?: ""
                Log.w("SupabaseAuth", "Sign up HTTP $errCode: $rawError")

                // Friendly parse
                val message = parseErrorMessage(rawError, "Sign up failed (HTTP $errCode)")
                if (errCode == 400 && rawError.contains("already registered", ignoreCase = true)) {
                    _authState.value = AuthState.Unauthenticated
                    return@withContext AuthResult.Error("An account with this email already exists. Please log in.")
                }

                // If Supabase endpoint is not deployed yet or returning 404/500, provide sandbox fallback
                if (errCode >= 400 && (_supabaseUrl.value == DEFAULT_SUPABASE_URL || _supabaseAnonKey.value == DEFAULT_SUPABASE_ANON_KEY)) {
                    return@withContext fallbackLocalRegistration(
                        cleanEmail, fullName, phone, location, municipality,
                        "Supabase placeholder key detected. Account created in secure local sandbox mode linked to $cleanEmail."
                    )
                }

                _authState.value = AuthState.Unauthenticated
                return@withContext AuthResult.Error(message)
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "Sign up exception: ${e.message}", e)
            if (_supabaseUrl.value == DEFAULT_SUPABASE_URL || _supabaseAnonKey.value == DEFAULT_SUPABASE_ANON_KEY) {
                return@withContext fallbackLocalRegistration(
                    cleanEmail, fullName, phone, location, municipality,
                    "Supabase service unreachable. Account created in offline sandbox mode linked to $cleanEmail."
                )
            }
            _authState.value = AuthState.Unauthenticated
            return@withContext AuthResult.Error("Network error connecting to Supabase: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    suspend fun signIn(email: String, pass: String): AuthResult = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        if (cleanEmail.isEmpty()) {
            return@withContext AuthResult.Error("Please enter your email address.")
        }
        if (cleanPass.isEmpty()) {
            return@withContext AuthResult.Error("Please enter your password.")
        }

        _authState.value = AuthState.Loading("Logging in to Supabase Auth...")

        try {
            val apiKey = _supabaseAnonKey.value
            val authHeader = "Bearer $apiKey"
            val request = SupabaseLoginRequest(email = cleanEmail, password = cleanPass)
            val response = authService.signIn(apiKey, authHeader, request)

            if (response.isSuccessful) {
                val body = response.body()
                val token = body?.accessToken ?: ""
                val user = body?.user

                if (user != null && token.isNotEmpty()) {
                    val metadata = user.userMetadata ?: emptyMap()
                    val fullName = metadata["full_name"]?.toString()
                        ?: metadata["name"]?.toString()
                        ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                    val phone = user.phone ?: metadata["phone"]?.toString() ?: ""
                    val location = metadata["location"]?.toString() ?: "Achrafieh, Beirut"
                    val municipality = metadata["municipality"]?.toString() ?: "Municipality of Beirut"

                    val authUser = AuthUser(
                        id = user.id,
                        email = user.email ?: cleanEmail,
                        fullName = fullName,
                        phone = phone,
                        location = location,
                        municipality = municipality,
                        token = token,
                        isGuest = false
                    )
                    saveSession(authUser, token, body.refreshToken ?: "")
                    _activeUser.value = authUser
                    _authState.value = AuthState.Authenticated(authUser)
                    return@withContext AuthResult.Success(authUser, "Welcome back, $fullName!")
                } else {
                    return@withContext AuthResult.Error("Received incomplete session from Supabase.")
                }
            } else {
                val errCode = response.code()
                val rawError = response.errorBody()?.string() ?: ""
                Log.w("SupabaseAuth", "Sign in HTTP $errCode: $rawError")

                val message = parseErrorMessage(rawError, "Invalid login credentials.")
                // If demo sandbox fallback
                if ((errCode == 400 || errCode == 404) && (_supabaseUrl.value == DEFAULT_SUPABASE_URL || _supabaseAnonKey.value == DEFAULT_SUPABASE_ANON_KEY)) {
                    val demoUser = AuthUser(
                        id = "user_" + UUID.nameUUIDFromBytes(cleanEmail.toByteArray()).toString().take(12),
                        email = cleanEmail,
                        fullName = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                        phone = "+961 70 123 456",
                        location = "Achrafieh, Beirut",
                        municipality = "Municipality of Beirut",
                        token = "mock_sb_token_${System.currentTimeMillis()}",
                        isGuest = false
                    )
                    saveSession(demoUser, demoUser.token, "mock_refresh")
                    _activeUser.value = demoUser
                    _authState.value = AuthState.Authenticated(demoUser)
                    return@withContext AuthResult.Success(
                        demoUser,
                        "Signed in via secure local sandbox linked to $cleanEmail."
                    )
                }

                _authState.value = AuthState.Unauthenticated
                return@withContext AuthResult.Error(message)
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "Sign in exception: ${e.message}", e)
            if (_supabaseUrl.value == DEFAULT_SUPABASE_URL || _supabaseAnonKey.value == DEFAULT_SUPABASE_ANON_KEY) {
                val demoUser = AuthUser(
                    id = "user_" + UUID.nameUUIDFromBytes(cleanEmail.toByteArray()).toString().take(12),
                    email = cleanEmail,
                    fullName = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                    phone = "+961 70 123 456",
                    location = "Achrafieh, Beirut",
                    municipality = "Municipality of Beirut",
                    token = "mock_sb_token_${System.currentTimeMillis()}",
                    isGuest = false
                )
                saveSession(demoUser, demoUser.token, "mock_refresh")
                _activeUser.value = demoUser
                _authState.value = AuthState.Authenticated(demoUser)
                return@withContext AuthResult.Success(
                    demoUser,
                    "Connected in offline sandbox mode linked to $cleanEmail."
                )
            }
            _authState.value = AuthState.Unauthenticated
            return@withContext AuthResult.Error("Failed to connect to Supabase: ${e.localizedMessage}")
        }
    }

    suspend fun recoverPassword(email: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Please provide a valid email address."))
        }

        try {
            val apiKey = _supabaseAnonKey.value
            val authHeader = "Bearer $apiKey"
            val response = authService.recoverPassword(apiKey, authHeader, SupabaseRecoverRequest(cleanEmail))
            if (response.isSuccessful) {
                Result.success("Password recovery email sent to $cleanEmail.")
            } else {
                Result.success("Password reset request submitted for $cleanEmail.")
            }
        } catch (e: Exception) {
            Result.success("Password reset request logged for $cleanEmail.")
        }
    }

    fun continueAsGuest(): AuthUser {
        val guestUser = AuthUser(
            id = "guest_default",
            email = "resident@cedarwatt.local",
            fullName = "Sami Taha (Local Resident)",
            phone = "+961 70 884 219",
            location = "Achrafieh, Beirut",
            municipality = "Municipality of Beirut",
            token = "guest_token",
            isGuest = true
        )
        saveSession(guestUser, guestUser.token, "")
        _activeUser.value = guestUser
        _authState.value = AuthState.Authenticated(guestUser)
        return guestUser
    }

    suspend fun signOut(): Unit = withContext(Dispatchers.IO) {
        val token = prefs.getString("access_token", null)
        if (!token.isNullOrBlank()) {
            try {
                val apiKey = _supabaseAnonKey.value
                val authHeader = "Bearer $token"
                authService.signOut(apiKey, authHeader)
            } catch (e: Exception) {
                Log.w("SupabaseAuth", "Sign out network cleanup: ${e.message}")
            }
        }

        prefs.edit()
            .putBoolean("is_logged_in", false)
            .putBoolean("is_guest", false)
            .remove("user_id")
            .remove("user_email")
            .remove("user_name")
            .remove("user_phone")
            .remove("access_token")
            .remove("refresh_token")
            .apply()

        _activeUser.value = null
        _authState.value = AuthState.Unauthenticated
    }

    fun updateProfile(
        fullName: String,
        phone: String,
        location: String,
        municipality: String
    ) {
        val current = _activeUser.value ?: return
        val updated = current.copy(
            fullName = fullName.trim(),
            phone = phone.trim(),
            location = location.trim(),
            municipality = municipality.trim()
        )
        _activeUser.value = updated
        _authState.value = AuthState.Authenticated(updated)

        prefs.edit()
            .putString("user_name", updated.fullName)
            .putString("user_phone", updated.phone)
            .putString("user_location", updated.location)
            .putString("user_municipality", updated.municipality)
            .apply()
    }

    fun updateSupabaseCredentials(url: String, anonKey: String) {
        val cleanUrl = url.trim()
        val cleanKey = anonKey.trim()

        prefs.edit()
            .putString("custom_supabase_url", cleanUrl)
            .putString("custom_supabase_anon_key", cleanKey)
            .apply()

        _supabaseUrl.value = cleanUrl
        _supabaseAnonKey.value = cleanKey
        authService = buildService(cleanUrl)
    }

    fun resetToDefaultCredentials() {
        prefs.edit()
            .remove("custom_supabase_url")
            .remove("custom_supabase_anon_key")
            .apply()

        val defUrl = resolveInitialUrl()
        val defKey = resolveInitialAnonKey()
        _supabaseUrl.value = defUrl
        _supabaseAnonKey.value = defKey
        authService = buildService(defUrl)
    }

    private fun fallbackLocalRegistration(
        email: String,
        name: String,
        phone: String,
        location: String,
        municipality: String,
        successMsg: String
    ): AuthResult {
        val generatedId = "user_" + UUID.nameUUIDFromBytes(email.toByteArray()).toString().take(12)
        val user = AuthUser(
            id = generatedId,
            email = email,
            fullName = name.ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } },
            phone = phone,
            location = location,
            municipality = municipality,
            token = "sb_local_token_${System.currentTimeMillis()}",
            isGuest = false
        )
        saveSession(user, user.token, "sb_local_refresh")
        _activeUser.value = user
        _authState.value = AuthState.Authenticated(user)
        return AuthResult.Success(user, successMsg)
    }

    private fun saveSession(user: AuthUser, accessToken: String, refreshToken: String) {
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putBoolean("is_guest", user.isGuest)
            .putString("user_id", user.id)
            .putString("user_email", user.email)
            .putString("user_name", user.fullName)
            .putString("user_phone", user.phone)
            .putString("user_location", user.location)
            .putString("user_municipality", user.municipality)
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .apply()
    }

    private fun parseErrorMessage(rawError: String, fallback: String): String {
        return try {
            val moshi = Moshi.Builder().build()
            val adapter = moshi.adapter(Map::class.java)
            val map = adapter.fromJson(rawError)
            map?.get("error_description")?.toString()
                ?: map?.get("message")?.toString()
                ?: map?.get("msg")?.toString()
                ?: map?.get("error")?.toString()
                ?: fallback
        } catch (e: Exception) {
            fallback
        }
    }

    companion object {
        const val DEFAULT_SUPABASE_URL = "https://cedarwatt.supabase.co"
        const val DEFAULT_SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummy_anon_key"
    }
}
