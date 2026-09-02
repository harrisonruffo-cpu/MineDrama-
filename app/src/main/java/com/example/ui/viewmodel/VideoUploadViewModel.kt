package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.AppwriteStorageManager
import com.example.data.util.MediaStorageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoUploadViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "VideoUploadVM"
    private val appwriteStorage = AppwriteStorageManager(application)

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val _uploadStatusMessage = MutableStateFlow<String?>(null)
    val uploadStatusMessage: StateFlow<String?> = _uploadStatusMessage.asStateFlow()

    fun uploadVideo(
        uri: Uri,
        fileName: String,
        onResult: (cloudUrl: String?, localPath: String?) -> Unit
    ) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadStatusMessage.value = "Salvando vídeo no dispositivo..."
            try {
                val localPath = MediaStorageHelper.copyUriToLocalStorage(
                    getApplication(),
                    uri,
                    "video_${System.currentTimeMillis()}_$fileName"
                )

                _uploadStatusMessage.value = "Enviando vídeo para a Nuvem Appwrite..."
                val cloudUrl = appwriteStorage.uploadVideo(uri, fileName) { progress ->
                    _uploadProgress.value = progress
                }

                _uploadStatusMessage.value = if (cloudUrl != null) "Upload na nuvem concluído!" else "Salvo com segurança no dispositivo."
                Log.d(TAG, "Vídeo processado: cloudUrl=$cloudUrl, localPath=$localPath")
                onResult(cloudUrl, localPath)
            } catch (e: Exception) {
                Log.e(TAG, "Erro no upload do vídeo: ${e.message}")
                _uploadStatusMessage.value = "Erro no upload: ${e.message}"
                onResult(null, null)
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun uploadCover(
        uri: Uri,
        fileName: String,
        onResult: (coverUrl: String?) -> Unit
    ) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadStatusMessage.value = "Processando capa..."
            try {
                val cloudUrl = appwriteStorage.uploadCover(uri, fileName)
                val finalUrl = cloudUrl ?: MediaStorageHelper.copyUriToLocalStorage(
                    getApplication(),
                    uri,
                    "cover_${System.currentTimeMillis()}_$fileName"
                )
                Log.d(TAG, "Capa processada: $finalUrl")
                onResult(finalUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Erro no upload da capa: ${e.message}")
                onResult(null)
            } finally {
                _isUploading.value = false
            }
        }
    }
}
