package com.example.data.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.Drama
import com.example.data.model.Episode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DonoDoMorroManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("dono_do_morro_prefs", Context.MODE_PRIVATE)

    companion object {
        const val DRAMA_ID = "drama_dono_do_morro"
        const val DEFAULT_YOUTUBE_LINK = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        private const val KEY_EP1_LINK = "ep1_youtube_link"
    }

    private val _ep1Link = MutableStateFlow(
        prefs.getString(KEY_EP1_LINK, DEFAULT_YOUTUBE_LINK) ?: DEFAULT_YOUTUBE_LINK
    )
    val ep1Link: StateFlow<String> = _ep1Link.asStateFlow()

    fun updateEpisode1Link(newLink: String) {
        val clean = newLink.trim()
        if (clean.isNotBlank()) {
            prefs.edit().putString(KEY_EP1_LINK, clean).apply()
            _ep1Link.value = clean
        }
    }

    fun getDrama(): Drama {
        val currentLink = _ep1Link.value
        return Drama(
            id = DRAMA_ID,
            title = "Dono Do Morro",
            description = "Série Brasileira Ação Favela",
            coverUrl = "dono_do_morro_cover",
            bannerUrl = "dono_do_morro_banner",
            genre = "Ação Favela",
            totalEpisodes = 4,
            rating = 5.0,
            viewsCount = 980000L,
            likesCount = 145000L,
            isTrending = true,
            isFeatured = true,
            episodes = listOf(
                Episode(
                    id = "ep_morro_1",
                    dramaId = DRAMA_ID,
                    episodeNumber = 1,
                    title = "Episódio 1: A Chegada do Malvadão",
                    videoUrl = currentLink,
                    durationSeconds = 120,
                    isFree = true,
                    thumbnail = "dono_do_morro_cover"
                ),
                Episode(
                    id = "ep_morro_2",
                    dramaId = DRAMA_ID,
                    episodeNumber = 2,
                    title = "Episódio 2: A Lei da Favela",
                    videoUrl = currentLink,
                    durationSeconds = 115,
                    isFree = true,
                    thumbnail = "dono_do_morro_cover"
                ),
                Episode(
                    id = "ep_morro_3",
                    dramaId = DRAMA_ID,
                    episodeNumber = 3,
                    title = "Episódio 3: O Cerco no Beco",
                    videoUrl = currentLink,
                    durationSeconds = 130,
                    isFree = false,
                    thumbnail = "dono_do_morro_cover"
                ),
                Episode(
                    id = "ep_morro_4",
                    dramaId = DRAMA_ID,
                    episodeNumber = 4,
                    title = "Episódio 4: O Trono do Morro",
                    videoUrl = currentLink,
                    durationSeconds = 145,
                    isFree = false,
                    thumbnail = "dono_do_morro_cover"
                )
            ),
            isPublishedLocally = false,
            createdAt = System.currentTimeMillis()
        )
    }
}
