package com.company.warehousevio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.company.warehousevio.data.entity.EventLogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface EventLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: EventLogEntry): Long

    @Query("SELECT * FROM event_log WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
    fun getForSession(sessionId: Long): Flow<List<EventLogEntry>>

    @Query("SELECT * FROM event_log WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
    suspend fun getForSessionOnce(sessionId: Long): List<EventLogEntry>
}
