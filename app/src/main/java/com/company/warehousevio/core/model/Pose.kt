package com.company.warehousevio.core.model

/**
 * Pose 2D proyectada al plano X-Z de ARCore.
 * Y se descarta (vibración vertical). Heading en radianes respecto al eje +Z.
 */
data class Pose(
    val timestampMs: Long,
    val x: Float,
    val z: Float,
    val heading: Float,
    val trackingState: TrackingState = TrackingState.TRACKING,
)
