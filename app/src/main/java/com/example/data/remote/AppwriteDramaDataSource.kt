package com.example.data.remote

import android.util.Log
import com.example.Appwrite
import com.example.data.model.Drama
import com.example.data.model.Episode
import io.appwrite.ID
import io.appwrite.Query
import io.appwrite.models.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AppwriteDramaDataSource {
    private val TAG = "AppwriteDramaDS"

    suspend fun listDramas(): List<Drama> = withContext(Dispatchers.IO) {
        try {
            val db = Appwrite.databases ?: return@withContext emptyList()
            Appwrite.ensureSession()
            val response = try {
                db.listDocuments(
                    databaseId = Appwrite.DATABASE_ID,
                    collectionId = Appwrite.COLLECTION_DRAMAS,
                    queries = listOf(Query.limit(100))
                )
            } catch (queryErr: Throwable) {
                Log.w(TAG, "Tentando listar sem queries: ${queryErr.message}")
                db.listDocuments(
                    databaseId = Appwrite.DATABASE_ID,
                    collectionId = Appwrite.COLLECTION_DRAMAS
                )
            }
            val dramas = response.documents.mapNotNull { doc ->
                mapDocumentToDrama(doc)
            }
            Log.d(TAG, "Total de dramas carregados do Appwrite NYC Cloud: ${dramas.size}")
            dramas
        } catch (e: Throwable) {
            Log.e(TAG, "Erro ao listar dramas do Appwrite: ${e.message}")
            emptyList()
        }
    }

    suspend fun saveDrama(drama: Drama): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = Appwrite.databases ?: return@withContext false
            Appwrite.ensureSession()

            val episodesJson = JSONArray().apply {
                drama.episodes.forEach { ep ->
                    put(
                        JSONObject().apply {
                            put("id", ep.id)
                            put("dramaId", drama.id)
                            put("episodeNumber", ep.episodeNumber)
                            put("title", ep.title)
                            put("videoUrl", ep.videoUrl)
                            put("durationSeconds", ep.durationSeconds)
                            put("isFree", ep.isFree)
                            put("thumbnail", ep.thumbnail)
                            put("localUri", "")
                        }
                    )
                }
            }.toString()

            val docId = if (drama.id.isNotBlank() && drama.id.length <= 36 && !drama.id.contains(" ") && !drama.id.contains("-")) {
                drama.id.replace(Regex("[^a-zA-Z0-9_]"), "_").take(36)
            } else {
                "drama_" + (System.currentTimeMillis() % 1000000000)
            }

            // Múltiplos formatos para garantir compatibilidade com qualquer schema no Appwrite
            val payloadFull = mutableMapOf<String, Any>(
                "title" to drama.title,
                "description" to (drama.description.ifBlank { "Novela exclusiva Litoral Novelas." }),
                "synopsis" to (drama.description.ifBlank { "Novela exclusiva Litoral Novelas." }),
                "coverUrl" to drama.coverUrl,
                "genre" to drama.genre,
                "tags" to drama.genre,
                "episodes" to episodesJson,
                "episodesJson" to episodesJson,
                "authorName" to "Litoral Novelas",
                "createdAt" to drama.createdAt.toString(),
                "views" to drama.viewsCount.toString(),
                "rating" to drama.rating.toString()
            )

            val payloadClassic = mutableMapOf<String, Any>(
                "title" to drama.title,
                "description" to drama.description,
                "coverUrl" to drama.coverUrl,
                "genre" to drama.genre,
                "episodesJson" to episodesJson
            )

            val payloadArrayEpisodes = mutableMapOf<String, Any>(
                "title" to drama.title,
                "synopsis" to drama.description,
                "coverUrl" to drama.coverUrl,
                "tags" to listOf(drama.genre),
                "episodes" to listOf(episodesJson)
            )

            val payloadMinimal = mutableMapOf<String, Any>(
                "title" to drama.title,
                "coverUrl" to drama.coverUrl,
                "episodes" to episodesJson
            )

            val payloadsToTry = listOf(payloadFull, payloadClassic, payloadArrayEpisodes, payloadMinimal)
            var saved = false

            for ((index, currentPayload) in payloadsToTry.withIndex()) {
                if (saved) break
                try {
                    try {
                        db.updateDocument(
                            databaseId = Appwrite.DATABASE_ID,
                            collectionId = Appwrite.COLLECTION_DRAMAS,
                            documentId = docId,
                            data = currentPayload
                        )
                        Log.d(TAG, "Drama atualizado com sucesso no Appwrite (payload $index): $docId")
                        saved = true
                    } catch (_: Throwable) {
                        db.createDocument(
                            databaseId = Appwrite.DATABASE_ID,
                            collectionId = Appwrite.COLLECTION_DRAMAS,
                            documentId = docId,
                            data = currentPayload
                        )
                        Log.d(TAG, "Drama criado com sucesso no Appwrite (payload $index): $docId")
                        saved = true
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Tentativa de payload $index falhou no Appwrite: ${e.message}")
                }
            }
            saved
        } catch (e: Throwable) {
            Log.e(TAG, "Falha ao salvar drama no Appwrite: ${e.message}", e)
            false
        }
    }

    private fun mapDocumentToDrama(doc: Document<Map<String, Any>>): Drama? {
        return try {
            val data = doc.data
            val episodes = mutableListOf<Episode>()
            val epRaw = when (val raw = data["episodes"] ?: data["episodesJson"]) {
                is List<*> -> raw.firstOrNull()?.toString()
                is String -> raw
                else -> null
            }

            if (!epRaw.isNullOrBlank()) {
                try {
                    val arr = JSONArray(epRaw)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        episodes.add(
                            Episode(
                                id = obj.optString("id", "${doc.id}_$i"),
                                dramaId = doc.id,
                                episodeNumber = obj.optInt("episodeNumber", i + 1),
                                title = obj.optString("title", "Episódio ${i + 1}"),
                                videoUrl = obj.optString("videoUrl"),
                                durationSeconds = obj.optInt("durationSeconds", 90),
                                isFree = obj.optBoolean("isFree", true),
                                thumbnail = obj.optString("thumbnail"),
                                localUri = null
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Erro ao parsear episodesJson: ${e.message}")
                }
            }

            val desc = (data["description"] ?: data["synopsis"])?.let {
                if (it is List<*>) it.firstOrNull()?.toString() else it.toString()
            } ?: ""

            val genreTag = (data["genre"] ?: data["tags"])?.let {
                if (it is List<*>) it.firstOrNull()?.toString() else it.toString()
            } ?: "Romance"

            val ratingVal = (data["rating"])?.let {
                if (it is List<*>) it.firstOrNull()?.toString()?.toDoubleOrNull()
                else (it as? Number)?.toDouble() ?: it.toString().toDoubleOrNull()
            } ?: 4.9

            val cover = (data["coverUrl"] as? String)?.ifBlank {
                "https://images.unsplash.com/photo-1518173946687-a4c8a383392e?auto=format&fit=crop&w=600&q=80"
            } ?: "https://images.unsplash.com/photo-1518173946687-a4c8a383392e?auto=format&fit=crop&w=600&q=80"

            Drama(
                id = doc.id,
                title = (data["title"] as? String) ?: "Sem Título",
                description = desc,
                coverUrl = cover,
                bannerUrl = cover,
                genre = genreTag,
                totalEpisodes = episodes.size.coerceAtLeast(1),
                rating = ratingVal,
                viewsCount = 1200L,
                likesCount = 350L,
                isTrending = true,
                isFeatured = true,
                episodes = episodes,
                isPublishedLocally = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro mapeando documento do Appwrite: ${e.message}")
            null
        }
    }
}
