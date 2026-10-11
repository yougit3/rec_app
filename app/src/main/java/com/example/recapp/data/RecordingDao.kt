package com.example.recapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<Recording>>

    @Insert
    suspend fun insert(recording: Recording): Long

    @Delete
    suspend fun delete(recording: Recording)
}
