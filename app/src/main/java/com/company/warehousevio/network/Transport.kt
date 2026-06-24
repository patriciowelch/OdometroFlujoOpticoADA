package com.company.warehousevio.network

import kotlinx.coroutines.flow.Flow

/**
 * Abstracción de transporte bidireccional.
 * Monitor actúa como servidor, Tracker como cliente.
 */
interface Transport {
    /** Flow de mensajes entrantes. Se completa al cerrar la conexión. */
    val incoming: Flow<ProtocolMessage>

    /** Envía un mensaje al par conectado. */
    suspend fun send(message: ProtocolMessage)

    /** Inicia el transporte (bind del servidor o connect del cliente). */
    suspend fun start()

    /** Cierra la conexión limpiamente. */
    suspend fun close()
}
