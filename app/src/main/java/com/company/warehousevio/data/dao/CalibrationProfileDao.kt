package com.company.warehousevio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.company.warehousevio.data.entity.CalibrationProfileEntity

@Dao
interface CalibrationProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: CalibrationProfileEntity): Long

    @Query("SELECT * FROM calibration_profiles ORDER BY createdAtMs DESC LIMIT 1")
    suspend fun getLatest(): CalibrationProfileEntity?

    @Query("SELECT * FROM calibration_profiles ORDER BY createdAtMs DESC")
    suspend fun getAll(): List<CalibrationProfileEntity>
}
