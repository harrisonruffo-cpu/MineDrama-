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
        const val DEFAULT_EP1_LINK = "https://drive.google.com/file/d/1qhB6ie6zskXrROdm7oqN6T5aOvZHjlMT/view?usp=drivesdk"
        const val DEFAULT_EP1_ALT_LINK = "https://vimeo.com/1223423999?share=copy&fl=sv&fe=ci"
        const val DEFAULT_YOUTUBE_LINK = DEFAULT_EP1_LINK
        private const val KEY_EP1_LINK = "ep1_link_drive_v1"
        private const val KEY_EP1_ALT_LINK = "ep1_link_alt_vimeo_v1"
        private const val KEY_ACTIVE_PLAYER = "active_player_index_v1"
    }

    private val _ep1Link = MutableStateFlow(
        prefs.getString(KEY_EP1_LINK, DEFAULT_EP1_LINK) ?: DEFAULT_EP1_LINK
    )
    val ep1Link: StateFlow<String> = _ep1Link.asStateFlow()

    private val _ep1AltLink = MutableStateFlow(
        prefs.getString(KEY_EP1_ALT_LINK, DEFAULT_EP1_ALT_LINK) ?: DEFAULT_EP1_ALT_LINK
    )
    val ep1AltLink: StateFlow<String> = _ep1AltLink.asStateFlow()

    private val _activePlayer = MutableStateFlow(
        prefs.getInt(KEY_ACTIVE_PLAYER, 1)
    )
    val activePlayer: StateFlow<Int> = _activePlayer.asStateFlow()

    fun setActivePlayer(playerNum: Int) {
        val valid = if (playerNum in 1..2) playerNum else 1
        prefs.edit().putInt(KEY_ACTIVE_PLAYER, valid).apply()
        _activePlayer.value = valid
    }

    fun getActiveEp1Link(): String {
        return if (_activePlayer.value == 2) _ep1AltLink.value else _ep1Link.value
    }

    fun updateEpisode1Link(newLink: String) {
        val clean = newLink.trim()
        if (clean.isNotBlank()) {
            prefs.edit().putString(KEY_EP1_LINK, clean).apply()
            _ep1Link.value = clean
        }
    }

    fun updateEpisode1AltLink(newLink: String) {
        val clean = newLink.trim()
        if (clean.isNotBlank()) {
            prefs.edit().putString(KEY_EP1_ALT_LINK, clean).apply()
            _ep1AltLink.value = clean
        }
    }

    fun getDrama(playerOverride: Int? = null): Drama {
        val player = playerOverride ?: _activePlayer.value
        val currentLink = if (player == 2) _ep1AltLink.value else _ep1Link.value
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
