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

private const val TAG = "WifiDirectTransport"
private const val GROUP_OWNER_IP = "192.168.49.1"

/**
 * Transporte de producción vía Wi-Fi Direct.
 *
 * Monitor = Group Owner (IP fija 192.168.49.1), abre ServerSocket.
 * Tracker  = cliente, conecta al Group Owner.
 *
 * La negociación Wi-Fi Direct (WifiP2pManager, descubrimiento de peers,
 * formación del grupo) ocurre ANTES de llamar a start() y se gestiona
 * desde WifiDirectManager (capa de UI/ViewModel).
 */
class WifiDirectTransport(
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
            Log.d(TAG, "Servidor Wi-Fi Direct escuchando en $port")
            serverSocket = ServerSocket(port)
            socket = serverSocket!!.accept()
            Log.d(TAG, "Tracker conectado: ${socket!!.remoteSocketAddress}")
        } else {
            Log.d(TAG, "Conectando a Group Owner $GROUP_OWNER_IP:$port")
            socket = Socket(GROUP_OWNER_IP, port)
            Log.d(TAG, "Conectado al Monitor")
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
