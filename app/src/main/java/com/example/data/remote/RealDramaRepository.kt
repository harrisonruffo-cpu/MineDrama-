package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.DramaEntity
import com.example.data.local.EpisodeEntity
import com.example.data.local.LocalPublishedDramaStore
import com.example.data.model.Drama
import com.example.data.model.Episode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RealDramaRepository(private val context: Context) : DramaApiService {
    private val TAG = "RealDramaRepo"
    private val localStore = LocalPublishedDramaStore(context)
    private val db = AppDatabase.getDatabase(context)
    private val cloudSyncManager = CloudSyncManager(context)

    override suspend fun getTrendingDramas(): List<Drama> = withContext(Dispatchers.IO) {
        val all = syncCloudDramas()
        all.filter { it.isTrending || it.rating >= 4.7 }
    }

    override suspend fun getFeaturedDramas(): List<Drama> = withContext(Dispatchers.IO) {
        val all = syncCloudDramas()
        all.filter { it.isFeatured || it.viewsCount > 5000L }
    }

    override suspend fun getDramasByGenre(genre: String): List<Drama> = withContext(Dispatchers.IO) {
        val all = syncCloudDramas()
        if (genre.equals("Todos", ignoreCase = true)) all
        else all.filter { it.genre.equals(genre, ignoreCase = true) }
    }

    override suspend fun getDramaDetails(dramaId: String): Drama? = withContext(Dispatchers.IO) {
        syncCloudDramas().find { it.id == dramaId }
    }

    override suspend fun publishDrama(drama: Drama): Boolean = withContext(Dispatchers.IO) {
        // 1. Salva imediatamente no banco Room local
        try {
            val entity = DramaEntity(
                id = drama.id,
                title = drama.title,
                description = drama.description,
                coverUrl = drama.coverUrl,
                bannerUrl = drama.bannerUrl,
                genre = drama.genre,
                tagsJson = "",
                totalEpisodes = drama.totalEpisodes,
                rating = drama.rating,
                viewsCount = drama.viewsCount,
                likesCount = drama.likesCount,
                isTrending = drama.isTrending,
                isFeatured = drama.isFeatured,
                isPublishedLocally = true,
                createdAt = drama.createdAt
            )
            db.dramaDao().insertDrama(entity)
            val epEntities = drama.episodes.map { ep ->
                EpisodeEntity(
                    id = ep.id,
                    dramaId = drama.id,
                    episodeNumber = ep.episodeNumber,
                    title = ep.title,
                    videoUrl = ep.videoUrl,
                    durationSeconds = ep.durationSeconds,
                    isFree = ep.isFree,
                    thumbnail = ep.thumbnail,
                    localUri = ep.localUri
                )
            }
            db.dramaDao().insertEpisodes(epEntities)
            Log.d(TAG, "Drama inserido no Room: ${drama.id}")
        } catch (e: Throwable) {
            Log.e(TAG, "Erro ao gravar no Room: ${e.message}")
        }

        // 2. Salva no armazenamento secundário local
        localStore.saveDrama(drama)

        // 3. Sincronização perene na Nuvem (Appwrite NYC Cloud + Cloud Store Global)
        try {
            val cloudOk = cloudSyncManager.saveDramaToCloud(drama)
            Log.d(TAG, "Sincronização na nuvem do drama '${drama.title}': $cloudOk")
        } catch (e: Throwable) {
            Log.e(TAG, "Erro ao sincronizar drama na nuvem: ${e.message}")
        }

        true
    }

    override suspend fun syncCloudDramas(): List<Drama> = withContext(Dispatchers.IO) {
        val mergedMap = mutableMapOf<String, Drama>()

        // 1. DADOS PADRÃO (Seeders)
        getSeedDramas().forEach { mergedMap[it.id] = it }

        // 2. DADOS DO BANCO ROOM LOCAL
        try {
            val roomDramas = db.dramaDao().getDramaById("dummy") // Força abrir DB
        } catch (_: Throwable) {}

        localStore.getSavedDramas().forEach { mergedMap[it.id] = it }

        // 3. SINCRONIZAÇÃO COMPLETA DA NUVEM (Appwrite Cloud + Global Cloud Engine)
        // Isso garante que mesmo se o APK for desinstalado e reinstalado, todas as novelas
        // publicadas, capas e episódios sejam baixados e recriados no dispositivo!
        try {
            val cloudDramas = cloudSyncManager.fetchAllCloudDramas()
            Log.d(TAG, "Dramas recebidos da nuvem: ${cloudDramas.size}")

            for (cloudDrama in cloudDramas) {
                mergedMap[cloudDrama.id] = cloudDrama

                // Atualiza/salva no Room local e no localStore para ficar disponível offline
                localStore.saveDrama(cloudDrama)
                try {
                    val entity = DramaEntity(
                        id = cloudDrama.id,
                        title = cloudDrama.title,
                        description = cloudDrama.description,
                        coverUrl = cloudDrama.coverUrl,
                        bannerUrl = cloudDrama.bannerUrl,
                        genre = cloudDrama.genre,
                        tagsJson = "",
                        totalEpisodes = cloudDrama.totalEpisodes,
                        rating = cloudDrama.rating,
                        viewsCount = cloudDrama.viewsCount,
                        likesCount = cloudDrama.likesCount,
                        isTrending = cloudDrama.isTrending,
                        isFeatured = cloudDrama.isFeatured,
                        isPublishedLocally = false,
                        createdAt = cloudDrama.createdAt
                    )
                    db.dramaDao().insertDrama(entity)
                    val epEntities = cloudDrama.episodes.map { ep ->
                        EpisodeEntity(
                            id = ep.id,
                            dramaId = cloudDrama.id,
                            episodeNumber = ep.episodeNumber,
                            title = ep.title,
                            videoUrl = ep.videoUrl,
                            durationSeconds = ep.durationSeconds,
                            isFree = ep.isFree,
                            thumbnail = ep.thumbnail,
                            localUri = null
                        )
                    }
                    db.dramaDao().insertEpisodes(epEntities)
                } catch (e: Throwable) {
                    Log.w(TAG, "Erro ao popular Room com drama da nuvem: ${e.message}")
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Erro na sincronização de nuvem: ${e.message}")
        }

        mergedMap.values.sortedByDescending { it.createdAt }
    }

    private fun getSeedDramas(): List<Drama> {
        return listOf(
            Drama(
                id = "drama_litoral_1",
                title = "O Segredo da Baía",
                description = "Um mistério envolvente que acontece nas noites de verão do litoral sul.",
                coverUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=600&q=80",
                bannerUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80",
                genre = "Suspense",
                totalEpisodes = 3,
                rating = 4.9,
                viewsCount = 18500L,
                likesCount = 4200L,
                isTrending = true,
                isFeatured = true,
                episodes = listOf(
                    Episode(
                        id = "ep_litoral_1_1",
                        dramaId = "drama_litoral_1",
                        episodeNumber = 1,
                        title = "O Encontro no Cais",
                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                        durationSeconds = 60
                    ),
                    Episode(
                        id = "ep_litoral_1_2",
                        dramaId = "drama_litoral_1",
                        episodeNumber = 2,
                        title = "Sombras na Praia",
                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                        durationSeconds = 60
                    )
                ),
                createdAt = 1700000000000L
            ),
            Drama(
                id = "drama_litoral_2",
                title = "Amor em Mar Aberto",
                description = "Entre ventos e marés, duas almas encontram o amor verdadeiro.",
                coverUrl = "https://images.unsplash.com/photo-1519046904884-53103b34b206?auto=format&fit=crop&w=600&q=80",
                bannerUrl = "https://images.unsplash.com/photo-1519046904884-53103b34b206?auto=format&fit=crop&w=1200&q=80",
                genre = "Romance",
                totalEpisodes = 2,
                rating = 4.8,
                viewsCount = 14200L,
                likesCount = 3100L,
                isTrending = true,
                isFeatured = false,
                episodes = listOf(
                    Episode(
                        id = "ep_litoral_2_1",
                        dramaId = "drama_litoral_2",
                        episodeNumber = 1,
                        title = "Ventos de Paixão",
                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                        durationSeconds = 60
                    )
                ),
                createdAt = 1700000001000L
            )
        )
    }
}
