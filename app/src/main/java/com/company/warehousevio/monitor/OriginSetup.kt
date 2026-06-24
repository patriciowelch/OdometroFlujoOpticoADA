package com.company.warehousevio.monitor

/**
 * Configuración del origen del mapa: posición en píxeles del canvas y heading
 * de referencia (radianes) que corresponde a la dirección inicial de la traspaleta.
 */
data class OriginSetup(
    val pixelX: Float = 0f,
    val pixelY: Float = 0f,
    val headingRad: Float = 0f,
    val scalePixelsPerMeter: Float = 50f,
    val isSet: Boolean = false,
)
