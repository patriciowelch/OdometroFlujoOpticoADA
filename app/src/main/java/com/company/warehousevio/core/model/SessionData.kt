package com.company.warehousevio.core.model

/**
 * Metadatos de una sesión de telemetría.
 */
data class SessionData(
    val id: Long = 0,
    val startTimestampMs: Long,
    val endTimestampMs: Long? = null,
    val operatorName: String = "",
    val notes: String = "",
    val calibrationProfileId: Long? = null,
)
