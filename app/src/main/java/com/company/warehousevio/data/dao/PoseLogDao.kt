package com.company.warehousevio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.company.warehousevio.data.entity.PoseLogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PoseLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PoseLogEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<PoseLogEntry>)

    @Query("SELECT * FROM pose_log WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
    fun getForSession(sessionId: Long): Flow<List<PoseLogEntry>>

    @Query("SELECT * FROM pose_log WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
    suspend fun getForSessionOnce(sessionId: Long): List<PoseLogEntry>

    @Query("DELETE FROM pose_log WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)
}
