package com.company.warehousevio.monitor

import com.company.warehousevio.core.model.MotionEvent
import com.company.warehousevio.core.model.MotionState
import com.company.warehousevio.core.model.Pose
import com.company.warehousevio.core.model.TrackingFailureReason
import com.company.warehousevio.core.model.TrackingState

/**
 * Estado en vivo de la sesión recibido por el Monitor desde el Tracker.
 * Inmutable para UDF con StateFlow.
 */
data class LiveSessionState(
    val latestPose: Pose? = null,
    val instantaneousVelocityMs: Float = 0f,
    val accumulatedDistanceM: Float = 0f,
    val motionState: MotionState = MotionState.IDLE,
    val trackingState: TrackingState = TrackingState.STOPPED,
    val trackingFailureReason: TrackingFailureReason = TrackingFailureReason.NONE,
    val recentMotionEvents: List<MotionEvent> = emptyList(),
    val trajectory: List<Pair<Float, Float>> = emptyList(),  // lista de (x, z) para el Canvas
    val isConnected: Boolean = false,
)
