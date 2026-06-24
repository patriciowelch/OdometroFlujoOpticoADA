package com.company.warehousevio.core.geometry

import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Distancia incremental en el plano X-Z de ARCore.
 * Y se ignora (vibración vertical). Resultado en metros.
 */
fun deltaDistanceXZ(x1: Float, z1: Float, x2: Float, z2: Float): Float {
    val dx = x2 - x1
    val dz = z2 - z1
    return sqrt(dx * dx + dz * dz)
}

/**
 * Heading en radianes calculado a partir del vector de traslación incremental en X-Z.
 * 0 rad = dirección +Z, aumenta en sentido antihorario.
 */
fun headingXZ(dx: Float, dz: Float): Float = atan2(dx, dz)

/**
 * Producto punto entre el vector de movimiento (dx, dz) y el eje "adelante"
 * del dispositivo montado (forwardX, forwardZ).
 * Positivo → avance, negativo → retroceso.
 */
fun forwardDot(dx: Float, dz: Float, forwardX: Float, forwardZ: Float): Float =
    dx * forwardX + dz * forwardZ

/**
 * Convierte coordenadas de mundo ARCore (x, z) a píxeles en el canvas del Monitor.
 * origin: píxel del punto de inicio. scale: píxeles por metro. yAxisFlip: true para
 * invertir eje Z (pantalla Y crece hacia abajo, Z en ARCore "hacia la cámara").
 */
fun worldToCanvas(
    worldX: Float,
    worldZ: Float,
    originPxX: Float,
    originPxY: Float,
    scale: Float,
    headingOffsetRad: Float = 0f,
): Pair<Float, Float> {
    val cos = kotlin.math.cos(headingOffsetRad)
    val sin = kotlin.math.sin(headingOffsetRad)
    val rotX = worldX * cos - worldZ * sin
    val rotZ = worldX * sin + worldZ * cos
    return Pair(originPxX + rotX * scale, originPxY - rotZ * scale)
}
