package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager(context: Context) {
    private val TAG = "AuthManager"
    private val prefs: SharedPreferences =
        context.getSharedPreferences("litoral_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        val email = prefs.getString("user_email", null)
        if (!email.isNullOrBlank()) {
            val name = prefs.getString("user_name", "Usuário") ?: "Usuário"
            val avatar = prefs.getString("user_avatar", "") ?: ""
            val coins = prefs.getInt("user_coins", 150)
            val isVip = prefs.getBoolean("user_is_vip", true)
            val id = prefs.getString("user_id", "user_${Math.abs(email.hashCode())}") ?: "user_${Math.abs(email.hashCode())}"
            _currentUser.value = UserProfile(
                id = id,
                name = name,
                email = email,
                avatarUrl = avatar,
                coinsBalance = coins,
                isVip = isVip
            )
            Log.d(TAG, "Usuário autenticado carregado da sessão: $email ($name)")
        }
    }

    fun loginWithGoogle(
        name: String = "Usuário Google",
        email: String = "usuario@gmail.com",
        avatarUrl: String = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80"
    ) {
        val userId = "google_${Math.abs(email.hashCode())}"
        prefs.edit()
            .putString("user_id", userId)
            .putString("user_email", email)
            .putString("user_name", name)
            .putString("user_avatar", avatarUrl)
            .putInt("user_coins", 300)
            .putBoolean("user_is_vip", true)
            .apply()

        _currentUser.value = UserProfile(
            id = userId,
            name = name,
            email = email,
            avatarUrl = avatarUrl,
            coinsBalance = 300,
            isVip = true
        )
        Log.d(TAG, "Login com Google realizado com sucesso: $email")
    }

    fun loginWithFacebook(
        name: String = "Usuário Facebook",
        email: String = "usuario.fb@facebook.com",
        avatarUrl: String = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80"
    ) {
        val userId = "fb_${Math.abs(email.hashCode())}"
        prefs.edit()
            .putString("user_id", userId)
            .putString("user_email", email)
            .putString("user_name", name)
            .putString("user_avatar", avatarUrl)
            .putInt("user_coins", 300)
            .putBoolean("user_is_vip", true)
            .apply()

        _currentUser.value = UserProfile(
            id = userId,
            name = name,
            email = email,
            avatarUrl = avatarUrl,
            coinsBalance = 300,
            isVip = true
        )
    }

    fun login(name: String, email: String) {
        val avatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80"
        val userId = "user_${Math.abs(email.hashCode())}"
        prefs.edit()
            .putString("user_id", userId)
            .putString("user_email", email)
            .putString("user_name", name)
            .putString("user_avatar", avatar)
            .putInt("user_coins", 150)
            .putBoolean("user_is_vip", false)
            .apply()

        _currentUser.value = UserProfile(
            id = userId,
            name = name,
            email = email,
            avatarUrl = avatar,
            coinsBalance = 150,
            isVip = false
        )
    }

    fun loginAsGuest() {
        val guestId = "guest_${System.currentTimeMillis() % 10000}"
        val user = UserProfile(
            id = guestId,
            name = "Convidado",
            email = "convidado@litoralnovelas.com",
            avatarUrl = "",
            coinsBalance = 100,
            isVip = false
        )
        prefs.edit()
            .putString("user_id", user.id)
            .putString("user_email", user.email)
            .putString("user_name", user.name)
            .putString("user_avatar", "")
            .putInt("user_coins", 100)
            .putBoolean("user_is_vip", false)
            .apply()
        _currentUser.value = user
    }

    fun logout() {
        prefs.edit().clear().apply()
        _currentUser.value = null
    }
}
