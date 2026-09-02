package com.example.data.remote

import com.example.data.model.Drama

interface DramaApiService {
    suspend fun getTrendingDramas(): List<Drama>
    suspend fun getFeaturedDramas(): List<Drama>
    suspend fun getDramasByGenre(genre: String): List<Drama>
    suspend fun getDramaDetails(dramaId: String): Drama?
    suspend fun publishDrama(drama: Drama): Boolean
    suspend fun syncCloudDramas(): List<Drama>
}
