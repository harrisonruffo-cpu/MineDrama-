package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dramas")
data class DramaEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val coverUrl: String,
    val bannerUrl: String,
    val genre: String,
    val tagsJson: String,
    val totalEpisodes: Int,
    val rating: Double,
    val viewsCount: Long,
    val likesCount: Long,
    val isTrending: Boolean,
    val isFeatured: Boolean,
    val isPublishedLocally: Boolean,
    val createdAt: Long
)

@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val dramaId: String,
    val episodeNumber: Int,
    val title: String,
    val videoUrl: String,
    val durationSeconds: Int,
    val isFree: Boolean,
    val thumbnail: String,
    val localUri: String?
)

@Entity(tableName = "user_favorites")
data class FavoriteEntity(
    @PrimaryKey val dramaId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "watch_history")
data class HistoryEntity(
    @PrimaryKey val dramaId: String,
    val lastEpisodeNumber: Int,
    val lastPositionMs: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
