package com.company.warehousevio.`data`.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.company.warehousevio.`data`.entity.SessionEntity
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class SessionDao_Impl(
  __db: RoomDatabase,
) : SessionDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfSessionEntity: EntityInsertAdapter<SessionEntity>

  private val __updateAdapterOfSessionEntity: EntityDeleteOrUpdateAdapter<SessionEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfSessionEntity = object : EntityInsertAdapter<SessionEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `sessions` (`id`,`startTimestampMs`,`endTimestampMs`,`operatorName`,`notes`,`calibrationProfileId`) VALUES (nullif(?, 0),?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SessionEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.startTimestampMs)
        val _tmpEndTimestampMs: Long? = entity.endTimestampMs
        if (_tmpEndTimestampMs == null) {
          statement.bindNull(3)
        } else {
          statement.bindLong(3, _tmpEndTimestampMs)
        }
        statement.bindText(4, entity.operatorName)
        statement.bindText(5, entity.notes)
        val _tmpCalibrationProfileId: Long? = entity.calibrationProfileId
        if (_tmpCalibrationProfileId == null) {
          statement.bindNull(6)
        } else {
          statement.bindLong(6, _tmpCalibrationProfileId)
        }
      }
    }
    this.__updateAdapterOfSessionEntity = object : EntityDeleteOrUpdateAdapter<SessionEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `sessions` SET `id` = ?,`startTimestampMs` = ?,`endTimestampMs` = ?,`operatorName` = ?,`notes` = ?,`calibrationProfileId` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SessionEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.startTimestampMs)
        val _tmpEndTimestampMs: Long? = entity.endTimestampMs
        if (_tmpEndTimestampMs == null) {
          statement.bindNull(3)
        } else {
          statement.bindLong(3, _tmpEndTimestampMs)
        }
        statement.bindText(4, entity.operatorName)
        statement.bindText(5, entity.notes)
        val _tmpCalibrationProfileId: Long? = entity.calibrationProfileId
        if (_tmpCalibrationProfileId == null) {
          statement.bindNull(6)
        } else {
          statement.bindLong(6, _tmpCalibrationProfileId)
        }
        statement.bindLong(7, entity.id)
      }
    }
  }

  public override suspend fun insert(session: SessionEntity): Long = performSuspending(__db, false,
      true) { _connection ->
    val _result: Long = __insertAdapterOfSessionEntity.insertAndReturnId(_connection, session)
    _result
  }

  public override suspend fun update(session: SessionEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __updateAdapterOfSessionEntity.handle(_connection, session)
  }

  public override fun getAllSessions(): Flow<List<SessionEntity>> {
    val _sql: String = "SELECT * FROM sessions ORDER BY startTimestampMs DESC"
    return createFlow(__db, false, arrayOf("sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfStartTimestampMs: Int = getColumnIndexOrThrow(_stmt, "startTimestampMs")
        val _columnIndexOfEndTimestampMs: Int = getColumnIndexOrThrow(_stmt, "endTimestampMs")
        val _columnIndexOfOperatorName: Int = getColumnIndexOrThrow(_stmt, "operatorName")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfCalibrationProfileId: Int = getColumnIndexOrThrow(_stmt,
            "calibrationProfileId")
        val _result: MutableList<SessionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SessionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpStartTimestampMs: Long
          _tmpStartTimestampMs = _stmt.getLong(_columnIndexOfStartTimestampMs)
          val _tmpEndTimestampMs: Long?
          if (_stmt.isNull(_columnIndexOfEndTimestampMs)) {
            _tmpEndTimestampMs = null
          } else {
            _tmpEndTimestampMs = _stmt.getLong(_columnIndexOfEndTimestampMs)
          }
          val _tmpOperatorName: String
          _tmpOperatorName = _stmt.getText(_columnIndexOfOperatorName)
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          val _tmpCalibrationProfileId: Long?
          if (_stmt.isNull(_columnIndexOfCalibrationProfileId)) {
            _tmpCalibrationProfileId = null
          } else {
            _tmpCalibrationProfileId = _stmt.getLong(_columnIndexOfCalibrationProfileId)
          }
          _item =
              SessionEntity(_tmpId,_tmpStartTimestampMs,_tmpEndTimestampMs,_tmpOperatorName,_tmpNotes,_tmpCalibrationProfileId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getById(id: Long): SessionEntity? {
    val _sql: String = "SELECT * FROM sessions WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfStartTimestampMs: Int = getColumnIndexOrThrow(_stmt, "startTimestampMs")
        val _columnIndexOfEndTimestampMs: Int = getColumnIndexOrThrow(_stmt, "endTimestampMs")
        val _columnIndexOfOperatorName: Int = getColumnIndexOrThrow(_stmt, "operatorName")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfCalibrationProfileId: Int = getColumnIndexOrThrow(_stmt,
            "calibrationProfileId")
        val _result: SessionEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpStartTimestampMs: Long
          _tmpStartTimestampMs = _stmt.getLong(_columnIndexOfStartTimestampMs)
          val _tmpEndTimestampMs: Long?
          if (_stmt.isNull(_columnIndexOfEndTimestampMs)) {
            _tmpEndTimestampMs = null
          } else {
            _tmpEndTimestampMs = _stmt.getLong(_columnIndexOfEndTimestampMs)
          }
          val _tmpOperatorName: String
          _tmpOperatorName = _stmt.getText(_columnIndexOfOperatorName)
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          val _tmpCalibrationProfileId: Long?
          if (_stmt.isNull(_columnIndexOfCalibrationProfileId)) {
            _tmpCalibrationProfileId = null
          } else {
            _tmpCalibrationProfileId = _stmt.getLong(_columnIndexOfCalibrationProfileId)
          }
          _result =
              SessionEntity(_tmpId,_tmpStartTimestampMs,_tmpEndTimestampMs,_tmpOperatorName,_tmpNotes,_tmpCalibrationProfileId)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Long) {
    val _sql: String = "DELETE FROM sessions WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
