package com.example

import android.content.Context
import android.util.Log
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Appwrite {
    private const val TAG = "AppwriteManager"

    // Região selecionada pelo usuário: NYC Cloud
    const val ENDPOINT = "https://nyc.cloud.appwrite.io/v1"
    const val PROJECT_ID = "6a973ca90022ac6c7069"
    const val PROJECT_NAME = "Litoral Novelas"

    // Database e Coleções
    const val DATABASE_ID = "litoral_novelas"
    const val COLLECTION_DRAMAS = "dramas"
    const val COLLECTION_EPISODES = "episodes"

    // Bucket de Storage Unificado
    var BUCKET_MEDIA = "videos"
    val BUCKET_VIDEOS: String get() = BUCKET_MEDIA
    val BUCKET_COVERS: String get() = BUCKET_MEDIA

    private var _client: Client? = null
    val client: Client? get() = _client

    private var _account: Account? = null
    val account: Account? get() = _account

    private var _databases: Databases? = null
    val databases: Databases? get() = _databases

    private var _storage: Storage? = null
    val storage: Storage? get() = _storage

    var isInitialized = false
        private set

    fun init(context: Context) {
        if (isInitialized && _client != null) return
        try {
            val c = Client(context.applicationContext)
                .setEndpoint(ENDPOINT)
                .setProject(PROJECT_ID)
            _client = c
            _account = Account(c)
            _databases = Databases(c)
            _storage = Storage(c)
            isInitialized = true
            Log.d(TAG, "Appwrite inicializado com sucesso no endpoint $ENDPOINT (Project: $PROJECT_ID)")
        } catch (e: Throwable) {
            Log.e(TAG, "Erro ao inicializar Appwrite: ${e.message}", e)
        }
    }

    suspend fun ensureSession(): Boolean = withContext(Dispatchers.IO) {
        val acc = _account ?: return@withContext false
        try {
            try {
                val current = acc.get()
                Log.d(TAG, "Sessão ativa existente: ${current.id}")
                return@withContext true
            } catch (_: Throwable) {
                // Tenta criar sessão anônima
            }
            val session = acc.createAnonymousSession()
            Log.d(TAG, "Nova sessão anônima criada: ${session.id}")
            true
        } catch (e: Throwable) {
            Log.w(TAG, "Não foi possível criar sessão anônima: ${e.message}")
            true
        }
    }

    fun getFileViewUrl(bucketId: String, fileId: String): String {
        return "$ENDPOINT/storage/buckets/$bucketId/files/$fileId/view?project=$PROJECT_ID"
    }

    fun getFileDownloadUrl(bucketId: String, fileId: String): String {
        return "$ENDPOINT/storage/buckets/$bucketId/files/$fileId/download?project=$PROJECT_ID"
    }
}
