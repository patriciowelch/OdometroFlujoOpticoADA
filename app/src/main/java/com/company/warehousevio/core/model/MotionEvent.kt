package com.company.warehousevio.core.model

/**
 * Evento de movimiento generado por el MotionClassifier o marcado manualmente en el Monitor.
 */
data class MotionEvent(
    val type: MotionEventType,
    val timestampMs: Long,
    val x: Float,
    val z: Float,
)
