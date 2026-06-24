package com.company.warehousevio.core.model

/** Tipos de evento manual que el operador del Monitor puede registrar. */
enum class ManualEventType {
    DEAD_TIME,    // tiempo muerto (espera)
    LOAD_LIFT,    // elevación de carga
    LOAD_START,   // inicio de carga/descarga
}
