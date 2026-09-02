package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DramaDao {
    @Query("SELECT * FROM dramas ORDER BY createdAt DESC")
    fun getAllDramasFlow(): Flow<List<DramaEntity>>

    @Query("SELECT * FROM dramas WHERE id = :dramaId LIMIT 1")
    suspend fun getDramaById(dramaId: String): DramaEntity?

    @Query("SELECT * FROM episodes WHERE dramaId = :dramaId ORDER BY episodeNumber ASC")
    fun getEpisodesForDramaFlow(dramaId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE dramaId = :dramaId ORDER BY episodeNumber ASC")
    suspend fun getEpisodesForDrama(dramaId: String): List<EpisodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrama(drama: DramaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDramas(dramas: List<DramaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisode(episode: EpisodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<EpisodeEntity>)

    @Query("DELETE FROM dramas WHERE id = :dramaId")
    suspend fun deleteDrama(dramaId: String)

    @Query("DELETE FROM episodes WHERE dramaId = :dramaId")
    suspend fun deleteEpisodesForDrama(dramaId: String)

    // Favorites
    @Query("SELECT dramaId FROM user_favorites ORDER BY addedAt DESC")
    fun getFavoriteIdsFlow(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM user_favorites WHERE dramaId = :dramaId)")
    suspend fun isFavorite(dramaId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM user_favorites WHERE dramaId = :dramaId")
    suspend fun removeFavorite(dramaId: String)

    // History
    @Query("SELECT * FROM watch_history ORDER BY updatedAt DESC")
    fun getWatchHistoryFlow(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateHistory(history: HistoryEntity)
}
