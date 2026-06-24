package com.company.warehousevio.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

private const val TAG = "TcpTunnelTransport"

/**
 * Transporte TCP para depuración vía ADB tunnel.
 *
 * Modo servidor (Monitor/emulador): abre ServerSocket en [port] y espera al Tracker.
 * Modo cliente (Tracker/S22):       conecta a 127.0.0.1:[port] — el tunnel de ADB
 *                                   redirige al servidor corriendo en el emulador.
 *
 * Flujo de ADB (una vez configurado):
 *   adb -s emulator-5554 forward tcp:PORT tcp:PORT
 *   adb -s <serial_s22> reverse tcp:PORT tcp:PORT
 */
class TcpTunnelTransport(
    private val port: Int,
    private val isServer: Boolean,
) : Transport {

    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null
    private var writer: PrintWriter? = null

    override val incoming: Flow<ProtocolMessage> = flow {
        val reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val msg = runCatching { MessageCodec.decode(line!!) }.getOrNull()
                if (msg != null) emit(msg)
                else Log.w(TAG, "Línea no decodificable: $line")
            }
        } finally {
            Log.d(TAG, "Stream de entrada cerrado")
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun send(message: ProtocolMessage): Unit = withContext(Dispatchers.IO) {
        writer?.print(MessageCodec.encode(message))
        writer?.flush()
    }

    override suspend fun start(): Unit = withContext(Dispatchers.IO) {
        if (isServer) {
            Log.d(TAG, "Servidor escuchando en puerto $port")
            serverSocket = ServerSocket(port)
            socket = serverSocket!!.accept()
            Log.d(TAG, "Cliente conectado: ${socket!!.remoteSocketAddress}")
        } else {
            Log.d(TAG, "Conectando a 127.0.0.1:$port")
            socket = Socket("127.0.0.1", port)
            Log.d(TAG, "Conectado al servidor")
        }
        writer = PrintWriter(socket!!.getOutputStream(), false)
    }

    override suspend fun close(): Unit = withContext(Dispatchers.IO) {
        runCatching { socket?.close() }
        runCatching { serverSocket?.close() }
        socket = null
        serverSocket = null
        writer = null
    }
}
