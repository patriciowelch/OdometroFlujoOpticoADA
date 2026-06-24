package com.company.warehousevio.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.warehousevio.data.entity.SessionEntity
import com.company.warehousevio.ui.viewmodel.MonitorViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    onBack: () -> Unit,
    vm: MonitorViewModel = viewModel(),
) {
    val sessions by vm.allSessions.collectAsState(initial = emptyList())
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de sesiones") },
                navigationIcon = {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 8.dp),
                    ) { Text("←") }
                },
            )
        }
    ) { paddingValues ->
        if (sessions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "No hay sesiones guardadas",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionRow(
                        session = session,
                        onExport = { vm.exportCsv(context, session.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: SessionEntity,
    onExport: () -> Unit,
) {
    val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val startLabel = dateFmt.format(Date(session.startTimestampMs))

    val durationLabel = if (session.endTimestampMs == null) {
        "En curso"
    } else {
        val diffMs = session.endTimestampMs - session.startTimestampMs
        val h = TimeUnit.MILLISECONDS.toHours(diffMs)
        val m = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60
        val s = TimeUnit.MILLISECONDS.toSeconds(diffMs) % 60
        "%02d:%02d:%02d".format(h, m, s)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sesión #${session.id}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = startLabel,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Duración: $durationLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onExport) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Exportar CSV",
                )
            }
        }
    }
}
