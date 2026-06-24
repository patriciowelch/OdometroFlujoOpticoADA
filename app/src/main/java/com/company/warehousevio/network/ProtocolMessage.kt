package com.company.warehousevio.network

import com.company.warehousevio.core.model.ManualEventType
import com.company.warehousevio.core.model.MotionEventType
import com.company.warehousevio.core.model.TrackingFailureReason
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mensajes del protocolo NDJSON entre Tracker y Monitor.
 * Cada mensaje se serializa como una línea JSON terminada en '\n'.
 */
@Serializable
sealed class ProtocolMessage {

    // ── Tracker → Monitor ────────────────────────────────────────────────────

    /** Actualización de pose a ~10-30 Hz. */
    @Serializable
    @SerialName("pose_update")
    data class PoseUpdate(
        val ts: Long,
        val x: Float,
        val z: Float,
        val heading: Float,
        val vInst: Float,          // velocidad instantánea m/s
        val distAccum: Float,      // distancia acumulada m
        val trackingState: String,
        val motionState: String,
    ) : ProtocolMessage()

    /** Evento de movimiento automático (START_FORWARD, START_REVERSE, BRAKE). */
    @Serializable
    @SerialName("motion_event")
    data class MotionEventMsg(
        val type: MotionEventType,
        val ts: Long,
        val x: Float,
        val z: Float,
    ) : ProtocolMessage()

    /** Alerta de pérdida de tracking. */
    @Serializable
    @SerialName("tracking_alert")
    data class TrackingAlert(
        val reason: TrackingFailureReason,
        val ts: Long,
    ) : ProtocolMessage()

    /** Estado de calibración (escala y noiseFloor vigentes). */
    @Serializable
    @SerialName("calibration_status")
    data class CalibrationStatus(
        val scaleFactor: Float,
        val noiseFloorM: Float,
    ) : ProtocolMessage()

    // ── Monitor → Tracker ────────────────────────────────────────────────────

    /** Control de sesión desde el Monitor. */
    @Serializable
    @SerialName("session_control")
    data class SessionControl(
        val command: Command,
    ) : ProtocolMessage() {
        enum class Command { START, PAUSE, STOP }
    }

    /** El Monitor comunica el origen y heading de referencia del mapa. */
    @Serializable
    @SerialName("set_origin")
    data class SetOrigin(
        val x: Float,
        val z: Float,
        val heading: Float,
    ) : ProtocolMessage()

    // ── Monitor → ambos (evento manual sellado en el Monitor) ────────────────

    @Serializable
    @SerialName("manual_event")
    data class ManualEvent(
        val type: ManualEventType,
        val ts: Long,
        val x: Float,
        val z: Float,
    ) : ProtocolMessage()
}
