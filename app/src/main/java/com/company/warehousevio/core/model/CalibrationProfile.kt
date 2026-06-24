package com.company.warehousevio.core.model

/**
 * Perfil de calibración: factor de escala y deadband de ruido.
 * Persistido por Room para reutilizarse entre sesiones.
 */
data class CalibrationProfile(
    val id: Long = 0,
    val scaleFactor: Float = 1.0f,
    val noiseFloorM: Float = 0.005f,  // 5 mm por defecto
    val createdAtMs: Long = System.currentTimeMillis(),
    val label: String = "",
)
