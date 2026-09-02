package com.example.data.model

data class Episode(
    val id: String = "",
    val dramaId: String = "",
    val episodeNumber: Int = 1,
    val title: String = "",
    val videoUrl: String = "",
    val durationSeconds: Int = 90,
    val isFree: Boolean = true,
    val thumbnail: String = "",
    val localUri: String? = null
)

data class Drama(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val coverUrl: String = "",
    val bannerUrl: String = "",
    val genre: String = "Romance",
    val tags: List<String> = emptyList(),
    val totalEpisodes: Int = 1,
    val rating: Double = 4.8,
    val viewsCount: Long = 1200L,
    val likesCount: Long = 340L,
    val isTrending: Boolean = false,
    val isFeatured: Boolean = false,
    val episodes: List<Episode> = emptyList(),
    val isPublishedLocally: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class UserProfile(
    val id: String = "",
    val name: String = "Usuário",
    val email: String = "",
    val avatarUrl: String = "",
    val coinsBalance: Int = 100,
    val isVip: Boolean = false
)
