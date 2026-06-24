package com.company.warehousevio.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.company.warehousevio.data.dao.EventLogDao
import com.company.warehousevio.data.dao.PoseLogDao
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exporta una sesión a CSV y devuelve la Uri para compartir con FileProvider.
 *
 * Columnas: timestamp_ms, accumulated_distance_m, instantaneous_velocity_ms,
 *           x, z, motion_state, event_type, event_source
 *
 * Las filas son la unión del log de poses y eventos, ordenadas por timestamp.
 */
class CsvExporter(
    private val context: Context,
    private val poseLogDao: PoseLogDao,
    private val eventLogDao: EventLogDao,
) {
    suspend fun export(sessionId: Long): Uri {
        val poses = poseLogDao.getForSessionOnce(sessionId)
        val events = eventLogDao.getForSessionOnce(sessionId)

        val eventsMap = events.groupBy { it.timestampMs }

        val sb = StringBuilder()
        // Cabecera de metadatos: fecha de descarga e ID de sesión
        val downloadTs = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        sb.appendLine("# descarga: $downloadTs")
        sb.appendLine("# sesion_id: $sessionId")
        sb.appendLine("timestamp_ms,accumulated_distance_m,instantaneous_velocity_ms,x,z,motion_state,event_type,event_source")

        val allTimestamps = (poses.map { it.timestampMs } + events.map { it.timestampMs })
            .distinct()
            .sorted()

        val poseByTime = poses.associateBy { it.timestampMs }

        for (ts in allTimestamps) {
            val pose = poseByTime[ts]
            val evList = eventsMap[ts] ?: emptyList()

            if (pose != null && evList.isEmpty()) {
                sb.appendLine("$ts,${pose.accumulatedDistanceM},${pose.instantaneousVelocityMs},${pose.x},${pose.z},${pose.motionState},,")
            } else if (pose != null) {
                for (ev in evList) {
                    sb.appendLine("$ts,${pose.accumulatedDistanceM},${pose.instantaneousVelocityMs},${pose.x},${pose.z},${pose.motionState},${ev.eventType},${ev.eventSource}")
                }
            } else {
                for (ev in evList) {
                    sb.appendLine("$ts,,,${ev.x},${ev.z},,${ev.eventType},${ev.eventSource}")
                }
            }
        }

        val fileName = "session_${sessionId}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.csv"
        val exportDir = File(context.filesDir, "exports").also { it.mkdirs() }
        val file = File(exportDir, fileName)
        file.writeText(sb.toString())

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun buildShareIntent(uri: Uri): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
