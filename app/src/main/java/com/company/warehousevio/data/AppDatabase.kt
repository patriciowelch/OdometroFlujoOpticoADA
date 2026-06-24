package com.company.warehousevio.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.company.warehousevio.data.dao.CalibrationProfileDao
import com.company.warehousevio.data.dao.EventLogDao
import com.company.warehousevio.data.dao.PoseLogDao
import com.company.warehousevio.data.dao.SessionDao
import com.company.warehousevio.data.entity.CalibrationProfileEntity
import com.company.warehousevio.data.entity.EventLogEntry
import com.company.warehousevio.data.entity.PoseLogEntry
import com.company.warehousevio.data.entity.SessionEntity

@Database(
    entities = [
        SessionEntity::class,
        PoseLogEntry::class,
        EventLogEntry::class,
        CalibrationProfileEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun poseLogDao(): PoseLogDao
    abstract fun eventLogDao(): EventLogDao
    abstract fun calibrationProfileDao(): CalibrationProfileDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "warehouse_vio.db",
                ).build().also { INSTANCE = it }
            }
    }
}
