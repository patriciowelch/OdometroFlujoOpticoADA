package com.company.warehousevio.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.company.warehousevio.App
import com.company.warehousevio.core.model.ManualEventType
import com.company.warehousevio.core.model.MotionEvent
import com.company.warehousevio.core.model.MotionEventType
import com.company.warehousevio.core.model.MotionState
import com.company.warehousevio.core.model.Pose
import com.company.warehousevio.core.model.TrackingFailureReason
import com.company.warehousevio.core.model.TrackingState
import com.company.warehousevio.data.AppDatabase
import com.company.warehousevio.data.entity.EventLogEntry
import com.company.warehousevio.data.entity.SessionEntity
import com.company.warehousevio.data.export.CsvExporter
import com.company.warehousevio.monitor.EventMarker
import com.company.warehousevio.monitor.LiveSessionState
import com.company.warehousevio.monitor.OriginSetup
import com.company.warehousevio.network.ProtocolMessage
import com.company.warehousevio.network.TcpTunnelTransport
import com.company.warehousevio.network.Transport
import com.company.warehousevio.network.WifiDirectState
import com.company.warehousevio.network.WifiDirectTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "MonitorViewModel"
private const val TCP_PORT = 9876
private const val PREFS_NAME = "monitor_prefs"
private const val KEY_MAP_URI = "map_image_uri"

class MonitorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val eventMarker = EventMarker()
    private val csvExporter = CsvExporter(
        context = application,
        poseLogDao = db.poseLogDao(),
        eventLogDao = db.eventLogDao(),
    )
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Wi-Fi Direct manager del singleton App. */
    val wifiDirectManager get() = getApplication<App>().wifiDirectManager
    val wifiDirectState: StateFlow<WifiDirectState> = wifiDirectManager.state

    private val _liveState = MutableStateFlow(LiveSessionState())
    val liveState: StateFlow<LiveSessionState> = _liveState

    private val _originSetup = MutableStateFlow(OriginSetup())
    val originSetup: StateFlow<OriginSetup> = _originSetup

    /** Todas las sesiones guardadas en Room, para la pantalla de historial. */
    val allSessions = db.sessionDao().getAllSessions()

    /** URI del croquis del almacén seleccionado por el usuario. */
    private val _mapImageUri = MutableStateFlow<Uri?>(null)
    val mapImageUri: StateFlow<Uri?> = _mapImageUri

    private var transport: Transport? = null
    private var networkJob: Job? = null
    private var currentSessionId: Long = -1

    init {
        // Restaurar URI del mapa guardada en SharedPreferences
        val savedUri = prefs.getString(KEY_MAP_URI, null)
        if (savedUri != null) {
            _mapImageUri.value = Uri.parse(savedUri)
        }
    }

    /**
     * Guarda la URI del croquis y toma permiso persistible para que
     * la app pueda seguir leyendo el archivo entre reinicios.
     */
    fun setMapImage(uri: Uri) {
        runCatching {
            getApplication<Application>().contentResolver
                .takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
        }
        _mapImageUri.value = uri
        prefs.edit().putString(KEY_MAP_URI, uri.toString()).apply()
    }

    fun startListening(useWifiDirect: Boolean = false) {
        networkJob = viewModelScope.launch {
            try {
                db.sessionDao().insert(SessionEntity(startTimestampMs = System.currentTimeMillis()))
                    .also { currentSessionId = it }

                val t: Transport = if (useWifiDirect) {
                    // Crear grupo P2P; el Monitor actúa como Group Owner
                    wifiDirectManager.createGroup()
                    // Esperar hasta que el grupo esté listo
                    wifiDirectManager.state.first { it is WifiDirectState.GroupOwner || it is WifiDirectState.Error }
                    if (wifiDirectManager.state.value is WifiDirectState.Error) {
                        val err = (wifiDirectManager.state.value as WifiDirectState.Error).msg
                        _liveState.value = _liveState.value.copy(isConnected = false)
                        Log.e(TAG, "Wi-Fi Direct error: $err")
                        return@launch
                    }
                    WifiDirectTransport(port = TCP_PORT, isServer = true)
                } else {
                    TcpTunnelTransport(port = TCP_PORT, isServer = true)
                }
                transport = t

                t.start()
                _liveState.value = _liveState.value.copy(isConnected = true)
                Log.d(TAG, "Tracker conectado")

                t.incoming.collect { message -> handleMessage(message) }
            } catch (e: Exception) {
                Log.e(TAG, "Error de red: ${e.message}")
                _liveState.value = _liveState.value.copy(isConnected = false)
            }
        }
    }

    private fun handleMessage(message: ProtocolMessage) {
        when (message) {
            is ProtocolMessage.PoseUpdate -> {
                val pose = Pose(
                    timestampMs = message.ts,
                    x = message.x,
                    z = message.z,
                    heading = message.heading,
                    trackingState = runCatching { TrackingState.valueOf(message.trackingState) }.getOrDefault(TrackingState.PAUSED),
                )
                val motionState = runCatching { MotionState.valueOf(message.motionState) }.getOrDefault(MotionState.IDLE)
                val newTrajectory = _liveState.value.trajectory + Pair(message.x, message.z)

                _liveState.value = _liveState.value.copy(
                    latestPose = pose,
                    instantaneousVelocityMs = message.vInst,
                    accumulatedDistanceM = message.distAccum,
                    motionState = motionState,
                    trackingState = pose.trackingState,
                    trajectory = newTrajectory,
                )
            }

            is ProtocolMessage.MotionEventMsg -> {
                val event = MotionEvent(
                    type = message.type,
                    timestampMs = message.ts,
                    x = message.x,
                    z = message.z,
                )
                val updated = (_liveState.value.recentMotionEvents + event).takeLast(20)
                _liveState.value = _liveState.value.copy(recentMotionEvents = updated)
            }

            is ProtocolMessage.TrackingAlert -> {
                val reason = message.reason
                _liveState.value = _liveState.value.copy(
                    trackingState = TrackingState.PAUSED,
                    trackingFailureReason = reason,
                )
            }

            else -> {}
        }
    }

    fun markManualEvent(type: ManualEventType) {
        val pose = _liveState.value.latestPose ?: return
        val event = eventMarker.mark(type, pose.x, pose.z)
        viewModelScope.launch {
            if (currentSessionId > 0) {
                db.eventLogDao().insert(
                    EventLogEntry(
                        sessionId = currentSessionId,
                        timestampMs = event.timestampMs,
                        eventType = event.type.name,
                        eventSource = "MANUAL",
                        x = event.x,
                        z = event.z,
                    )
                )
            }
        }
    }

    fun resetTrajectory() {
        _liveState.value = _liveState.value.copy(
            trajectory = emptyList(),
            accumulatedDistanceM = 0f,
            recentMotionEvents = emptyList(),
            latestPose = null,
        )
        _originSetup.value = OriginSetup()
    }

    fun setOrigin(pixelX: Float, pixelY: Float, headingRad: Float, scalePixelsPerMeter: Float) {
        _originSetup.value = OriginSetup(pixelX, pixelY, headingRad, scalePixelsPerMeter, isSet = true)
        viewModelScope.launch {
            transport?.runCatching {
                send(ProtocolMessage.SetOrigin(pixelX, pixelY, headingRad))
            }
        }
    }

    fun exportCsv(context: Context, sessionId: Long = currentSessionId) {
        viewModelScope.launch {
            val uri = csvExporter.export(sessionId)
            val intent = csvExporter.buildShareIntent(uri)
            context.startActivity(android.content.Intent.createChooser(intent, "Compartir CSV"))
        }
    }

    fun stopListening() {
        networkJob?.cancel()
        viewModelScope.launch {
            transport?.close()
            if (currentSessionId > 0) {
                val session = db.sessionDao().getById(currentSessionId)
                session?.let {
                    db.sessionDao().update(it.copy(endTimestampMs = System.currentTimeMillis()))
                }
            }
            _liveState.value = LiveSessionState()
            currentSessionId = -1
        }
    }
}
