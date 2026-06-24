package com.company.warehousevio.ui.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.warehousevio.core.model.TrackingState
import com.company.warehousevio.ui.viewmodel.TrackerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    onBack: () -> Unit,
    vm: TrackerViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsState()

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.startSession()
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Estado de conexión
            StatusCard(
                label = "Red",
                value = if (state.isConnected) "Conectado al Monitor" else "Sin conexión",
                highlight = state.isConnected,
            )

            // Estado de tracking ARCore
            StatusCard(
                label = "ARCore",
                value = when (state.trackingState) {
                    TrackingState.TRACKING -> "Tracking OK"
                    TrackingState.PAUSED -> "Pausado — ${state.trackingFailureReason.name}"
                    TrackingState.STOPPED -> "Detenido"
                },
                highlight = state.trackingState == TrackingState.TRACKING,
            )

            // Métricas en vivo
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricRow("Distancia acumulada", "${"%.2f".format(state.accumulatedDistanceM)} m")
                    MetricRow("Velocidad", "${"%.2f".format(state.instantaneousVelocityMs)} m/s")
                    MetricRow("Movimiento", state.motionState.name)
                }
            }

            state.connectionError?.let {
                Text("Error: $it", color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.weight(1f))

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
