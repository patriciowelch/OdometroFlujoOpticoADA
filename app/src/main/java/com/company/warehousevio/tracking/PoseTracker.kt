package com.company.warehousevio.tracking

import android.util.Log
import com.company.warehousevio.core.geometry.deltaDistanceXZ
import com.company.warehousevio.core.geometry.headingXZ
import com.company.warehousevio.core.model.CalibrationProfile
import com.company.warehousevio.core.model.Pose
import com.company.warehousevio.core.model.TrackingState
import com.company.warehousevio.core.time.monotonicNowMs
import com.google.ar.core.TrackingState as ArTrackingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val TAG = "PoseTracker"
private const val SAMPLE_INTERVAL_MS = 33L  // ~30 Hz

/**
 * Loop de muestreo de pose ARCore.
 *
 * Emite [PoseSnapshot] por cada muestra válida. Aplica:
 *   - deadband de ruido (noiseFloor): Δd < noiseFloor → no acumular
 *   - descarte del primer Δd tras recuperar tracking (evita saltos de re-localización)
 *   - factor de escala del CalibrationProfile
 */
class PoseTracker(
    private val arSessionManager: ArSessionManager,
) {
    data class PoseSnapshot(
        val pose: Pose,
        val instantaneousVelocityMs: Float,
        val accumulatedDistanceM: Float,
        val motionState: com.company.warehousevio.core.model.MotionState,
        val rawDelta: Float,
    )

    var calibration: CalibrationProfile = CalibrationProfile()

    private var accumulatedDistance = 0f
    private var lastX = Float.NaN
    private var lastZ = Float.NaN
    private var lastTimestampMs = 0L
    private var wasTracking = false
    private var skipNextDelta = false

    // Expone el estado de tracking en tiempo real (incluso cuando no hay pose).
    val trackingStateFlow = kotlinx.coroutines.flow.MutableStateFlow(
        com.company.warehousevio.core.model.TrackingState.STOPPED to
        com.company.warehousevio.core.model.TrackingFailureReason.NONE
    )

    val poseFlow: Flow<PoseSnapshot> = flow {
        while (true) {
            val frame = arSessionManager.update()
            val nowMs = monotonicNowMs()

            if (frame == null) {
                delay(SAMPLE_INTERVAL_MS)
                continue
            }

            val camera = frame.camera
            val currentlyTracking = camera.trackingState == ArTrackingState.TRACKING

            // Actualizar estado de tracking aunque no haya pose válida
            val modelState = when (camera.trackingState) {
                ArTrackingState.TRACKING -> com.company.warehousevio.core.model.TrackingState.TRACKING
                ArTrackingState.PAUSED   -> com.company.warehousevio.core.model.TrackingState.PAUSED
                ArTrackingState.STOPPED  -> com.company.warehousevio.core.model.TrackingState.STOPPED
            }
            val modelReason = when (camera.trackingFailureReason) {
                com.google.ar.core.TrackingFailureReason.INSUFFICIENT_FEATURES -> com.company.warehousevio.core.model.TrackingFailureReason.INSUFFICIENT_FEATURES
                com.google.ar.core.TrackingFailureReason.EXCESSIVE_MOTION      -> com.company.warehousevio.core.model.TrackingFailureReason.EXCESSIVE_MOTION
                com.google.ar.core.TrackingFailureReason.INSUFFICIENT_LIGHT    -> com.company.warehousevio.core.model.TrackingFailureReason.INSUFFICIENT_LIGHT
                com.google.ar.core.TrackingFailureReason.CAMERA_UNAVAILABLE    -> com.company.warehousevio.core.model.TrackingFailureReason.CAMERA_UNAVAILABLE
                else -> com.company.warehousevio.core.model.TrackingFailureReason.NONE
            }
            trackingStateFlow.value = modelState to modelReason

            if (!currentlyTracking) {
                wasTracking = false
                skipNextDelta = true
                delay(SAMPLE_INTERVAL_MS)
                continue
            }

            // Primera muestra tras recuperar tracking → descartar Δd
            if (!wasTracking) {
                wasTracking = true
                skipNextDelta = true
                Log.d(TAG, "Tracking recuperado — descartando primer Δd")
            }

            val translation = camera.pose.translation  // [x, y, z]
            val x = translation[0]
            val z = translation[2]

            val dt = if (lastTimestampMs == 0L) SAMPLE_INTERVAL_MS else (nowMs - lastTimestampMs)
            lastTimestampMs = nowMs

            val heading = if (!lastX.isNaN()) headingXZ(x - lastX, z - lastZ) else 0f

            val delta = if (!lastX.isNaN()) {
                val rawDelta = deltaDistanceXZ(lastX, lastZ, x, z) * calibration.scaleFactor
                if (skipNextDelta || rawDelta < calibration.noiseFloorM) {
                    skipNextDelta = false
                    0f
                } else rawDelta
            } else 0f

            lastX = x
            lastZ = z

            accumulatedDistance += delta
            val velocity = if (dt > 0) delta / (dt / 1000f) else 0f

            emit(
                PoseSnapshot(
                    pose = Pose(
                        timestampMs = nowMs,
                        x = x,
                        z = z,
                        heading = heading,
                        trackingState = TrackingState.TRACKING,
                    ),
                    instantaneousVelocityMs = velocity,
                    accumulatedDistanceM = accumulatedDistance,
                    motionState = com.company.warehousevio.core.model.MotionState.IDLE,  // actualizado por MotionClassifier
                    rawDelta = delta,
                )
            )

            delay(SAMPLE_INTERVAL_MS)
        }
    }

    fun reset() {
        accumulatedDistance = 0f
        lastX = Float.NaN
        lastZ = Float.NaN
        lastTimestampMs = 0L
        wasTracking = false
        skipNextDelta = false
    }
}
