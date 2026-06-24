package com.company.warehousevio.core.time

import android.os.SystemClock

/** Timestamp monotónico en ms. No se ve afectado por cambios del reloj del sistema. */
fun monotonicNowMs(): Long = SystemClock.elapsedRealtime()

/** Timestamp de pared (epoch) en ms. Útil para mostrar al usuario y para CSV. */
fun wallNowMs(): Long = System.currentTimeMillis()
