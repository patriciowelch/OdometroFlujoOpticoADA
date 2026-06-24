package com.company.warehousevio.monitor

import com.company.warehousevio.core.model.ManualEventType
import com.company.warehousevio.core.time.wallNowMs

/**
 * Genera eventos manuales sellando timestamp y posición actual del Tracker.
 */
class EventMarker {

    data class MarkedEvent(
        val type: ManualEventType,
        val timestampMs: Long,
        val x: Float,
        val z: Float,
    )

    private val _events = mutableListOf<MarkedEvent>()
    val events: List<MarkedEvent> get() = _events.toList()

    fun mark(type: ManualEventType, currentX: Float, currentZ: Float): MarkedEvent {
        val event = MarkedEvent(
            type = type,
            timestampMs = wallNowMs(),
            x = currentX,
            z = currentZ,
        )
        _events.add(event)
        return event
    }

    fun clear() = _events.clear()
}
