package com.company.warehousevio.ui.viewmodel

import android.app.Application
import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.company.warehousevio.core.model.MotionState
import com.company.warehousevio.core.model.TrackingFailureReason
import com.company.warehousevio.core.model.TrackingState
import com.company.warehousevio.data.AppDatabase
import com.company.warehousevio.data.entity.EventLogEntry
import com.company.warehousevio.data.entity.PoseLogEntry
import com.company.warehousevio.data.entity.SessionEntity
import com.company.warehousevio.network.ProtocolMessage
import com.company.warehousevio.network.TcpTunnelTransport
import com.company.warehousevio.network.Transport
import com.company.warehousevio.tracking.ArSessionManager
import com.company.warehousevio.tracking.CalibrationEngine
import com.company.warehousevio.tracking.MotionClassifier
import com.company.warehousevio.tracking.PoseTracker
import com.company.warehousevio.tracking.TrackingHealth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val TAG = "TrackerViewModel"
private const val TCP_PORT = 9876

data class TrackerUiState(
    val isSessionActive: Boolean = false,
    val isConnected: Boolean = false,
    val accumulatedDistanceM: Float = 0f,
    val instantaneousVelocityMs: Float = 0f,
    val motionState: MotionState = MotionState.IDLE,
    val trackingState: TrackingState = TrackingState.STOPPED,
    val trackingFailureReason: TrackingFailureReason = TrackingFailureReason.NONE,
    val connectionError: String? = null,
    val torchOn: Boolean = false,
)

class TrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val arSessionManager = ArSessionManager(application)
    private val poseTracker = PoseTracker(arSessionManager)
    private val motionClassifier = MotionClassifier()
    private val trackingHealth = TrackingHealth()
    val calibrationEngine = CalibrationEngine()

    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState: StateFlow<TrackerUiState> = _uiState

    private var transport: Transport? = null
    private var currentSessionId: Long = -1
    private var trackingJob: Job? = null
    private var networkJob: Job? = null

    fun startSession(useDebugTunnel: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSessionActive = true, connectionError = null)

            // createSession() abre la cámara (SharedCamera) y llama resume() internamente.
            try {
                arSessionManager.createSession()
                // Si el usuario encendió la linterna antes de la sesión, sincronizar estado.
                if (_uiState.value.torchOn) arSessionManager.setTorch(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error iniciando ARCore: ${e.javaClass.simpleName} — ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isSessionActive = false,
                    connectionError = "ARCore: ${e.javaClass.simpleName}: ${e.message}",
                )
                return@launch
            }

            val session = SessionEntity(startTimestampMs = System.currentTimeMillis())
            currentSessionId = db.sessionDao().insert(session)

            startTracking()
            startTrackingStateMonitor()
            connectTransport(useDebugTunnel)
        }
    }

    private fun startTrackingStateMonitor() {
        poseTracker.trackingStateFlow
            .onEach { (state, reason) ->
                _uiState.value = _uiState.value.copy(
                    trackingState = state,
                    trackingFailureReason = reason,
                )
            }
            .launchIn(viewModelScope)
    }

    private fun startTracking() {
        trackingJob = poseTracker.poseFlow
            .onEach { snapshot ->
                val motionEvent = motionClassifier.update(
                    dx = kotlin.math.sin(snapshot.pose.heading) * snapshot.rawDelta,
                    dz = kotlin.math.cos(snapshot.pose.heading) * snapshot.rawDelta,
                    velocity = snapshot.instantaneousVelocityMs,
                    timestampMs = snapshot.pose.timestampMs,
                    x = snapshot.pose.x,
                    z = snapshot.pose.z,
                )

                _uiState.value = _uiState.value.copy(
                    accumulatedDistanceM = snapshot.accumulatedDistanceM,
                    instantaneousVelocityMs = snapshot.instantaneousVelocityMs,
                    motionState = motionClassifier.currentState,
                    trackingState = snapshot.pose.trackingState,
                )

                // Persistir en Room
                if (currentSessionId > 0) {
                    db.poseLogDao().insert(
                        PoseLogEntry(
                            sessionId = currentSessionId,
                            timestampMs = snapshot.pose.timestampMs,
                            x = snapshot.pose.x,
                            z = snapshot.pose.z,
                            accumulatedDistanceM = snapshot.accumulatedDistanceM,
                            instantaneousVelocityMs = snapshot.instantaneousVelocityMs,
                            trackingState = snapshot.pose.trackingState.name,
                            motionState = motionClassifier.currentState.name,
                        )
                    )
                }

                // Enviar al Monitor
                transport?.runCatching {
                    send(
                        ProtocolMessage.PoseUpdate(
                            ts = snapshot.pose.timestampMs,
                            x = snapshot.pose.x,
                            z = snapshot.pose.z,
                            heading = snapshot.pose.heading,
                            vInst = snapshot.instantaneousVelocityMs,
                            distAccum = snapshot.accumulatedDistanceM,
                            trackingState = snapshot.pose.trackingState.name,
                            motionState = motionClassifier.currentState.name,
                        )
                    )
                }

                if (motionEvent != null) {
                    transport?.runCatching {
                        send(ProtocolMessage.MotionEventMsg(motionEvent.type, motionEvent.timestampMs, motionEvent.x, motionEvent.z))
                    }
                    if (currentSessionId > 0) {
                        db.eventLogDao().insert(
                            EventLogEntry(
                                sessionId = currentSessionId,
                                timestampMs = motionEvent.timestampMs,
                                eventType = motionEvent.type.name,
                                eventSource = "AUTO",
                                x = motionEvent.x,
                                z = motionEvent.z,
                            )
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun connectTransport(useDebugTunnel: Boolean) {
        val t = TcpTunnelTransport(port = TCP_PORT, isServer = false)
        transport = t
        networkJob = viewModelScope.launch {
            try {
                t.start()
                _uiState.value = _uiState.value.copy(isConnected = true, connectionError = null)
                Log.d(TAG, "Conectado al Monitor")
                t.incoming.collect { message ->
                    handleIncomingMessage(message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de red: ${e.message}")
                _uiState.value = _uiState.value.copy(isConnected = false, connectionError = e.message)
            }
        }
    }

    private fun handleIncomingMessage(message: ProtocolMessage) {
        when (message) {
            is ProtocolMessage.SessionControl -> {
                when (message.command) {
                    ProtocolMessage.SessionControl.Command.STOP -> stopSession()
                    else -> {}
                }
            }
            else -> {}
        }
    }

    fun stopSession() {
        viewModelScope.launch {
            trackingJob?.cancel()
            networkJob?.cancel()
            transport?.close()
            arSessionManager.destroy()
            poseTracker.reset()
            if (currentSessionId > 0) {
                val session = db.sessionDao().getById(currentSessionId)
                session?.let {
                    db.sessionDao().update(it.copy(endTimestampMs = System.currentTimeMillis()))
                }
            }
            _uiState.value = TrackerUiState()
            currentSessionId = -1
        }
    }

    fun toggleTorch() {
        val newState = !_uiState.value.torchOn
        if (_uiState.value.isSessionActive) {
            // ARCore tiene la cámara: inyectar torch en sus CaptureRequest vía SharedCamera.
            viewModelScope.launch {
                arSessionManager.setTorch(newState)
                _uiState.value = _uiState.value.copy(torchOn = newState)
            }
        } else {
            // Sin sesión: usar CameraManager directamente.
            val cm = getApplication<Application>().getSystemService(CameraManager::class.java)
            val cameraId = cm.cameraIdList.firstOrNull() ?: return
            runCatching { cm.setTorchMode(cameraId, newState) }
                .onSuccess { _uiState.value = _uiState.value.copy(torchOn = newState) }
                .onFailure { Log.e(TAG, "Error al cambiar flash: ${it.message}") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (_uiState.value.torchOn) {
            viewModelScope.launch { arSessionManager.setTorch(false) }
        }
        viewModelScope.launch { arSessionManager.destroy() }
    }
}
