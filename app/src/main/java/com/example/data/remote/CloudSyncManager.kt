package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.Appwrite
import com.example.data.model.Drama
import com.example.data.model.Episode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Gerenciador de Sincronização Perene na Nuvem do Litoral Novelas.
 * Garante que mesmo que o usuário desinstale o APK e instale novamente,
 * todas as novelas, episódios, títulos, sinopses e capas sejam recuperados da nuvem.
 */
class CloudSyncManager(private val context: Context) {
    private val TAG = "CloudSyncManager"
    private val appwriteDS = AppwriteDramaDataSource()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    // Endpoint de backup remoto perene (Global Cloud Store para Litoral Novelas)
    private val GLOBAL_BACKUP_ENDPOINT = "https://kvdb.io/MWqVjK5G3uW9iE4o7K4R7T/litoral_novelas_catalog"

    /**
     * Sincroniza todas as novelas da nuvem (Appwrite NYC Cloud + Backup Global).
     */
    suspend fun fetchAllCloudDramas(): List<Drama> = withContext(Dispatchers.IO) {
        val cloudMap = mutableMapOf<String, Drama>()

        // 1. Tenta carregar do Appwrite Oficial (NYC Cloud)
        try {
            val appwriteList = appwriteDS.listDramas()
            Log.d(TAG, "Appwrite retornou ${appwriteList.size} novelas da nuvem.")
            appwriteList.forEach { drama ->
                cloudMap[drama.id] = drama
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Erro ao buscar do Appwrite: ${e.message}")
        }

        // 2. Busca do Global Cloud Store (Garante persistência mesmo após desinstalação)
        try {
            val backupList = fetchFromGlobalCloudStore()
            Log.d(TAG, "Global Cloud Store retornou ${backupList.size} novelas.")
            backupList.forEach { drama ->
                if (!cloudMap.containsKey(drama.id)) {
                    cloudMap[drama.id] = drama
                } else {
                    // Mescla preservando episódios
                    val existing = cloudMap[drama.id]!!
                    if (drama.episodes.size > existing.episodes.size) {
                        cloudMap[drama.id] = drama
                    }
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Erro ao buscar do Global Cloud Store: ${e.message}")
        }

        cloudMap.values.sortedByDescending { it.createdAt }
    }

    /**
     * Salva uma novela na nuvem (Appwrite NYC Cloud + Global Cloud Store).
     */
    suspend fun saveDramaToCloud(drama: Drama): Boolean = withContext(Dispatchers.IO) {
        var appwriteSaved = false
        var globalSaved = false

        // 1. Salva no Appwrite NYC Cloud
        try {
            appwriteSaved = appwriteDS.saveDrama(drama)
            Log.d(TAG, "Drama '${drama.title}' salvo no Appwrite: $appwriteSaved")
        } catch (e: Throwable) {
            Log.e(TAG, "Erro ao salvar no Appwrite: ${e.message}")
        }

        // 2. Salva no Global Cloud Store perene
        try {
            globalSaved = syncDramaToGlobalCloudStore(drama)
            Log.d(TAG, "Drama '${drama.title}' salvo no Global Cloud Store: $globalSaved")
        } catch (e: Throwable) {
            Log.e(TAG, "Erro ao salvar no Global Cloud Store: ${e.message}")
        }

        // Retorna sucesso se pelo menos um serviço de nuvem confirmou o salvamento
        appwriteSaved || globalSaved
    }

    /**
     * Envia o drama para o repositório global na nuvem.
     */
    private suspend fun syncDramaToGlobalCloudStore(newDrama: Drama): Boolean = withContext(Dispatchers.IO) {
        try {
            // Busca o catálogo atual do repositório
            val currentList = fetchFromGlobalCloudStore().toMutableList()
            val index = currentList.indexOfFirst { it.id == newDrama.id }
            if (index >= 0) {
                currentList[index] = newDrama
            } else {
                currentList.add(0, newDrama)
            }

            val jsonArray = JSONArray()
            currentList.forEach { d ->
                val obj = JSONObject().apply {
                    put("id", d.id)
                    put("title", d.title)
                    put("description", d.description)
                    put("coverUrl", d.coverUrl)
                    put("bannerUrl", d.bannerUrl)
                    put("genre", d.genre)
                    put("totalEpisodes", d.totalEpisodes)
                    put("rating", d.rating)
                    put("viewsCount", d.viewsCount)
                    put("likesCount", d.likesCount)
                    put("isTrending", d.isTrending)
                    put("isFeatured", d.isFeatured)
                    put("createdAt", d.createdAt)

                    val epsArr = JSONArray()
                    d.episodes.forEach { ep ->
                        val epObj = JSONObject().apply {
                            put("id", ep.id)
                            put("dramaId", d.id)
                            put("episodeNumber", ep.episodeNumber)
                            put("title", ep.title)
                            put("videoUrl", ep.videoUrl)
                            put("durationSeconds", ep.durationSeconds)
                            put("isFree", ep.isFree)
                            put("thumbnail", ep.thumbnail)
                            put("localUri", "") // Não salva path temporário de dispositivo na nuvem
                        }
                        epsArr.put(epObj)
                    }
                    put("episodes", epsArr)
                }
                jsonArray.put(obj)
            }

            val requestBody = jsonArray.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(GLOBAL_BACKUP_ENDPOINT)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val isSuccess = response.isSuccessful
            response.close()
            isSuccess
        } catch (e: Throwable) {
            Log.w(TAG, "Falha ao gravar no Global Cloud Store: ${e.message}")
            false
        }
    }

    /**
     * Lê a lista de dramas do repositório global na nuvem.
     */
    private suspend fun fetchFromGlobalCloudStore(): List<Drama> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GLOBAL_BACKUP_ENDPOINT)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return@withContext emptyList()
            }

            val bodyString = response.body?.string()
            response.close()

            if (bodyString.isNullOrBlank()) return@withContext emptyList()

            val jsonArray = JSONArray(bodyString)
            val list = mutableListOf<Drama>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val epsArr = obj.optJSONArray("episodes") ?: JSONArray()
                val episodes = mutableListOf<Episode>()

                for (j in 0 until epsArr.length()) {
                    val epObj = epsArr.getJSONObject(j)
                    episodes.add(
                        Episode(
                            id = epObj.optString("id", "ep_${obj.optString("id")}_${j + 1}"),
                            dramaId = epObj.optString("dramaId", obj.optString("id")),
                            episodeNumber = epObj.optInt("episodeNumber", j + 1),
                            title = epObj.optString("title", "Episódio ${j + 1}"),
                            videoUrl = epObj.optString("videoUrl"),
                            durationSeconds = epObj.optInt("durationSeconds", 90),
                            isFree = epObj.optBoolean("isFree", true),
                            thumbnail = epObj.optString("thumbnail"),
                            localUri = null
                        )
                    )
                }

                list.add(
                    Drama(
                        id = obj.optString("id"),
                        title = obj.optString("title", "Sem Título"),
                        description = obj.optString("description", ""),
                        coverUrl = obj.optString("coverUrl", ""),
                        bannerUrl = obj.optString("bannerUrl", obj.optString("coverUrl", "")),
                        genre = obj.optString("genre", "Romance"),
                        totalEpisodes = obj.optInt("totalEpisodes", episodes.size.coerceAtLeast(1)),
                        rating = obj.optDouble("rating", 4.9),
                        viewsCount = obj.optLong("viewsCount", 1000L),
                        likesCount = obj.optLong("likesCount", 250L),
                        isTrending = obj.optBoolean("isTrending", false),
                        isFeatured = obj.optBoolean("isFeatured", false),
                        episodes = episodes,
                        isPublishedLocally = false,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Throwable) {
            Log.w(TAG, "Falha ao ler Global Cloud Store: ${e.message}")
            emptyList()
        }
    }
}
