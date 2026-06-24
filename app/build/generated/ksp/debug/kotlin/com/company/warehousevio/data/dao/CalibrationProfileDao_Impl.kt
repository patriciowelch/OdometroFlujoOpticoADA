package com.company.warehousevio.`data`.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.company.warehousevio.`data`.entity.CalibrationProfileEntity
import javax.`annotation`.processing.Generated
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class CalibrationProfileDao_Impl(
  __db: RoomDatabase,
) : CalibrationProfileDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCalibrationProfileEntity:
      EntityInsertAdapter<CalibrationProfileEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfCalibrationProfileEntity = object :
        EntityInsertAdapter<CalibrationProfileEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `calibration_profiles` (`id`,`scaleFactor`,`noiseFloorM`,`createdAtMs`,`label`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CalibrationProfileEntity) {
        statement.bindLong(1, entity.id)
        statement.bindDouble(2, entity.scaleFactor.toDouble())
        statement.bindDouble(3, entity.noiseFloorM.toDouble())
        statement.bindLong(4, entity.createdAtMs)
        statement.bindText(5, entity.label)
      }
    }
  }

  public override suspend fun insert(profile: CalibrationProfileEntity): Long =
      performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfCalibrationProfileEntity.insertAndReturnId(_connection,
        profile)
    _result
  }

  public override suspend fun getLatest(): CalibrationProfileEntity? {
    val _sql: String = "SELECT * FROM calibration_profiles ORDER BY createdAtMs DESC LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfScaleFactor: Int = getColumnIndexOrThrow(_stmt, "scaleFactor")
        val _columnIndexOfNoiseFloorM: Int = getColumnIndexOrThrow(_stmt, "noiseFloorM")
        val _columnIndexOfCreatedAtMs: Int = getColumnIndexOrThrow(_stmt, "createdAtMs")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _result: CalibrationProfileEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpScaleFactor: Float
          _tmpScaleFactor = _stmt.getDouble(_columnIndexOfScaleFactor).toFloat()
          val _tmpNoiseFloorM: Float
          _tmpNoiseFloorM = _stmt.getDouble(_columnIndexOfNoiseFloorM).toFloat()
          val _tmpCreatedAtMs: Long
          _tmpCreatedAtMs = _stmt.getLong(_columnIndexOfCreatedAtMs)
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          _result =
              CalibrationProfileEntity(_tmpId,_tmpScaleFactor,_tmpNoiseFloorM,_tmpCreatedAtMs,_tmpLabel)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAll(): List<CalibrationProfileEntity> {
    val _sql: String = "SELECT * FROM calibration_profiles ORDER BY createdAtMs DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfScaleFactor: Int = getColumnIndexOrThrow(_stmt, "scaleFactor")
        val _columnIndexOfNoiseFloorM: Int = getColumnIndexOrThrow(_stmt, "noiseFloorM")
        val _columnIndexOfCreatedAtMs: Int = getColumnIndexOrThrow(_stmt, "createdAtMs")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _result: MutableList<CalibrationProfileEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CalibrationProfileEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpScaleFactor: Float
          _tmpScaleFactor = _stmt.getDouble(_columnIndexOfScaleFactor).toFloat()
          val _tmpNoiseFloorM: Float
          _tmpNoiseFloorM = _stmt.getDouble(_columnIndexOfNoiseFloorM).toFloat()
          val _tmpCreatedAtMs: Long
          _tmpCreatedAtMs = _stmt.getLong(_columnIndexOfCreatedAtMs)
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          _item =
              CalibrationProfileEntity(_tmpId,_tmpScaleFactor,_tmpNoiseFloorM,_tmpCreatedAtMs,_tmpLabel)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
