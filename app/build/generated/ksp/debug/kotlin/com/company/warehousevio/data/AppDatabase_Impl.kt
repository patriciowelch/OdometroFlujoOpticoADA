package com.company.warehousevio.`data`

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.company.warehousevio.`data`.dao.CalibrationProfileDao
import com.company.warehousevio.`data`.dao.CalibrationProfileDao_Impl
import com.company.warehousevio.`data`.dao.EventLogDao
import com.company.warehousevio.`data`.dao.EventLogDao_Impl
import com.company.warehousevio.`data`.dao.PoseLogDao
import com.company.warehousevio.`data`.dao.PoseLogDao_Impl
import com.company.warehousevio.`data`.dao.SessionDao
import com.company.warehousevio.`data`.dao.SessionDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _sessionDao: Lazy<SessionDao> = lazy {
    SessionDao_Impl(this)
  }

  private val _poseLogDao: Lazy<PoseLogDao> = lazy {
    PoseLogDao_Impl(this)
  }

  private val _eventLogDao: Lazy<EventLogDao> = lazy {
    EventLogDao_Impl(this)
  }

  private val _calibrationProfileDao: Lazy<CalibrationProfileDao> = lazy {
    CalibrationProfileDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1,
        "0a256f0ff77e6e057e66e380dfad459a", "954dcda8e4849524cec5296e3a389499") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startTimestampMs` INTEGER NOT NULL, `endTimestampMs` INTEGER, `operatorName` TEXT NOT NULL, `notes` TEXT NOT NULL, `calibrationProfileId` INTEGER)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `pose_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `timestampMs` INTEGER NOT NULL, `x` REAL NOT NULL, `z` REAL NOT NULL, `accumulatedDistanceM` REAL NOT NULL, `instantaneousVelocityMs` REAL NOT NULL, `trackingState` TEXT NOT NULL, `motionState` TEXT NOT NULL, FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_pose_log_sessionId` ON `pose_log` (`sessionId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `event_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `timestampMs` INTEGER NOT NULL, `eventType` TEXT NOT NULL, `eventSource` TEXT NOT NULL, `x` REAL NOT NULL, `z` REAL NOT NULL, FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_event_log_sessionId` ON `event_log` (`sessionId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `calibration_profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `scaleFactor` REAL NOT NULL, `noiseFloorM` REAL NOT NULL, `createdAtMs` INTEGER NOT NULL, `label` TEXT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '0a256f0ff77e6e057e66e380dfad459a')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `sessions`")
        connection.execSQL("DROP TABLE IF EXISTS `pose_log`")
        connection.execSQL("DROP TABLE IF EXISTS `event_log`")
        connection.execSQL("DROP TABLE IF EXISTS `calibration_profiles`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsSessions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSessions.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("startTimestampMs", TableInfo.Column("startTimestampMs", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("endTimestampMs", TableInfo.Column("endTimestampMs", "INTEGER", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("operatorName", TableInfo.Column("operatorName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("notes", TableInfo.Column("notes", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("calibrationProfileId", TableInfo.Column("calibrationProfileId",
            "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSessions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSessions: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSessions: TableInfo = TableInfo("sessions", _columnsSessions, _foreignKeysSessions,
            _indicesSessions)
        val _existingSessions: TableInfo = read(connection, "sessions")
        if (!_infoSessions.equals(_existingSessions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |sessions(com.company.warehousevio.data.entity.SessionEntity).
              | Expected:
              |""".trimMargin() + _infoSessions + """
              |
              | Found:
              |""".trimMargin() + _existingSessions)
        }
        val _columnsPoseLog: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPoseLog.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPoseLog.put("sessionId", TableInfo.Column("sessionId", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPoseLog.put("timestampMs", TableInfo.Column("timestampMs", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPoseLog.put("x", TableInfo.Column("x", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPoseLog.put("z", TableInfo.Column("z", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPoseLog.put("accumulatedDistanceM", TableInfo.Column("accumulatedDistanceM", "REAL",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPoseLog.put("instantaneousVelocityMs", TableInfo.Column("instantaneousVelocityMs",
            "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPoseLog.put("trackingState", TableInfo.Column("trackingState", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPoseLog.put("motionState", TableInfo.Column("motionState", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPoseLog: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysPoseLog.add(TableInfo.ForeignKey("sessions", "CASCADE", "NO ACTION",
            listOf("sessionId"), listOf("id")))
        val _indicesPoseLog: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesPoseLog.add(TableInfo.Index("index_pose_log_sessionId", false, listOf("sessionId"),
            listOf("ASC")))
        val _infoPoseLog: TableInfo = TableInfo("pose_log", _columnsPoseLog, _foreignKeysPoseLog,
            _indicesPoseLog)
        val _existingPoseLog: TableInfo = read(connection, "pose_log")
        if (!_infoPoseLog.equals(_existingPoseLog)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |pose_log(com.company.warehousevio.data.entity.PoseLogEntry).
              | Expected:
              |""".trimMargin() + _infoPoseLog + """
              |
              | Found:
              |""".trimMargin() + _existingPoseLog)
        }
        val _columnsEventLog: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsEventLog.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEventLog.put("sessionId", TableInfo.Column("sessionId", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEventLog.put("timestampMs", TableInfo.Column("timestampMs", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEventLog.put("eventType", TableInfo.Column("eventType", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEventLog.put("eventSource", TableInfo.Column("eventSource", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEventLog.put("x", TableInfo.Column("x", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEventLog.put("z", TableInfo.Column("z", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysEventLog: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysEventLog.add(TableInfo.ForeignKey("sessions", "CASCADE", "NO ACTION",
            listOf("sessionId"), listOf("id")))
        val _indicesEventLog: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesEventLog.add(TableInfo.Index("index_event_log_sessionId", false,
            listOf("sessionId"), listOf("ASC")))
        val _infoEventLog: TableInfo = TableInfo("event_log", _columnsEventLog,
            _foreignKeysEventLog, _indicesEventLog)
        val _existingEventLog: TableInfo = read(connection, "event_log")
        if (!_infoEventLog.equals(_existingEventLog)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |event_log(com.company.warehousevio.data.entity.EventLogEntry).
              | Expected:
              |""".trimMargin() + _infoEventLog + """
              |
              | Found:
              |""".trimMargin() + _existingEventLog)
        }
        val _columnsCalibrationProfiles: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCalibrationProfiles.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCalibrationProfiles.put("scaleFactor", TableInfo.Column("scaleFactor", "REAL", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCalibrationProfiles.put("noiseFloorM", TableInfo.Column("noiseFloorM", "REAL", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCalibrationProfiles.put("createdAtMs", TableInfo.Column("createdAtMs", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCalibrationProfiles.put("label", TableInfo.Column("label", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCalibrationProfiles: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCalibrationProfiles: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoCalibrationProfiles: TableInfo = TableInfo("calibration_profiles",
            _columnsCalibrationProfiles, _foreignKeysCalibrationProfiles,
            _indicesCalibrationProfiles)
        val _existingCalibrationProfiles: TableInfo = read(connection, "calibration_profiles")
        if (!_infoCalibrationProfiles.equals(_existingCalibrationProfiles)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |calibration_profiles(com.company.warehousevio.data.entity.CalibrationProfileEntity).
              | Expected:
              |""".trimMargin() + _infoCalibrationProfiles + """
              |
              | Found:
              |""".trimMargin() + _existingCalibrationProfiles)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "sessions", "pose_log",
        "event_log", "calibration_profiles")
  }

  public override fun clearAllTables() {
    super.performClear(true, "sessions", "pose_log", "event_log", "calibration_profiles")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(SessionDao::class, SessionDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(PoseLogDao::class, PoseLogDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(EventLogDao::class, EventLogDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CalibrationProfileDao::class,
        CalibrationProfileDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun sessionDao(): SessionDao = _sessionDao.value

  public override fun poseLogDao(): PoseLogDao = _poseLogDao.value

  public override fun eventLogDao(): EventLogDao = _eventLogDao.value

  public override fun calibrationProfileDao(): CalibrationProfileDao = _calibrationProfileDao.value
}
