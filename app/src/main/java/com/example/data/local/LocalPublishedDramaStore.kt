package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.Drama
import com.example.data.model.Episode
import org.json.JSONArray
import org.json.JSONObject

class LocalPublishedDramaStore(context: Context) {
    private val TAG = "LocalDramaStore"
    private val prefs: SharedPreferences =
        context.getSharedPreferences("litoral_published_store", Context.MODE_PRIVATE)

    fun saveDrama(drama: Drama) {
        val list = getSavedDramas().toMutableList()
        val index = list.indexOfFirst { it.id == drama.id }
        if (index >= 0) {
            list[index] = drama
        } else {
            list.add(0, drama)
        }
        persistDramas(list)
        Log.d(TAG, "Drama '${drama.title}' salvo no armazenamento local com sucesso! Total: ${list.size}")
    }

    fun getSavedDramas(): List<Drama> {
        val raw = prefs.getString("published_dramas_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<Drama>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val epsArr = obj.optJSONArray("episodes") ?: JSONArray()
                val episodes = mutableListOf<Episode>()
                for (j in 0 until epsArr.length()) {
                    val epObj = epsArr.getJSONObject(j)
                    episodes.add(
                        Episode(
                            id = epObj.optString("id", "ep_$j"),
                            dramaId = epObj.optString("dramaId", obj.optString("id")),
                            episodeNumber = epObj.optInt("episodeNumber", j + 1),
                            title = epObj.optString("title", "Episódio ${j + 1}"),
                            videoUrl = epObj.optString("videoUrl"),
                            durationSeconds = epObj.optInt("durationSeconds", 90),
                            isFree = epObj.optBoolean("isFree", true),
                            thumbnail = epObj.optString("thumbnail"),
                            localUri = epObj.optString("localUri").takeIf { it.isNotBlank() }
                        )
                    )
                }

                list.add(
                    Drama(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        description = obj.optString("description"),
                        coverUrl = obj.optString("coverUrl"),
                        bannerUrl = obj.optString("bannerUrl"),
                        genre = obj.optString("genre", "Romance"),
                        totalEpisodes = obj.optInt("totalEpisodes", episodes.size.coerceAtLeast(1)),
                        rating = obj.optDouble("rating", 4.9),
                        viewsCount = obj.optLong("viewsCount", 1000L),
                        likesCount = obj.optLong("likesCount", 250L),
                        isTrending = obj.optBoolean("isTrending", false),
                        isFeatured = obj.optBoolean("isFeatured", false),
                        episodes = episodes,
                        isPublishedLocally = true,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ler dramas locais: ${e.message}")
            emptyList()
        }
    }

    fun deleteDrama(dramaId: String) {
        val list = getSavedDramas().filterNot { it.id == dramaId }
        persistDramas(list)
    }

    private fun persistDramas(list: List<Drama>) {
        val arr = JSONArray()
        for (d in list) {
            val obj = JSONObject()
            obj.put("id", d.id)
            obj.put("title", d.title)
            obj.put("description", d.description)
            obj.put("coverUrl", d.coverUrl)
            obj.put("bannerUrl", d.bannerUrl)
            obj.put("genre", d.genre)
            obj.put("totalEpisodes", d.totalEpisodes)
            obj.put("rating", d.rating)
            obj.put("viewsCount", d.viewsCount)
            obj.put("likesCount", d.likesCount)
            obj.put("isTrending", d.isTrending)
            obj.put("isFeatured", d.isFeatured)
            obj.put("createdAt", d.createdAt)

            val epsArr = JSONArray()
            for (ep in d.episodes) {
                val epObj = JSONObject()
                epObj.put("id", ep.id)
                epObj.put("dramaId", ep.dramaId)
                epObj.put("episodeNumber", ep.episodeNumber)
                epObj.put("title", ep.title)
                epObj.put("videoUrl", ep.videoUrl)
                epObj.put("durationSeconds", ep.durationSeconds)
                epObj.put("isFree", ep.isFree)
                epObj.put("thumbnail", ep.thumbnail)
                epObj.put("localUri", ep.localUri ?: "")
                epsArr.put(epObj)
            }
            obj.put("episodes", epsArr)
            arr.put(obj)
        }
        prefs.edit().putString("published_dramas_json", arr.toString()).apply()
    }
}
