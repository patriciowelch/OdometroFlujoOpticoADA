package com.company.warehousevio.ui.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.warehousevio.core.model.TrackingState
import com.company.warehousevio.network.WifiDirectState
import com.company.warehousevio.tracking.CalibrationEngine
import com.company.warehousevio.ui.viewmodel.TrackerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    onBack: () -> Unit,
    vm: TrackerViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsState()
    val wifiDirectState by vm.wifiDirectState.collectAsState()
    val calibrationPhase by vm.calibrationPhase.collectAsState()

    var useWifiDirect by remember { mutableStateOf(false) }
    var calibrationExpanded by remember { mutableStateOf(false) }
    var knownDistanceText by remember { mutableStateOf("") }

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.startSession(useWifiDirect = useWifiDirect)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rastreador (Tracker)") },
                navigationIcon = {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 8.dp),
                    ) { Text("←") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Selector de tipo de conexión ─────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Conexión", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!useWifiDirect) {
                            Button(
                                onClick = { useWifiDirect = false },
                                modifier = Modifier.weight(1f),
                            ) { Text("ADB Tunnel") }
                        } else {
                            OutlinedButton(
                                onClick = { useWifiDirect = false },
                                modifier = Modifier.weight(1f),
                            ) { Text("ADB Tunnel") }
                        }
                        if (useWifiDirect) {
                            Button(
                                onClick = { useWifiDirect = true },
                                modifier = Modifier.weight(1f),
                            ) { Text("Wi-Fi Direct") }
                        } else {
                            OutlinedButton(
                                onClick = { useWifiDirect = true },
                                modifier = Modifier.weight(1f),
                            ) { Text("Wi-Fi Direct") }
                        }
                    }

                    // Estado y peers Wi-Fi Direct cuando está seleccionado
                    if (useWifiDirect) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val stateLabel = when (val s = wifiDirectState) {
                            is WifiDirectState.Idle -> "Inactivo"
                            is WifiDirectState.Discovering -> "Buscando dispositivos…"
                            is WifiDirectState.PeersFound -> "Dispositivos encontrados: ${s.peers.size}"
                            is WifiDirectState.Connecting -> "Conectando…"
                            is WifiDirectState.Connected -> "Conectado"
                            is WifiDirectState.Error -> "Error: ${s.msg}"
                            else -> wifiDirectState.toString()
                        }
                        Text(
                            text = stateLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // Lista de peers para conectar
                        val peers = (wifiDirectState as? WifiDirectState.PeersFound)?.peers ?: emptyList()
                        peers.forEach { device ->
                            TextButton(onClick = { vm.wifiDirectManager.connectToPeer(device.deviceAddress) }) {
                                Text("Conectar a: ${device.deviceName.ifEmpty { device.deviceAddress }}")
                            }
                        }
                    }
                }
            }

            // ── Estado de conexión ───────────────────────────────────────────
            StatusCard(
                label = "Red",
                value = if (state.isConnected) "Conectado al Monitor" else "Sin conexión",
                highlight = state.isConnected,
            )

            // ── Estado de tracking ARCore ────────────────────────────────────
            StatusCard(
                label = "ARCore",
                value = when (state.trackingState) {
                    TrackingState.TRACKING -> "Tracking OK"
                    TrackingState.PAUSED -> "Pausado — ${state.trackingFailureReason.name}"
                    TrackingState.STOPPED -> "Detenido"
                },
                highlight = state.trackingState == TrackingState.TRACKING,
            )

            // ── Métricas en vivo ─────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricRow("Distancia acumulada", "${"%.2f".format(state.accumulatedDistanceM)} m")
                    MetricRow("Velocidad", "${"%.2f".format(state.instantaneousVelocityMs)} m/s")
                    MetricRow("Movimiento", state.motionState.name)
                }
            }

            // ── Panel de calibración (visible si hay sesión activa) ──────────
            if (state.isSessionActive) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Cabecera desplegable
                        TextButton(
                            onClick = { calibrationExpanded = !calibrationExpanded },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Calibración ${if (calibrationExpanded) "▲" else "▼"}",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        AnimatedVisibility(visible = calibrationExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                                // ── A) Escala ────────────────────────────────
                                Text("Escala", style = MaterialTheme.typography.labelMedium)
                                OutlinedTextField(
                                    value = knownDistanceText,
                                    onValueChange = { knownDistanceText = it },
                                    label = { Text("Distancia conocida (m)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val dist = knownDistanceText.toFloatOrNull() ?: return@OutlinedButton
                                            vm.startScaleCapture(dist)
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = calibrationPhase is CalibrationEngine.Phase.Idle ||
                                                  calibrationPhase is CalibrationEngine.Phase.Done,
                                    ) { Text("Iniciar recorrido") }
                                    Button(
                                        onClick = { vm.finishScaleCapture() },
                                        modifier = Modifier.weight(1f),
                                        enabled = calibrationPhase is CalibrationEngine.Phase.ScaleRunning,
                                    ) { Text("Finalizar") }
                                }
                                // Mostrar factor k si ya fue calculado
                                (calibrationPhase as? CalibrationEngine.Phase.Done)?.profile?.let { profile ->
                                    Text(
                                        text = "Factor k calculado: ${"%.3f".format(profile.scaleFactor)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                // ── B) Ruido / vibración ─────────────────────
                                Text("Ruido / vibración", style = MaterialTheme.typography.labelMedium)
                                OutlinedButton(
                                    onClick = { vm.startNoiseSampling() },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = calibrationPhase !is CalibrationEngine.Phase.NoiseRunning,
                                ) {
                                    Text(
                                        if (calibrationPhase is CalibrationEngine.Phase.NoiseRunning)
                                            "Muestreando… (5 s)"
                                        else
                                            "Iniciar muestreo (5 s)"
                                    )
                                }
                                (calibrationPhase as? CalibrationEngine.Phase.Done)?.profile?.let { profile ->
                                    Text(
                                        text = "NoiseFloor: ${"%.3f".format(profile.noiseFloorM)} m",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                // ── Aplicar calibración ──────────────────────
                                Button(
                                    onClick = { vm.applyCalibration() },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = calibrationPhase is CalibrationEngine.Phase.Done,
                                ) { Text("Aplicar calibración") }
                            }
                        }
                    }
                }
            }

            state.connectionError?.let {
                Text("Error: $it", color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Botón de linterna ────────────────────────────────────────────
            OutlinedButton(
                onClick = { vm.toggleTorch() },
                modifier = Modifier.fillMaxWidth(),
                colors = if (state.torchOn)
                    ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFEB3B).copy(alpha = 0.2f))
                else
                    ButtonDefaults.outlinedButtonColors(),
            ) {
                Text(if (state.torchOn) "Linterna ON — apagar" else "Linterna — encender")
            }

            if (!state.isSessionActive) {
                Button(
                    onClick = { cameraPermission.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Iniciar sesión")
                }
            } else {
                Button(
                    onClick = { vm.stopSession() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Detener sesión")
                }
            }
        }
    }
}

@Composable
private fun StatusCard(label: String, value: String, highlight: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                value,
                color = if (highlight) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
