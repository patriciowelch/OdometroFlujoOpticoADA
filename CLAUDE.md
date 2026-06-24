# CLAUDE.md — Sistema de Telemetría VIO para Traspaletas de Almacén

> Documento guía para el desarrollo asistido. Define arquitectura, convenciones,
> decisiones técnicas cerradas y "trampas" conocidas. Leer completo antes de
> generar o modificar código.

---

## 0. Convenciones del repositorio

- **Nombres de clases, funciones, variables, paquetes y archivos: en inglés.**
- **Comentarios y documentación (KDoc) en código: en español.**
- **Idioma de la UI visible al usuario: español.**
- Estilo Kotlin oficial (ktlint / detekt). Sin código muerto ni TODOs sin issue.
- Asincronía con **Coroutines + Flow**. Nada de callbacks anidados ni `Thread` crudos.
- No introducir dependencias nuevas sin justificarlas en este documento.

---

## 1. Qué es este proyecto (y qué NO es)

Estudio de tiempos y recorridos para una empresa de logística contratada.
**No es una app permanente ni de producción comercial.** Prioridad: que la
medición sea fiable y los datos exportables, no la pulcritud de tienda de apps.

Dos roles, **una sola codebase**, rol elegido en pantalla de arranque:

| Rol | Dispositivo | Función |
|-----|-------------|---------|
| **Tracker** (rastreador) | Samsung **Galaxy S22** (físico, montado en la traspaleta) | Corre ARCore, calcula odometría, persiste log local, hace streaming de pose. |
| **Monitor** | Segundo teléfono (producción) / **emulador** (debug) | Recibe el stream, dibuja el recorrido sobre el croquis, marca eventos manuales, guarda historial, exporta CSV. |

**Regla dura:** el emulador NUNCA es Tracker. ARCore no entrega odometría real
en emulador (solo una escena AR virtual). El emulador solo sirve como Monitor.

---

## 2. Decisiones físicas y matemáticas CERRADAS

### 2.1 Prohibiciones
- **PROHIBIDO** estimar posición por doble integración de la IMU (`d = ∫∫a dt²`).
  Genera deriva cuadrática por bias. No implementar esto ni como fallback.
- **PROHIBIDO** implementar flujo óptico crudo desde cero. Las vibraciones de las
  ruedas rígidas alteran la altura del dispositivo y arruinan la escala.

### 2.2 Solución obligatoria: VIO vía ARCore (headless)
Se usa el **motor VIO interno de ARCore** (ya fusiona cámara + giroscopio +
acelerómetro a alta frecuencia y entrega pose **en metros, con escala métrica
resuelta**). No se renderizan objetos 3D.

### 2.3 Sistema de coordenadas — convención REAL de ARCore (no la del brief original)
ARCore usa **Y hacia arriba** (alineado con la gravedad). El plano horizontal es
**X–Z**. Las vibraciones verticales viven en **Y** y se descartan.

```
Distancia incremental entre muestras consecutivas:
Δd = sqrt( (x₂ − x₁)² + (z₂ − z₁)² )     // se ignora Y
```

> El brief original usaba la convención matemática (X,Y horizontal, Z vertical).
> Está invertida respecto a ARCore. **Usar SIEMPRE X–Z como plano de medición.**

### 2.4 La escala ya viene resuelta
ARCore entrega traslación en **metros reales**. NO hay que calibrar un "factor de
flujo óptico". La calibración (sección 6) es **validación de escala + filtrado de
ruido**, nada más.

---

## 3. Stack tecnológico

- **Lenguaje:** Kotlin
- **IDE:** Android Studio
- **minSdk:** 26 · **targetSdk:** la más reciente estable · S22 es compatible con ARCore.
- **AR / VIO:** `com.google.ar:core`
- **UI:** Jetpack Compose. Dibujo de trayectoria con `Canvas` de Compose.
- **Async / streaming:** Coroutines + Flow / StateFlow.
- **Protocolo de red:** **NDJSON** (JSON delimitado por `\n`) sobre **sockets TCP**, serializado con **kotlinx.serialization**.
- **Red P2P:** `WifiP2pManager` (Wi-Fi Direct).
- **Persistencia:** **Room** (sesiones + log de pose + eventos).
- **Export/compartir:** `FileProvider` + `Intent.ACTION_SEND` (botón "Share" a cualquier medio).
- **Arquitectura:** MVVM (ViewModel + StateFlow + UDF). Repositorios entre datos y UI.

---

## 4. Estructura de proyecto

Un solo módulo `:app` con separación clara por paquetes (modularización en Gradle
queda como opcional futuro, no necesaria para el estudio).

```
com.company.warehousevio
├── core/
│   ├── model/            // Pose, MotionEvent, SessionData, CalibrationProfile…
│   ├── geometry/         // cálculo de distancia X-Z, heading, proyecciones
│   └── time/             // timestamps monotónicos
├── tracking/             // ROL TRACKER
│   ├── ArSessionManager  // ciclo de vida ARCore headless (ver §7)
│   ├── PoseTracker        // loop de muestreo, Δd, velocidad, acumulación
│   ├── MotionClassifier   // máquina de estados avance/retroceso/freno (§5.3)
│   ├── TrackingHealth     // detección de pérdida de tracking (§5.4)
│   └── CalibrationEngine  // validación de escala + filtro de vibración (§6)
├── monitor/              // ROL MONITOR
│   ├── MapCanvas          // dibujo 2D del recorrido sobre croquis
│   ├── OriginSetup        // colocar origen + dirección sobre el mapa
│   ├── EventMarker        // eventos manuales (tiempo muerto, elevación, carga)
│   └── LiveSessionState   // estado en vivo recibido del Tracker
├── network/              // capa de transporte COMPARTIDA (§8)
│   ├── Transport          // interfaz común (server/client TCP)
│   ├── WifiDirectTransport// producción
│   ├── TcpTunnelTransport // debug (adb reverse/forward)
│   ├── ProtocolMessage    // sealed class de mensajes NDJSON
│   └── MessageCodec        // serialización/deserialización
├── data/                 // Room: DAOs, entities, repositorios
│   └── export/           // CsvExporter + share
└── ui/                   // Compose: RoleSelection, TrackerScreen, MonitorScreen…
```

---

## 5. Lógica del Tracker (S22)

### 5.1 Loop de muestreo
- Muestrear pose de ARCore cada **n ms** (configurable; arrancar en ~33 ms ≈ 30 Hz).
- Por muestra: extraer `(x, y, z)` de `frame.camera.pose.translation`.
- Calcular `Δd` en plano X–Z (§2.3). **Aplicar deadband de ruido** (§6.2) antes de acumular: si `Δd < noiseFloor`, NO sumar (evita deriva por vibración en reposo).
- `instantaneousVelocity = Δd / Δt`. Mantener también velocidad promedio y tiempo de ciclo.

### 5.2 Modelo de datos del log (Room)
Cada entrada: `timestamp`, `x`, `z`, `accumulatedDistance`, `instantaneousVelocity`,
`trackingState`, `motionState`. (Se guarda X y Z crudos por si hay que re-procesar offline.)

### 5.3 Eventos de movimiento AUTOMÁTICOS (`MotionClassifier`)
Máquina de estados con **histéresis** (evita spam por jitter):

- Estados: `IDLE`, `FORWARD`, `REVERSE`.
- El dispositivo va montado fijo: su eje "adelante" (forward axis local) corresponde
  al avance de la traspaleta. **Producto punto** entre el vector de traslación y el
  forward axis decide avance (>0) o retroceso (<0).
- Umbral de velocidad con banda muerta: `START` cuando supera `vStart`,
  `BRAKE/STOP` cuando cae bajo `vStop` durante una ventana sostenida (`vStart > vStop`).
- Transiciones generan eventos: `START_FORWARD`, `START_REVERSE`, `BRAKE`.
- Estos eventos se transmiten al Monitor y se guardan en el log con timestamp + posición.

### 5.4 Manejo de pérdida de tracking (`TrackingHealth`)
- Si `camera.trackingState != TRACKING` → **pausar acumulación**, marcar un *gap* y
  alertar a ambas pantallas. Reportar `TrackingFailureReason`
  (`INSUFFICIENT_FEATURES`, `EXCESSIVE_MOTION`, `INSUFFICIENT_LIGHT`, etc.).
- Tras recuperar tracking: ARCore puede **saltar** la pose al relocalizar.
  **Descartar el primer Δd post-recuperación** (tratarlo como continuación del gap)
  para no inyectar un salto falso a la distancia. Nunca registrar datos corruptos.

---

## 6. Calibración (semiautomática)

Dos procesos independientes, guardados en un `CalibrationProfile` persistente.

### 6.1 Validación de escala (distancia conocida)
1. Se pide al conductor recorrer una distancia medida con cinta (p. ej. 10 m).
2. Presiona "iniciar" al arrancar y "fin" al completar.
3. `k = knownDistance / arcoreMeasuredDistance`.
4. Se aplica `k` como factor de corrección a las mediciones posteriores.
   (En teoría ARCore ya da metros; `k` corrige sesgos residuales del montaje/superficie.)

### 6.2 Filtro de vibración / ruido
1. Con la traspaleta encendida pero **detenida**, muestrear el jitter de pose unos segundos.
2. Establecer `noiseFloor` (deadband de `Δd`) y, si hace falta, un filtro paso-bajo sobre `(x,z)`.
3. Por debajo de `noiseFloor`, el desplazamiento se considera ruido y no se acumula.

---

## 7. ⚠️ Gotcha crítico: ARCore "headless" no es realmente sin GL

Aunque no se rendericen objetos 3D, **ARCore exige un contexto GL y una textura de
cámara** para funcionar:

- Crear un contexto GL offscreen mínimo.
- Llamar `session.setCameraTextureName(textureId)` una vez.
- Ejecutar `session.update()` por frame en el hilo de render para obtener cada `Frame`.
- Sin esto, `session.update()` no avanza y no hay pose.

Documentar bien esta clase (`ArSessionManager`). Es el punto más frágil del proyecto.
Manejar también: permiso de cámara, `ArCoreApk` (instalación/actualización de ARCore),
y `Config` de la `Session` (focus mode, update mode).

---

## 8. Capa de red (compartida) — §clave de la arquitectura

El **código de sockets es idéntico** en debug y producción. Solo cambia *cómo se
encuentran* los dispositivos. Abstraer detrás de la interfaz `Transport`.

- **Monitor = servidor.** **Tracker = cliente** que conecta y hace streaming saliente.
- **Producción → `WifiDirectTransport`:** sin WiFi de almacén. El **Monitor es Group
  Owner** (IP fija `192.168.49.1`) y abre el `ServerSocket`. El Tracker descubre el
  grupo, conecta a `192.168.49.1:PORT`.
- **Debug → `TcpTunnelTransport`:** el Monitor (emulador) corre el servidor; se tuneliza:
  ```
  # servidor del emulador visible en el host:
  adb -s emulator-5554 forward tcp:PORT tcp:PORT
  # el S22 alcanza el host:PORT vía su propio localhost:
  adb -s <serial_s22> reverse tcp:PORT tcp:PORT
  # → el Tracker SIEMPRE conecta a 127.0.0.1:PORT
  ```
  Cadena: `S22 localhost:PORT --reverse--> PC localhost:PORT --forward--> emulador:PORT`.
- **Depuración del S22 por WiFi** (`adb pair` / `adb connect`) para moverlo libremente.
  Ojo: si el ADB es por WiFi y el transporte de la app es Wi-Fi Direct, son redes/
  interfaces distintas; en debug se recomienda emulador-como-Monitor + tunneling ADB.

### 8.1 Protocolo (NDJSON, `sealed class ProtocolMessage`)
- **Tracker → Monitor:** `PoseUpdate(ts, x, z, heading, vInst, distAccum, trackingState, motionState)`,
  `MotionEventMsg(type, ts, x, z)`, `TrackingAlert(reason)`, `CalibrationStatus`.
- **Monitor → Tracker:** `SessionControl(start|pause|stop)`, `SetOrigin(x, z, heading)`.
- Pose updates ~10–30 Hz. Mantener mensajes pequeños.

### 8.2 Resiliencia (enlace inestable, sin WiFi fijo)
- El **Tracker es la fuente de verdad de la pose**: persiste su log local en Room
  aunque caiga el enlace. Al reconectar, puede reenviar el tramo faltante.
- El Monitor guarda el espejo en vivo + los eventos manuales.
- Reconexión automática con backoff. La caída de red **no** detiene la medición del Tracker.

---

## 9. Lógica del Monitor

- **Setup de origen:** el observador coloca el punto de inicio y la **dirección**
  del Tracker sobre un mapa en pantalla. Eso define el frame de referencia para dibujar.
- **Fondo del mapa:** inicialmente fondo liso; debe poder **cargar la imagen del
  croquis del almacén** como fondo para dar referencia visual. Escalable/encuadrable.
- **Trazo:** recorrido 2D cenital (vista de pájaro) sobre el croquis, dibujado en `Canvas`.
- **Eventos manuales (`EventMarker`):** botones para `DEAD_TIME` (tiempo muerto),
  `LOAD_LIFT` (elevación de carga), `LOAD_START` (inicio de carga). Cada uno sella
  **timestamp + posición actual** (la posición sale del último `PoseUpdate`).
- **Feed de eventos automáticos:** mostrar en vivo los `START_FORWARD/REVERSE/BRAKE`
  que llegan del Tracker.

---

## 10. Persistencia y exportación

- **Room** guarda **sesiones históricas** completas (revisables después): metadatos
  de sesión, log de pose, eventos automáticos y manuales, perfil de calibración usado.
- **CSV:** `CsvExporter` convierte una sesión a `.csv`. Columnas mínimas:
  `timestamp, accumulated_distance_m, instantaneous_velocity_ms, x, z, motion_state, event_type`.
- **Compartir:** botón **"Share"** vía `FileProvider` + `ACTION_SEND` hacia cualquier
  app/medio (mail, drive, etc.).
- En el merge para export, unir log de pose (Tracker) + eventos manuales (Monitor)
  ordenando por `timestamp`.

---

## 11. Plan de entregables para Claude

Al implementar, seguir este orden y mantener cada pieza pequeña y testeable:

1. `core/model` + `core/geometry` (cálculo X–Z, heading) con tests unitarios.
2. `network` (Transport, codec, protocolo) con un test de loopback TCP.
3. `tracking/ArSessionManager` headless (la pieza frágil — §7) + `PoseTracker`.
4. `MotionClassifier` + `TrackingHealth` con tests de la máquina de estados.
5. `CalibrationEngine` (escala + ruido).
6. `monitor` (MapCanvas, OriginSetup, EventMarker) + integración del stream.
7. `data` (Room) + `export` (CSV + share).
8. UI Compose y pantalla de selección de rol.

> Antes de escribir una clase nueva, confirmar que respeta: convención X–Z (§2.3),
> prohibiciones (§2.1), roles de dispositivo (§1), y la abstracción `Transport` (§8).
