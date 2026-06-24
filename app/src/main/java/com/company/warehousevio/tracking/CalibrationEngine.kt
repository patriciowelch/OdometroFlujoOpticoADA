package com.company.warehousevio.tracking

import com.company.warehousevio.core.geometry.deltaDistanceXZ
import com.company.warehousevio.core.model.CalibrationProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Dos procesos de calibración independientes:
 *
 * A) Validación de escala: el conductor recorre una distancia conocida → calcula k.
 * B) Filtro de vibración: muestrear en reposo → determinar noiseFloor.
 */
class CalibrationEngine {

    sealed class Phase {
        object Idle : Phase()
        data class ScaleRunning(val startX: Float, val startZ: Float, val measuredM: Float) : Phase()
        data class NoiseRunning(val samples: MutableList<Float> = mutableListOf()) : Phase()
        data class Done(val profile: CalibrationProfile) : Phase()
    }

    private val _phase = MutableStateFlow<Phase>(Phase.Idle)
    val phase: StateFlow<Phase> = _phase

    // ── A) Escala ────────────────────────────────────────────────────────────

    fun startScaleCapture(startX: Float, startZ: Float, knownDistanceM: Float) {
        _phase.value = Phase.ScaleRunning(startX, startZ, knownDistanceM)
    }

    /**
     * Lllamado al finalizar el recorrido de referencia.
     * Devuelve el factor k calculado.
     */
    fun finishScaleCapture(endX: Float, endZ: Float): Float {
        val current = _phase.value as? Phase.ScaleRunning ?: return 1f
        val arcoreDist = deltaDistanceXZ(current.startX, current.startZ, endX, endZ)
        val k = if (arcoreDist > 0.01f) current.measuredM / arcoreDist else 1f
        val profile = CalibrationProfile(scaleFactor = k)
        _phase.value = Phase.Done(profile)
        return k
    }

    // ── B) Ruido / vibración ─────────────────────────────────────────────────

    fun startNoiseSampling() {
        _phase.value = Phase.NoiseRunning()
    }

    /** Añadir un Δd muestreado con la traspaleta encendida y detenida. */
    fun addNoiseSample(deltaM: Float) {
        val current = _phase.value as? Phase.NoiseRunning ?: return
        current.samples.add(deltaM)
    }

    /**
     * Finaliza el muestreo de ruido. Establece noiseFloor como media + 2σ
     * de los deltas muestreados para cubrir el jitter de las vibraciones.
     */
    fun finishNoiseSampling(): Float {
        val current = _phase.value as? Phase.NoiseRunning ?: return 0.005f
        val samples = current.samples
        if (samples.isEmpty()) return 0.005f

        val mean = samples.average().toFloat()
        val variance = samples.map { (it - mean) * (it - mean) }.average().toFloat()
        val sigma = kotlin.math.sqrt(variance)
        val noiseFloor = mean + 2 * sigma

        val prevProfile = (_phase.value as? Phase.Done)?.profile ?: CalibrationProfile()
        val updatedProfile = prevProfile.copy(noiseFloorM = noiseFloor.coerceAtLeast(0.002f))
        _phase.value = Phase.Done(updatedProfile)
        return noiseFloor
    }

    fun reset() {
        _phase.value = Phase.Idle
    }

    fun currentProfile(): CalibrationProfile =
        (_phase.value as? Phase.Done)?.profile ?: CalibrationProfile()
}
