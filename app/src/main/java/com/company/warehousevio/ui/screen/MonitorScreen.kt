package com.company.warehousevio.ui.screen

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.warehousevio.core.geometry.worldToCanvas
import com.company.warehousevio.core.model.ManualEventType
import com.company.warehousevio.core.model.TrackingState
import com.company.warehousevio.monitor.OriginSetup
import com.company.warehousevio.network.WifiDirectState
import com.company.warehousevio.ui.viewmodel.MonitorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(
    onBack: () -> Unit,
    onHistory: () -> Unit = {},
    vm: MonitorViewModel = viewModel(),
) {
    val state by vm.liveState.collectAsState()
    val origin by vm.originSetup.collectAsState()
    val mapImageUri by vm.mapImageUri.collectAsState()
    val wifiDirectState by vm.wifiDirectState.collectAsState()
    val context = LocalContext.current

    var settingOrigin by remember { mutableStateOf(false) }
    var useWifiDirect by remember { mutableStateOf(false) }

    // Bitmap del croquis cargado en memoria
    var mapBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(mapImageUri) {
        mapBitmap = mapImageUri?.let { uri ->
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    // Selector de imagen del croquis
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { vm.setMapImage(it) }
    }

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
                        onClick = onHistory,
                        modifier = Modifier.padding(end = 4.dp),
                    ) { Text("Historial") }
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
                    // Fondo: croquis del almacén si existe
                    mapBitmap?.let { bmp ->
                        drawImage(
                            image = bmp,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(bmp.width, bmp.height),
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                        )
                    }

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

                // ── Selector de tipo de conexión ─────────────────────────────
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Conexión", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(6.dp))
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
                        // Estado Wi-Fi Direct cuando está seleccionado
                        if (useWifiDirect) {
                            val stateLabel = when (val s = wifiDirectState) {
                                is WifiDirectState.Idle -> "Inactivo"
                                is WifiDirectState.CreatingGroup -> "Creando grupo…"
                                is WifiDirectState.GroupOwner -> "Grupo creado — esperando Tracker"
                                is WifiDirectState.Connected -> "Tracker conectado"
                                is WifiDirectState.Error -> "Error: ${s.msg}"
                                else -> wifiDirectState.toString()
                            }
                            Text(
                                text = stateLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

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
                        Button(
                            onClick = { vm.startListening(useWifiDirect = useWifiDirect) },
                            modifier = Modifier.weight(1f),
                        ) {
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
                    OutlinedButton(
                        onClick = { vm.resetTrajectory(); settingOrigin = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) { Text("↺ Reset") }
                    OutlinedButton(
                        onClick = { imagePicker.launch(arrayOf("image/*")) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Mapa") }
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
