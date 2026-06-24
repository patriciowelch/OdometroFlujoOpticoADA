package com.company.warehousevio.`data`.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.company.warehousevio.`data`.entity.PoseLogEntry
import javax.`annotation`.processing.Generated
import kotlin.Float
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
public class PoseLogDao_Impl(
  __db: RoomDatabase,
) : PoseLogDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfPoseLogEntry: EntityInsertAdapter<PoseLogEntry>
  init {
    this.__db = __db
    this.__insertAdapterOfPoseLogEntry = object : EntityInsertAdapter<PoseLogEntry>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `pose_log` (`id`,`sessionId`,`timestampMs`,`x`,`z`,`accumulatedDistanceM`,`instantaneousVelocityMs`,`trackingState`,`motionState`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: PoseLogEntry) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.sessionId)
        statement.bindLong(3, entity.timestampMs)
        statement.bindDouble(4, entity.x.toDouble())
        statement.bindDouble(5, entity.z.toDouble())
        statement.bindDouble(6, entity.accumulatedDistanceM.toDouble())
        statement.bindDouble(7, entity.instantaneousVelocityMs.toDouble())
        statement.bindText(8, entity.trackingState)
        statement.bindText(9, entity.motionState)
      }
    }
  }

  public override suspend fun insert(entry: PoseLogEntry): Long = performSuspending(__db, false,
      true) { _connection ->
    val _result: Long = __insertAdapterOfPoseLogEntry.insertAndReturnId(_connection, entry)
    _result
  }

  public override suspend fun insertAll(entries: List<PoseLogEntry>): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfPoseLogEntry.insert(_connection, entries)
  }

  public override fun getForSession(sessionId: Long): Flow<List<PoseLogEntry>> {
    val _sql: String = "SELECT * FROM pose_log WHERE sessionId = ? ORDER BY timestampMs ASC"
    return createFlow(__db, false, arrayOf("pose_log")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, sessionId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSessionId: Int = getColumnIndexOrThrow(_stmt, "sessionId")
        val _columnIndexOfTimestampMs: Int = getColumnIndexOrThrow(_stmt, "timestampMs")
        val _columnIndexOfX: Int = getColumnIndexOrThrow(_stmt, "x")
        val _columnIndexOfZ: Int = getColumnIndexOrThrow(_stmt, "z")
        val _columnIndexOfAccumulatedDistanceM: Int = getColumnIndexOrThrow(_stmt,
            "accumulatedDistanceM")
        val _columnIndexOfInstantaneousVelocityMs: Int = getColumnIndexOrThrow(_stmt,
            "instantaneousVelocityMs")
        val _columnIndexOfTrackingState: Int = getColumnIndexOrThrow(_stmt, "trackingState")
        val _columnIndexOfMotionState: Int = getColumnIndexOrThrow(_stmt, "motionState")
        val _result: MutableList<PoseLogEntry> = mutableListOf()
        while (_stmt.step()) {
          val _item: PoseLogEntry
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSessionId: Long
          _tmpSessionId = _stmt.getLong(_columnIndexOfSessionId)
          val _tmpTimestampMs: Long
          _tmpTimestampMs = _stmt.getLong(_columnIndexOfTimestampMs)
          val _tmpX: Float
          _tmpX = _stmt.getDouble(_columnIndexOfX).toFloat()
          val _tmpZ: Float
          _tmpZ = _stmt.getDouble(_columnIndexOfZ).toFloat()
          val _tmpAccumulatedDistanceM: Float
          _tmpAccumulatedDistanceM = _stmt.getDouble(_columnIndexOfAccumulatedDistanceM).toFloat()
          val _tmpInstantaneousVelocityMs: Float
          _tmpInstantaneousVelocityMs =
              _stmt.getDouble(_columnIndexOfInstantaneousVelocityMs).toFloat()
          val _tmpTrackingState: String
          _tmpTrackingState = _stmt.getText(_columnIndexOfTrackingState)
          val _tmpMotionState: String
          _tmpMotionState = _stmt.getText(_columnIndexOfMotionState)
          _item =
              PoseLogEntry(_tmpId,_tmpSessionId,_tmpTimestampMs,_tmpX,_tmpZ,_tmpAccumulatedDistanceM,_tmpInstantaneousVelocityMs,_tmpTrackingState,_tmpMotionState)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getForSessionOnce(sessionId: Long): List<PoseLogEntry> {
    val _sql: String = "SELECT * FROM pose_log WHERE sessionId = ? ORDER BY timestampMs ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, sessionId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSessionId: Int = getColumnIndexOrThrow(_stmt, "sessionId")
        val _columnIndexOfTimestampMs: Int = getColumnIndexOrThrow(_stmt, "timestampMs")
        val _columnIndexOfX: Int = getColumnIndexOrThrow(_stmt, "x")
        val _columnIndexOfZ: Int = getColumnIndexOrThrow(_stmt, "z")
        val _columnIndexOfAccumulatedDistanceM: Int = getColumnIndexOrThrow(_stmt,
            "accumulatedDistanceM")
        val _columnIndexOfInstantaneousVelocityMs: Int = getColumnIndexOrThrow(_stmt,
            "instantaneousVelocityMs")
        val _columnIndexOfTrackingState: Int = getColumnIndexOrThrow(_stmt, "trackingState")
        val _columnIndexOfMotionState: Int = getColumnIndexOrThrow(_stmt, "motionState")
        val _result: MutableList<PoseLogEntry> = mutableListOf()
        while (_stmt.step()) {
          val _item: PoseLogEntry
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSessionId: Long
          _tmpSessionId = _stmt.getLong(_columnIndexOfSessionId)
          val _tmpTimestampMs: Long
          _tmpTimestampMs = _stmt.getLong(_columnIndexOfTimestampMs)
          val _tmpX: Float
          _tmpX = _stmt.getDouble(_columnIndexOfX).toFloat()
          val _tmpZ: Float
          _tmpZ = _stmt.getDouble(_columnIndexOfZ).toFloat()
          val _tmpAccumulatedDistanceM: Float
          _tmpAccumulatedDistanceM = _stmt.getDouble(_columnIndexOfAccumulatedDistanceM).toFloat()
          val _tmpInstantaneousVelocityMs: Float
          _tmpInstantaneousVelocityMs =
              _stmt.getDouble(_columnIndexOfInstantaneousVelocityMs).toFloat()
          val _tmpTrackingState: String
          _tmpTrackingState = _stmt.getText(_columnIndexOfTrackingState)
          val _tmpMotionState: String
          _tmpMotionState = _stmt.getText(_columnIndexOfMotionState)
          _item =
              PoseLogEntry(_tmpId,_tmpSessionId,_tmpTimestampMs,_tmpX,_tmpZ,_tmpAccumulatedDistanceM,_tmpInstantaneousVelocityMs,_tmpTrackingState,_tmpMotionState)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteForSession(sessionId: Long) {
    val _sql: String = "DELETE FROM pose_log WHERE sessionId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, sessionId)
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
