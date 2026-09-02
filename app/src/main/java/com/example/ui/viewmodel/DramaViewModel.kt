package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthManager
import com.example.data.auth.GoogleAuthHelper
import com.example.data.auth.GoogleUserAccount
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteEntity
import com.example.data.local.HistoryEntity
import com.example.data.model.Drama
import com.example.data.model.Episode
import com.example.data.remote.CloudDiagnosticManager
import com.example.data.remote.CloudDiagnosticResult
import com.example.data.remote.RealDramaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DramaViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "DramaViewModel"
    private val repository = RealDramaRepository(application)
    private val authManager = AuthManager(application)
    val googleAuthHelper = GoogleAuthHelper(application)
    private val db = AppDatabase.getDatabase(application)
    private val diagnosticManager = CloudDiagnosticManager(application)

    val currentUser = authManager.currentUser

    private val _allDramas = MutableStateFlow<List<Drama>>(emptyList())
    val allDramas: StateFlow<List<Drama>> = _allDramas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentDrama = MutableStateFlow<Drama?>(null)
    val currentDrama: StateFlow<Drama?> = _currentDrama.asStateFlow()

    private val _currentEpisodeIndex = MutableStateFlow(0)
    val currentEpisodeIndex: StateFlow<Int> = _currentEpisodeIndex.asStateFlow()

    private val _favoriteDramaIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteDramaIds: StateFlow<Set<String>> = _favoriteDramaIds.asStateFlow()

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

    private val _publishMessage = MutableStateFlow<String?>(null)
    val publishMessage: StateFlow<String?> = _publishMessage.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val _diagnosticResult = MutableStateFlow<CloudDiagnosticResult?>(null)
    val diagnosticResult: StateFlow<CloudDiagnosticResult?> = _diagnosticResult.asStateFlow()

    private val _isRunningDiagnostic = MutableStateFlow(false)
    val isRunningDiagnostic: StateFlow<Boolean> = _isRunningDiagnostic.asStateFlow()

    init {
        loadDramas()
        observeFavorites()
    }

    fun loadDramas() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = repository.syncCloudDramas()
                _allDramas.value = list
                if (_currentDrama.value == null && list.isNotEmpty()) {
                    _currentDrama.value = list.first()
                } else if (_currentDrama.value != null) {
                    val updatedCurrent = list.find { it.id == _currentDrama.value?.id }
                    if (updatedCurrent != null) {
                        _currentDrama.value = updatedCurrent
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao carregar dramas: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            try {
                db.dramaDao().getFavoriteIdsFlow()
                    .catch { e ->
                        Log.e(TAG, "Erro ao observar favoritos: ${e.message}")
                        emit(emptyList())
                    }
                    .collect { ids ->
                        _favoriteDramaIds.value = ids.toSet()
                    }
            } catch (e: Throwable) {
                Log.e(TAG, "Erro de inicialização Room favoritos: ${e.message}")
            }
        }
    }

    fun selectDrama(drama: Drama, episodeIndex: Int = 0) {
        _currentDrama.value = drama
        _currentEpisodeIndex.value = episodeIndex.coerceIn(0, (drama.episodes.size - 1).coerceAtLeast(0))
        saveWatchHistory(drama.id, _currentEpisodeIndex.value)
    }

    fun selectEpisode(index: Int) {
        val drama = _currentDrama.value ?: return
        if (index in drama.episodes.indices) {
            _currentEpisodeIndex.value = index
            saveWatchHistory(drama.id, index)
        }
    }

    fun nextEpisode() {
        val drama = _currentDrama.value ?: return
        val nextIdx = _currentEpisodeIndex.value + 1
        if (nextIdx < drama.episodes.size) {
            selectEpisode(nextIdx)
        }
    }

    fun toggleFavorite(dramaId: String) {
        viewModelScope.launch {
            try {
                val isFav = _favoriteDramaIds.value.contains(dramaId)
                if (isFav) {
                    db.dramaDao().removeFavorite(dramaId)
                } else {
                    db.dramaDao().addFavorite(FavoriteEntity(dramaId))
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Erro ao alternar favorito: ${e.message}")
            }
        }
    }

    private fun saveWatchHistory(dramaId: String, episodeNumber: Int) {
        viewModelScope.launch {
            try {
                db.dramaDao().updateHistory(
                    HistoryEntity(
                        dramaId = dramaId,
                        lastEpisodeNumber = episodeNumber + 1,
                        lastPositionMs = 0L
                    )
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Erro ao salvar histórico: ${e.message}")
            }
        }
    }

    fun runCloudDiagnostic() {
        viewModelScope.launch {
            _isRunningDiagnostic.value = true
            try {
                val result = diagnosticManager.runDiagnostic()
                _diagnosticResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Erro no diagnóstico: ${e.message}")
            } finally {
                _isRunningDiagnostic.value = false
            }
        }
    }

    fun clearDiagnosticResult() {
        _diagnosticResult.value = null
    }

    fun publishDrama(
        title: String,
        description: String,
        genre: String,
        coverUrl: String,
        bannerUrl: String,
        episodes: List<Episode>,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _isPublishing.value = true
            try {
                val dramaId = "drama_${System.currentTimeMillis()}"
                val newDrama = Drama(
                    id = dramaId,
                    title = title,
                    description = description,
                    genre = genre,
                    coverUrl = coverUrl,
                    bannerUrl = bannerUrl.ifBlank { coverUrl },
                    totalEpisodes = episodes.size,
                    episodes = episodes.mapIndexed { idx, ep ->
                        ep.copy(
                            id = "ep_${dramaId}_${idx + 1}",
                            dramaId = dramaId,
                            episodeNumber = idx + 1
                        )
                    },
                    isPublishedLocally = true,
                    createdAt = System.currentTimeMillis()
                )
                val success = repository.publishDrama(newDrama)
                if (success) {
                    _publishMessage.value = "Novela publicada com sucesso!"
                    loadDramas()
                } else {
                    _publishMessage.value = "Erro ao publicar novela."
                }
                onComplete(success)
            } catch (e: Exception) {
                _publishMessage.value = "Erro: ${e.message}"
                onComplete(false)
            } finally {
                _isPublishing.value = false
            }
        }
    }

    fun updateDramaTitle(dramaId: String, newTitle: String) {
        viewModelScope.launch {
            val list = _allDramas.value.toMutableList()
            val index = list.indexOfFirst { it.id == dramaId }
            if (index >= 0) {
                val updated = list[index].copy(title = newTitle)
                repository.publishDrama(updated)
                loadDramas()
            }
        }
    }

    fun login(name: String, email: String) {
        authManager.login(name, email)
    }

    fun loginWithGoogleAccount(account: GoogleUserAccount) {
        authManager.loginWithGoogleAccount(account)
    }

    fun loginWithGoogle(
        name: String = "Usuário Google",
        email: String = "usuario@gmail.com",
        avatarUrl: String = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80"
    ) {
        authManager.loginWithGoogle(name, email, avatarUrl)
    }

    fun loginWithFacebook(
        name: String = "Usuário Facebook",
        email: String = "usuario.fb@facebook.com",
        avatarUrl: String = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80"
    ) {
        authManager.loginWithFacebook(name, email, avatarUrl)
    }

    fun loginAsGuest() {
        authManager.loginAsGuest()
    }

    fun logout() {
        authManager.logout()
    }
}
