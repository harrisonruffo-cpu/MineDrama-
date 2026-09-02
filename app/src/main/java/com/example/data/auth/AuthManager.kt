package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.Appwrite
import com.example.data.model.UserProfile
import com.example.data.util.YouTubeHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthManager(private val context: Context) {
    private val TAG = "AuthManager"
    private val prefs: SharedPreferences =
        context.getSharedPreferences("litoral_auth_prefs", Context.MODE_PRIVATE)

    companion object {
        const val ADMIN_EMAIL = "harrisonruffo@gmail.com"
        const val ADMIN_NAME = "Harrison Ruffo"
        const val ADMIN_DRIVE_PHOTO = "https://drive.google.com/file/d/1VWIfZ8lcuPWCc2ijTwvX6WoWnbqkUpO7/view?usp=drivesdk"
        const val ADMIN_AVATAR = "https://lh3.googleusercontent.com/u/0/d/1VWIfZ8lcuPWCc2ijTwvX6WoWnbqkUpO7"
        const val ADMIN_ROLE = "Desenvolvedor & ADM Oficial"
        private const val BASE_FOLLOWERS = 28450
        private const val KEY_FOLLOWERS_EXTRA = "followers_extra_registered_v1"
        private const val KEY_USER_VISITED = "user_visited_and_followed_v1"
    }

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    init {
        registerVisitorAsFollower()
        loadUser()
    }

    fun getFollowersCount(): Int {
        val extra = prefs.getInt(KEY_FOLLOWERS_EXTRA, 1)
        return BASE_FOLLOWERS + extra
    }

    private fun registerVisitorAsFollower() {
        val alreadyCounted = prefs.getBoolean(KEY_USER_VISITED, false)
        if (!alreadyCounted) {
            val currentExtra = prefs.getInt(KEY_FOLLOWERS_EXTRA, 0)
            prefs.edit()
                .putInt(KEY_FOLLOWERS_EXTRA, currentExtra + 1)
                .putBoolean(KEY_USER_VISITED, true)
                .apply()
        }
    }

    fun createAdminProfile(): UserProfile {
        return UserProfile(
            id = "admin_harrisonruffo",
            name = ADMIN_NAME,
            email = ADMIN_EMAIL,
            avatarUrl = ADMIN_AVATAR,
            coinsBalance = 9999,
            isVip = true,
            role = ADMIN_ROLE,
            isAdmin = true,
            followersCount = getFollowersCount(),
            isFollowingAdmin = true
        )
    }

    private fun loadUser() {
        val email = prefs.getString("user_email", null)
        if (!email.isNullOrBlank()) {
            val isAdmin = email.equals(ADMIN_EMAIL, ignoreCase = true)
            val name = if (isAdmin) ADMIN_NAME else (prefs.getString("user_name", "Usuário") ?: "Usuário")
            val rawAvatar = prefs.getString("user_avatar", if (isAdmin) ADMIN_AVATAR else "") ?: ""
            val avatar = if (isAdmin) ADMIN_AVATAR else YouTubeHelper.normalizeAvatarUrl(rawAvatar)
            val coins = if (isAdmin) 9999 else prefs.getInt("user_coins", 150)
            val isVip = if (isAdmin) true else prefs.getBoolean("user_is_vip", true)
            val id = if (isAdmin) "admin_harrisonruffo" else (prefs.getString("user_id", "user_${Math.abs(email.hashCode())}") ?: "user_${Math.abs(email.hashCode())}")
            
            _currentUser.value = UserProfile(
                id = id,
                name = name,
                email = email,
                avatarUrl = avatar,
                coinsBalance = coins,
                isVip = isVip,
                role = if (isAdmin) ADMIN_ROLE else "Membro Oficial • Seguidor do ADM",
                isAdmin = isAdmin,
                followersCount = getFollowersCount(),
                isFollowingAdmin = true
            )
            Log.d(TAG, "Usuário autenticado carregado da sessão: $email ($name) - isAdmin=$isAdmin")
        } else {
            // Perfil Principal padrão do aplicativo: Harrison Ruffo (Desenvolvedor & ADM Oficial)
            val admin = createAdminProfile()
            _currentUser.value = admin
            Log.d(TAG, "Perfil Principal padrão carregado: ${admin.name} (${admin.email}) como ${admin.role}")
        }
    }

    fun loginWithGoogleAccount(googleAccount: GoogleUserAccount) {
        val isAdmin = googleAccount.email.equals(ADMIN_EMAIL, ignoreCase = true)
        val userId = if (isAdmin) "admin_harrisonruffo" else googleAccount.id.ifBlank { "google_${Math.abs(googleAccount.email.hashCode())}" }
        val avatar = if (isAdmin) ADMIN_AVATAR else (googleAccount.photoUrl?.ifBlank {
            "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80"
        } ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80")
        val name = if (isAdmin) ADMIN_NAME else googleAccount.name

        prefs.edit()
            .putString("user_id", userId)
            .putString("user_email", googleAccount.email)
            .putString("user_name", name)
            .putString("user_avatar", avatar)
            .putInt("user_coins", if (isAdmin) 9999 else 300)
            .putBoolean("user_is_vip", true)
            .putString("auth_provider", "google")
            .apply()

        val profile = UserProfile(
            id = userId,
            name = name,
            email = googleAccount.email,
            avatarUrl = avatar,
            coinsBalance = if (isAdmin) 9999 else 300,
            isVip = true,
            role = if (isAdmin) ADMIN_ROLE else "Membro Oficial • Seguidor do ADM",
            isAdmin = isAdmin,
            followersCount = getFollowersCount(),
            isFollowingAdmin = true
        )
        _currentUser.value = profile
        syncWithCloud(profile)
        Log.d(TAG, "Login com Conta Google oficial: ${googleAccount.email} ($name) - isAdmin=$isAdmin")
    }

    fun loginWithGoogle(
        name: String = "Usuário Google",
        email: String = "usuario@gmail.com",
        avatarUrl: String = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80"
    ) {
        val isAdmin = email.equals(ADMIN_EMAIL, ignoreCase = true)
        val userId = if (isAdmin) "admin_harrisonruffo" else "google_${Math.abs(email.hashCode())}"
        val finalAvatar = if (isAdmin) ADMIN_AVATAR else avatarUrl
        val finalName = if (isAdmin) ADMIN_NAME else name

        prefs.edit()
            .putString("user_id", userId)
            .putString("user_email", email)
            .putString("user_name", finalName)
            .putString("user_avatar", finalAvatar)
            .putInt("user_coins", if (isAdmin) 9999 else 300)
            .putBoolean("user_is_vip", true)
            .putString("auth_provider", "google")
            .apply()

        val profile = UserProfile(
            id = userId,
            name = finalName,
            email = email,
            avatarUrl = finalAvatar,
            coinsBalance = if (isAdmin) 9999 else 300,
            isVip = true,
            role = if (isAdmin) ADMIN_ROLE else "Membro Oficial • Seguidor do ADM",
            isAdmin = isAdmin,
            followersCount = getFollowersCount(),
            isFollowingAdmin = true
        )
        _currentUser.value = profile
        syncWithCloud(profile)
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
            .putString("auth_provider", "facebook")
            .apply()

        val profile = UserProfile(
            id = userId,
            name = name,
            email = email,
            avatarUrl = avatarUrl,
            coinsBalance = 300,
            isVip = true,
            role = "Membro Oficial • Seguidor do ADM",
            isAdmin = false,
            followersCount = getFollowersCount(),
            isFollowingAdmin = true
        )
        _currentUser.value = profile
        syncWithCloud(profile)
    }

    fun login(name: String, email: String) {
        val isAdmin = email.equals(ADMIN_EMAIL, ignoreCase = true)
        val finalAvatar = if (isAdmin) ADMIN_AVATAR else "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80"
        val finalName = if (isAdmin) ADMIN_NAME else name
        val userId = if (isAdmin) "admin_harrisonruffo" else "user_${Math.abs(email.hashCode())}"
        prefs.edit()
            .putString("user_id", userId)
            .putString("user_email", email)
            .putString("user_name", finalName)
            .putString("user_avatar", finalAvatar)
            .putInt("user_coins", if (isAdmin) 9999 else 150)
            .putBoolean("user_is_vip", isAdmin)
            .putString("auth_provider", "email")
            .apply()

        val profile = UserProfile(
            id = userId,
            name = finalName,
            email = email,
            avatarUrl = finalAvatar,
            coinsBalance = if (isAdmin) 9999 else 150,
            isVip = isAdmin,
            role = if (isAdmin) ADMIN_ROLE else "Membro Oficial • Seguidor do ADM",
            isAdmin = isAdmin,
            followersCount = getFollowersCount(),
            isFollowingAdmin = true
        )
        _currentUser.value = profile
        syncWithCloud(profile)
    }

    fun loginAsGuest() {
        val guestId = "guest_${System.currentTimeMillis() % 10000}"
        val user = UserProfile(
            id = guestId,
            name = "Convidado",
            email = "convidado@litoralnovelas.com",
            avatarUrl = "",
            coinsBalance = 100,
            isVip = false,
            role = "Convidado • Seguidor do ADM",
            isAdmin = false,
            followersCount = getFollowersCount(),
            isFollowingAdmin = true
        )
        prefs.edit()
            .putString("user_id", user.id)
            .putString("user_email", user.email)
            .putString("user_name", user.name)
            .putString("user_avatar", "")
            .putInt("user_coins", 100)
            .putBoolean("user_is_vip", false)
            .putString("auth_provider", "guest")
            .apply()
        _currentUser.value = user
    }

    fun updateProfile(name: String, avatarUrl: String) {
        val current = _currentUser.value ?: return
        val normalizedAvatar = YouTubeHelper.normalizeAvatarUrl(avatarUrl)
        val updated = current.copy(name = name, avatarUrl = normalizedAvatar)
        prefs.edit()
            .putString("user_name", name)
            .putString("user_avatar", normalizedAvatar)
            .apply()
        _currentUser.value = updated
        syncWithCloud(updated)
        Log.d(TAG, "Perfil atualizado: $name, avatar=$normalizedAvatar")
    }

    fun logout() {
        prefs.edit().clear().apply()
        _currentUser.value = createAdminProfile()
    }

    private fun syncWithCloud(user: UserProfile) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!Appwrite.isInitialized) {
                    Appwrite.init(context)
                }
                Appwrite.ensureSession()
                Log.d(TAG, "Sincronização de perfil com a nuvem iniciada para ${user.email}")
            } catch (e: Throwable) {
                Log.w(TAG, "Falha na sincronização de perfil na nuvem: ${e.message}")
            }
        }
    }
}
