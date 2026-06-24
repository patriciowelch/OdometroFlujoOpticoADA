package com.company.warehousevio.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.warehousevio.core.geometry.worldToCanvas
import com.company.warehousevio.core.model.ManualEventType
import com.company.warehousevio.core.model.TrackingState
import com.company.warehousevio.monitor.OriginSetup
import com.company.warehousevio.ui.viewmodel.MonitorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(
    onBack: () -> Unit,
    vm: MonitorViewModel = viewModel(),
) {
    val state by vm.liveState.collectAsState()
    val origin by vm.originSetup.collectAsState()
    val context = LocalContext.current
    var settingOrigin by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monitor") },
                navigationIcon = {
                    OutlinedButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) {
                        Text("←")
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { vm.exportCsv(context) },
                        modifier = Modifier.padding(end = 8.dp),
                    ) { Text("CSV") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // ── Canvas de trayectoria ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF1A1A2E))
                    .pointerInput(settingOrigin) {
                        if (settingOrigin) {
                            detectTapGestures { offset ->
                                vm.setOrigin(offset.x, offset.y, 0f, 50f)
                                settingOrigin = false
                            }
                        }
                    },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (state.trajectory.size < 2 || !origin.isSet) return@Canvas

                    val path = Path()
                    var first = true
                    for ((wx, wz) in state.trajectory) {
                        val (px, py) = worldToCanvas(
                            worldX = wx,
                            worldZ = wz,
                            originPxX = origin.pixelX,
                            originPxY = origin.pixelY,
                            scale = origin.scalePixelsPerMeter,
                            headingOffsetRad = origin.headingRad,
                        )
                        if (first) { path.moveTo(px, py); first = false }
                        else path.lineTo(px, py)
                    }
                    drawPath(path, Color(0xFF00BCD4), style = Stroke(width = 3f))

                    // Punto actual
                    state.latestPose?.let { pose ->
                        val (px, py) = worldToCanvas(
                            worldX = pose.x,
                            worldZ = pose.z,
                            originPxX = origin.pixelX,
                            originPxY = origin.pixelY,
                            scale = origin.scalePixelsPerMeter,
                            headingOffsetRad = origin.headingRad,
                        )
                        drawCircle(Color(0xFFFF5722), radius = 10f, center = Offset(px, py))
                    }

                    // Origen
                    if (origin.isSet) {
                        drawCircle(Color(0xFF4CAF50), radius = 8f, center = Offset(origin.pixelX, origin.pixelY))
                    }
                }

                if (!origin.isSet) {
                    Text(
                        text = if (settingOrigin) "Toca para fijar el origen" else "Sin origen configurado",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                if (state.trackingState == TrackingState.PAUSED) {
                    Text(
                        text = "⚠ Tracking perdido: ${state.trackingFailureReason.name}",
                        color = Color(0xFFFF9800),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(8.dp),
                    )
                }
            }

            // ── Panel inferior ───────────────────────────────────────────────
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // Métricas
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        MetricChip("Dist.", "${"%.2f".format(state.accumulatedDistanceM)} m")
                        MetricChip("Vel.", "${"%.2f".format(state.instantaneousVelocityMs)} m/s")
                        MetricChip("Estado", state.motionState.name)
                    }
                }

                // Últimos eventos
                if (state.recentMotionEvents.isNotEmpty()) {
                    Text(
                        text = "Último: ${state.recentMotionEvents.last().type.name}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Botones de control
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!state.isConnected) {
                        Button(onClick = { vm.startListening() }, modifier = Modifier.weight(1f)) {
                            Text("Escuchar")
                        }
                    } else {
                        OutlinedButton(onClick = { vm.stopListening() }, modifier = Modifier.weight(1f)) {
                            Text("Detener")
                        }
                    }
                    OutlinedButton(
                        onClick = { settingOrigin = true },
                        modifier = Modifier.weight(1f),
                    ) { Text("Fijar origen") }
                }

                // Eventos manuales
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ManualEventType.entries.forEach { type ->
                        OutlinedButton(
                            onClick = { vm.markManualEvent(type) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = when (type) {
                                    ManualEventType.DEAD_TIME  -> "T. Muerto"
                                    ManualEventType.LOAD_LIFT  -> "Elevación"
                                    ManualEventType.LOAD_START -> "Carga"
                                },
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
