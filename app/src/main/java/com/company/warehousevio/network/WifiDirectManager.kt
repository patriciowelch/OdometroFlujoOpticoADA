@file:Suppress("DEPRECATION")  // NetworkInfo es la única API disponible para WifiP2pManager en minSdk 26

package com.company.warehousevio.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "WifiDirectManager"

/**
 * Estados posibles del canal Wi-Fi Direct.
 */
sealed class WifiDirectState {
    /** Sin actividad. */
    object Idle : WifiDirectState()
    /** Creando el grupo P2P (Monitor como Group Owner). */
    object CreatingGroup : WifiDirectState()
    /** Grupo creado; este dispositivo es Group Owner (Monitor). */
    object GroupOwner : WifiDirectState()
    /** Descubriendo peers (Tracker). */
    object Discovering : WifiDirectState()
    /** Peers encontrados. */
    data class PeersFound(val peers: List<WifiP2pDevice>) : WifiDirectState()
    /** Conectando a un peer. */
    object Connecting : WifiDirectState()
    /** Conectado a un peer. */
    object Connected : WifiDirectState()
    /** Error con mensaje descriptivo. */
    data class Error(val msg: String) : WifiDirectState()
}

/**
 * Singleton de alcance de aplicación que envuelve WifiP2pManager.
 *
 * Expone [state] y [peers] como StateFlow para que los ViewModels
 * puedan observarlos con coroutines.
 *
 * Llamar [register] en onResume y [unregister] en onPause de MainActivity.
 */
class WifiDirectManager(private val context: Context) {

    private val wifiP2pManager: WifiP2pManager =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager

    private val channel: WifiP2pManager.Channel =
        wifiP2pManager.initialize(context, context.mainLooper, null)

    private val _state = MutableStateFlow<WifiDirectState>(WifiDirectState.Idle)
    val state: StateFlow<WifiDirectState> = _state

    private val _peers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val peers: StateFlow<List<WifiP2pDevice>> = _peers

    // ── BroadcastReceiver ────────────────────────────────────────────────────

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    Log.d(TAG, "Peers cambiados — solicitando lista")
                    requestPeers()
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    // Usar API compatible con el minSdk 26 del proyecto
                    val networkInfo: NetworkInfo? = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                    val connected = networkInfo?.isConnected == true
                    if (connected) {
                        Log.d(TAG, "Conectado a peer Wi-Fi Direct")
                        _state.value = WifiDirectState.Connected
                    } else {
                        Log.d(TAG, "Desconectado de peer Wi-Fi Direct")
                        if (_state.value is WifiDirectState.Connected) {
                            _state.value = WifiDirectState.Idle
                        }
                    }
                }
            }
        }
    }

    /** Registrar receptor; llamar desde MainActivity.onResume(). */
    fun register() {
        context.registerReceiver(receiver, intentFilter)
    }

    /** Desregistrar receptor; llamar desde MainActivity.onPause(). */
    fun unregister() {
        runCatching { context.unregisterReceiver(receiver) }
    }

    // ── Operaciones P2P ──────────────────────────────────────────────────────

    /**
     * Crea un grupo P2P (Monitor = Group Owner).
     * Primero elimina cualquier grupo previo para evitar conflictos.
     */
    fun createGroup() {
        _state.value = WifiDirectState.CreatingGroup
        // Eliminar grupo previo antes de crear uno nuevo
        wifiP2pManager.removeGroup(channel, object : ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Grupo anterior eliminado — creando nuevo grupo")
                doCreateGroup()
            }
            override fun onFailure(reason: Int) {
                // No había grupo; crear directamente
                Log.d(TAG, "Sin grupo previo (código $reason) — creando grupo")
                doCreateGroup()
            }
        })
    }

    private fun doCreateGroup() {
        wifiP2pManager.createGroup(channel, object : ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Grupo creado correctamente")
                _state.value = WifiDirectState.GroupOwner
            }
            override fun onFailure(reason: Int) {
                val msg = "Error al crear grupo: código $reason"
                Log.e(TAG, msg)
                _state.value = WifiDirectState.Error(msg)
            }
        })
    }

    /** Inicia el descubrimiento de peers (Tracker). */
    fun discoverPeers() {
        _state.value = WifiDirectState.Discovering
        wifiP2pManager.discoverPeers(channel, object : ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Descubrimiento iniciado")
            }
            override fun onFailure(reason: Int) {
                val msg = "Error al descubrir peers: código $reason"
                Log.e(TAG, msg)
                _state.value = WifiDirectState.Error(msg)
            }
        })
    }

    /** Conecta al peer con la dirección MAC indicada. */
    fun connectToPeer(deviceAddress: String) {
        _state.value = WifiDirectState.Connecting
        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
        }
        wifiP2pManager.connect(channel, config, object : ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Solicitud de conexión enviada a $deviceAddress")
                // El estado Connected llegará vía BroadcastReceiver
            }
            override fun onFailure(reason: Int) {
                val msg = "Error al conectar a $deviceAddress: código $reason"
                Log.e(TAG, msg)
                _state.value = WifiDirectState.Error(msg)
            }
        })
    }

    /** Elimina el grupo P2P activo. */
    fun removeGroup() {
        wifiP2pManager.removeGroup(channel, object : ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Grupo eliminado")
                _state.value = WifiDirectState.Idle
            }
            override fun onFailure(reason: Int) {
                Log.w(TAG, "No se pudo eliminar el grupo: código $reason")
            }
        })
    }

    /** Solicita la lista de peers actualizada al sistema. */
    private fun requestPeers() {
        wifiP2pManager.requestPeers(channel) { peerList ->
            val devices = peerList.deviceList.toList()
            _peers.value = devices
            Log.d(TAG, "Peers encontrados: ${devices.size}")
            if (devices.isNotEmpty()) {
                _state.value = WifiDirectState.PeersFound(devices)
            }
        }
    }
}
