package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DramaEntity::class,
        EpisodeEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dramaDao(): DramaDao

    companion object {
        private const val TAG = "AppDatabase"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "litoral_novelas.db"
                    )
                        .fallbackToDestructiveMigration()
                        .fallbackToDestructiveMigrationOnDowngrade()
                        .build()
                    INSTANCE = instance
                    instance
                } catch (e: Throwable) {
                    Log.e(TAG, "Falha ao abrir banco Room, recriando em memória: ${e.message}")
                    val memoryDb = Room.inMemoryDatabaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java
                    ).build()
                    INSTANCE = memoryDb
                    memoryDb
                }
            }
        }
    }
}
