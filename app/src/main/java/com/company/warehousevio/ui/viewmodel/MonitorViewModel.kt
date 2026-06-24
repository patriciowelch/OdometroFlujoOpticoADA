package com.company.warehousevio.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "MonitorViewModel"
private const val TCP_PORT = 9876

class MonitorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val eventMarker = EventMarker()
    private val csvExporter = CsvExporter(
        context = application,
        poseLogDao = db.poseLogDao(),
        eventLogDao = db.eventLogDao(),
    )

    private val _liveState = MutableStateFlow(LiveSessionState())
    val liveState: StateFlow<LiveSessionState> = _liveState

    private val _originSetup = MutableStateFlow(OriginSetup())
    val originSetup: StateFlow<OriginSetup> = _originSetup

    private var transport: Transport? = null
    private var networkJob: Job? = null
    private var currentSessionId: Long = -1

    fun startListening() {
        val t = TcpTunnelTransport(port = TCP_PORT, isServer = true)
        transport = t
        networkJob = viewModelScope.launch {
            try {
                db.sessionDao().insert(SessionEntity(startTimestampMs = System.currentTimeMillis()))
                    .also { currentSessionId = it }

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
