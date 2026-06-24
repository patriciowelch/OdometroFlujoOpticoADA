package com.company.warehousevio.`data`.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.company.warehousevio.`data`.entity.EventLogEntry
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
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class EventLogDao_Impl(
  __db: RoomDatabase,
) : EventLogDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfEventLogEntry: EntityInsertAdapter<EventLogEntry>
  init {
    this.__db = __db
    this.__insertAdapterOfEventLogEntry = object : EntityInsertAdapter<EventLogEntry>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `event_log` (`id`,`sessionId`,`timestampMs`,`eventType`,`eventSource`,`x`,`z`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: EventLogEntry) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.sessionId)
        statement.bindLong(3, entity.timestampMs)
        statement.bindText(4, entity.eventType)
        statement.bindText(5, entity.eventSource)
        statement.bindDouble(6, entity.x.toDouble())
        statement.bindDouble(7, entity.z.toDouble())
      }
    }
  }

  public override suspend fun insert(entry: EventLogEntry): Long = performSuspending(__db, false,
      true) { _connection ->
    val _result: Long = __insertAdapterOfEventLogEntry.insertAndReturnId(_connection, entry)
    _result
  }

  public override fun getForSession(sessionId: Long): Flow<List<EventLogEntry>> {
    val _sql: String = "SELECT * FROM event_log WHERE sessionId = ? ORDER BY timestampMs ASC"
    return createFlow(__db, false, arrayOf("event_log")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, sessionId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSessionId: Int = getColumnIndexOrThrow(_stmt, "sessionId")
        val _columnIndexOfTimestampMs: Int = getColumnIndexOrThrow(_stmt, "timestampMs")
        val _columnIndexOfEventType: Int = getColumnIndexOrThrow(_stmt, "eventType")
        val _columnIndexOfEventSource: Int = getColumnIndexOrThrow(_stmt, "eventSource")
        val _columnIndexOfX: Int = getColumnIndexOrThrow(_stmt, "x")
        val _columnIndexOfZ: Int = getColumnIndexOrThrow(_stmt, "z")
        val _result: MutableList<EventLogEntry> = mutableListOf()
        while (_stmt.step()) {
          val _item: EventLogEntry
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSessionId: Long
          _tmpSessionId = _stmt.getLong(_columnIndexOfSessionId)
          val _tmpTimestampMs: Long
          _tmpTimestampMs = _stmt.getLong(_columnIndexOfTimestampMs)
          val _tmpEventType: String
          _tmpEventType = _stmt.getText(_columnIndexOfEventType)
          val _tmpEventSource: String
          _tmpEventSource = _stmt.getText(_columnIndexOfEventSource)
          val _tmpX: Float
          _tmpX = _stmt.getDouble(_columnIndexOfX).toFloat()
          val _tmpZ: Float
          _tmpZ = _stmt.getDouble(_columnIndexOfZ).toFloat()
          _item =
              EventLogEntry(_tmpId,_tmpSessionId,_tmpTimestampMs,_tmpEventType,_tmpEventSource,_tmpX,_tmpZ)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getForSessionOnce(sessionId: Long): List<EventLogEntry> {
    val _sql: String = "SELECT * FROM event_log WHERE sessionId = ? ORDER BY timestampMs ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, sessionId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSessionId: Int = getColumnIndexOrThrow(_stmt, "sessionId")
        val _columnIndexOfTimestampMs: Int = getColumnIndexOrThrow(_stmt, "timestampMs")
        val _columnIndexOfEventType: Int = getColumnIndexOrThrow(_stmt, "eventType")
        val _columnIndexOfEventSource: Int = getColumnIndexOrThrow(_stmt, "eventSource")
        val _columnIndexOfX: Int = getColumnIndexOrThrow(_stmt, "x")
        val _columnIndexOfZ: Int = getColumnIndexOrThrow(_stmt, "z")
        val _result: MutableList<EventLogEntry> = mutableListOf()
        while (_stmt.step()) {
          val _item: EventLogEntry
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSessionId: Long
          _tmpSessionId = _stmt.getLong(_columnIndexOfSessionId)
          val _tmpTimestampMs: Long
          _tmpTimestampMs = _stmt.getLong(_columnIndexOfTimestampMs)
          val _tmpEventType: String
          _tmpEventType = _stmt.getText(_columnIndexOfEventType)
          val _tmpEventSource: String
          _tmpEventSource = _stmt.getText(_columnIndexOfEventSource)
          val _tmpX: Float
          _tmpX = _stmt.getDouble(_columnIndexOfX).toFloat()
          val _tmpZ: Float
          _tmpZ = _stmt.getDouble(_columnIndexOfZ).toFloat()
          _item =
              EventLogEntry(_tmpId,_tmpSessionId,_tmpTimestampMs,_tmpEventType,_tmpEventSource,_tmpX,_tmpZ)
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
