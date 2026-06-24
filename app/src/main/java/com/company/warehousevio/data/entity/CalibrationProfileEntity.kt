package com.company.warehousevio.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calibration_profiles")
data class CalibrationProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scaleFactor: Float,
    val noiseFloorM: Float,
    val createdAtMs: Long,
    val label: String,
)
