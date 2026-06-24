package com.company.warehousevio.tracking

import com.company.warehousevio.core.model.TrackingFailureReason
import com.company.warehousevio.core.model.TrackingState
import com.google.ar.core.TrackingFailureReason as ArFailureReason
import com.google.ar.core.TrackingState as ArTrackingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Observa el estado de tracking de ARCore y expone alertas al ViewModel.
 * Cuando el tracking se pierde, la acumulación debe pausarse (lo hace PoseTracker).
 */
class TrackingHealth {

    data class HealthState(
        val trackingState: TrackingState,
        val failureReason: TrackingFailureReason,
        val hasGap: Boolean,
    )

    private val _state = MutableStateFlow(
        HealthState(TrackingState.STOPPED, TrackingFailureReason.NONE, false)
    )
    val state: StateFlow<HealthState> = _state

    fun update(arState: ArTrackingState, arReason: ArFailureReason) {
        val newState = arState.toModel()
        val newReason = arReason.toModel()
        val hasGap = newState != TrackingState.TRACKING
        _state.value = HealthState(newState, newReason, hasGap)
    }

    private fun ArTrackingState.toModel() = when (this) {
        ArTrackingState.TRACKING -> TrackingState.TRACKING
        ArTrackingState.PAUSED   -> TrackingState.PAUSED
        ArTrackingState.STOPPED  -> TrackingState.STOPPED
    }

    private fun ArFailureReason.toModel() = when (this) {
        ArFailureReason.NONE                   -> TrackingFailureReason.NONE
        ArFailureReason.INSUFFICIENT_FEATURES  -> TrackingFailureReason.INSUFFICIENT_FEATURES
        ArFailureReason.EXCESSIVE_MOTION       -> TrackingFailureReason.EXCESSIVE_MOTION
        ArFailureReason.INSUFFICIENT_LIGHT     -> TrackingFailureReason.INSUFFICIENT_LIGHT
        ArFailureReason.CAMERA_UNAVAILABLE     -> TrackingFailureReason.CAMERA_UNAVAILABLE
        else                                   -> TrackingFailureReason.UNKNOWN
    }
}
