package com.company.warehousevio.tracking

import com.company.warehousevio.core.geometry.forwardDot
import com.company.warehousevio.core.model.MotionEvent
import com.company.warehousevio.core.model.MotionEventType
import com.company.warehousevio.core.model.MotionState

/**
 * Máquina de estados con histéresis para clasificar el movimiento de la traspaleta.
 *
 * [forwardAxisX] y [forwardAxisZ] definen el eje "adelante" del dispositivo montado.
 * Ajustable según la orientación de montaje física en la traspaleta.
 *
 * [vStart]: velocidad umbral para detectar inicio de movimiento (m/s).
 * [vStop]:  velocidad umbral (menor) para detectar frenada/parada. Histéresis: vStop < vStart.
 * [stopWindowMs]: tiempo sostenido bajo vStop antes de confirmar parada.
 */
class MotionClassifier(
    var forwardAxisX: Float = 0f,
    var forwardAxisZ: Float = 1f,
    var vStart: Float = 0.05f,
    var vStop: Float = 0.02f,
    var stopWindowMs: Long = 500L,
) {
    var currentState: MotionState = MotionState.IDLE
        private set

    private var belowStopSinceMs: Long = 0L

    /**
     * Actualiza la máquina de estados con la última muestra.
     * Devuelve un [MotionEvent] si hay transición, o null si el estado no cambia.
     */
    fun update(
        dx: Float,
        dz: Float,
        velocity: Float,
        timestampMs: Long,
        x: Float,
        z: Float,
    ): MotionEvent? {
        val dot = forwardDot(dx, dz, forwardAxisX, forwardAxisZ)

        return when (currentState) {
            MotionState.IDLE -> {
                if (velocity >= vStart) {
                    val newState = if (dot >= 0) MotionState.FORWARD else MotionState.REVERSE
                    currentState = newState
                    belowStopSinceMs = 0L
                    MotionEvent(
                        type = if (newState == MotionState.FORWARD) MotionEventType.START_FORWARD
                               else MotionEventType.START_REVERSE,
                        timestampMs = timestampMs,
                        x = x,
                        z = z,
                    )
                } else null
            }

            MotionState.FORWARD, MotionState.REVERSE -> {
                if (velocity < vStop) {
                    if (belowStopSinceMs == 0L) belowStopSinceMs = timestampMs
                    val elapsed = timestampMs - belowStopSinceMs
                    if (elapsed >= stopWindowMs) {
                        currentState = MotionState.IDLE
                        belowStopSinceMs = 0L
                        MotionEvent(
                            type = MotionEventType.BRAKE,
                            timestampMs = timestampMs,
                            x = x,
                            z = z,
                        )
                    } else null
                } else {
                    belowStopSinceMs = 0L
                    // Detectar cambio de dirección en marcha
                    val expectedForward = currentState == MotionState.FORWARD
                    val actualForward = dot >= 0
                    if (expectedForward != actualForward && velocity >= vStart) {
                        val newState = if (actualForward) MotionState.FORWARD else MotionState.REVERSE
                        currentState = newState
                        MotionEvent(
                            type = if (newState == MotionState.FORWARD) MotionEventType.START_FORWARD
                                   else MotionEventType.START_REVERSE,
                            timestampMs = timestampMs,
                            x = x,
                            z = z,
                        )
                    } else null
                }
            }
        }
    }

    fun reset() {
        currentState = MotionState.IDLE
        belowStopSinceMs = 0L
    }
}
