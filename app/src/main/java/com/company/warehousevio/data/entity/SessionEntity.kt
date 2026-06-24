package com.company.warehousevio.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimestampMs: Long,
    val endTimestampMs: Long? = null,
    val operatorName: String = "",
    val notes: String = "",
    val calibrationProfileId: Long? = null,
)
