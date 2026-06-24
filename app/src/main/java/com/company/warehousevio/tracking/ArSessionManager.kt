package com.company.warehousevio.tracking

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.EnumSet
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "ArSessionManager"

/**
 * Dispatcher de hilo único para todas las operaciones ARCore + EGL.
 * ARCore exige que create/resume/update/pause/destroy corran en el mismo hilo
 * y que el contexto GL esté activo en ese hilo.
 */
val ArCoreDispatcher: kotlinx.coroutines.CoroutineDispatcher =
    java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "ArCoreThread")
    }.asCoroutineDispatcher()

/**
 * Gestiona el ciclo de vida de ARCore en modo headless con SharedCamera.
 *
 * Con Session.Feature.SHARED_CAMERA nosotros abrimos la cámara con Camera2 y
 * construimos el CaptureRequest. Esto permite inyectar FLASH_MODE_TORCH en cada
 * frame mientras ARCore está activo, resolviendo el conflicto de acceso exclusivo
 * a la cámara que hace fallar a CameraManager.setTorchMode().
 *
 * API clave de SharedCamera en 1.47.0:
 *   - getArCoreSurfaces()              → superficies que ARCore necesita como targets
 *   - createARDeviceStateCallback()    → wrappea nuestro StateCallback con hooks de ARCore
 *   - createARSessionStateCallback()   → wrappea nuestro SessionStateCallback con hooks de ARCore
 *
 * Secuencia obligatoria:
 *   1. createSession()  → crea session, abre cámara, crea CaptureSession, resume()
 *   2. update() en loop
 *   3. destroy()        → cleanup total
 */
class ArSessionManager(private val context: Context) {

    private var session: Session? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var cameraTextureId: Int = 0

    // HandlerThread con Looper para callbacks de Camera2.
    private val cameraThread = HandlerThread("Camera2Thread").also { it.start() }
    private val cameraHandler = Handler(cameraThread.looper)

    // ── EGL offscreen ────────────────────────────────────────────────────────
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    // ── Estado ───────────────────────────────────────────────────────────────
    var lastTrackingState: TrackingState = TrackingState.STOPPED
        private set
    var lastFailureReason: TrackingFailureReason = TrackingFailureReason.NONE
        private set

    @Volatile private var torchEnabled = false

    // ─────────────────────────────────────────────────────────────────────────

    suspend fun checkAvailability(): Boolean = withContext(kotlinx.coroutines.Dispatchers.Main) {
        val availability = ArCoreApk.getInstance().checkAvailability(context)
        if (availability.isTransient) return@withContext false
        availability.isSupported
    }

    fun requestInstallIfNeeded(activity: android.app.Activity): Boolean {
        return when (ArCoreApk.getInstance().requestInstall(activity, true)) {
            ArCoreApk.InstallStatus.INSTALLED -> true
            ArCoreApk.InstallStatus.INSTALL_REQUESTED -> false
        }
    }

    /**
     * Crea la sesión ARCore con SharedCamera, abre la cámara trasera con flash,
     * construye la CaptureSession con los surfaces de ARCore y llama resume().
     *
     * SharedCamera no expone getCameraId() en 1.47.0: buscamos la cámara trasera
     * con flash entre las disponibles vía CameraManager.
     */
    suspend fun createSession(): Unit = withContext(ArCoreDispatcher) {
        check(session == null) { "Session ya existe; llamar destroy() primero" }

        initEgl()

        val s = Session(context, EnumSet.of(Session.Feature.SHARED_CAMERA))
        s.configure(
            Config(s).apply {
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                focusMode = Config.FocusMode.AUTO
                planeFindingMode = Config.PlaneFindingMode.DISABLED
            }
        )
        cameraTextureId = createCameraTexture()
        s.setCameraTextureName(cameraTextureId)
        session = s

        val sharedCamera = s.sharedCamera
        val cameraId = findRearCameraWithFlash()

        val device = openCamera(cameraId, sharedCamera)
        cameraDevice = device

        val arSurfaces = sharedCamera.getArCoreSurfaces()
        val capSession = createCaptureSession(device, arSurfaces, sharedCamera)
        captureSession = capSession

        submitRepeatingRequest(device, capSession, arSurfaces)

        s.resume()
        Log.d(TAG, "ARCore Session lista con SharedCamera, cameraId=$cameraId, textureId=$cameraTextureId")
    }

    /** Enciende o apaga el flash. Toma efecto en el próximo repeating request (~33 ms). */
    suspend fun setTorch(enabled: Boolean) = withContext(ArCoreDispatcher) {
        torchEnabled = enabled
        val dev = cameraDevice ?: return@withContext
        val cap = captureSession ?: return@withContext
        val surfaces = session?.sharedCamera?.getArCoreSurfaces() ?: return@withContext
        submitRepeatingRequest(dev, cap, surfaces)
        Log.d(TAG, "Torch ${ if (enabled) "encendido" else "apagado" }")
    }

    suspend fun update(): Frame? = withContext(ArCoreDispatcher) {
        val s = session ?: return@withContext null
        makeCurrent()
        try {
            val frame = s.update()
            lastTrackingState = frame.camera.trackingState
            lastFailureReason = frame.camera.trackingFailureReason
            frame
        } catch (e: Exception) {
            Log.e(TAG, "Error en session.update()", e)
            null
        }
    }

    suspend fun destroy() = withContext(ArCoreDispatcher) {
        captureSession?.close(); captureSession = null
        cameraDevice?.close(); cameraDevice = null
        session?.close(); session = null
        destroyEgl()
        Log.d(TAG, "ARCore Session destruida")
    }

    // ── Camera2 helpers ──────────────────────────────────────────────────────

    private fun findRearCameraWithFlash(): String {
        val cm = context.getSystemService(CameraManager::class.java)
        for (id in cm.cameraIdList) {
            val chars = cm.getCameraCharacteristics(id)
            val facing = chars.get(CameraCharacteristics.LENS_FACING)
            val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            if (facing == CameraCharacteristics.LENS_FACING_BACK && hasFlash) return id
        }
        // Fallback: primera cámara trasera, aunque no tenga flash registrado
        return cm.cameraIdList.firstOrNull { id ->
            cm.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_BACK
        } ?: "0"
    }

    private suspend fun openCamera(
        cameraId: String,
        sharedCamera: com.google.ar.core.SharedCamera,
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        val cm = context.getSystemService(CameraManager::class.java)
        val myCallback = object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) {
                if (cont.isActive) cont.resume(device)
            }
            override fun onDisconnected(device: CameraDevice) {
                device.close()
                if (cont.isActive) cont.resumeWithException(Exception("Cámara desconectada"))
            }
            override fun onError(device: CameraDevice, error: Int) {
                device.close()
                if (cont.isActive) cont.resumeWithException(Exception("Error de cámara: $error"))
            }
        }
        try {
            // Wrappear nuestro callback con el de ARCore para que reciba notificaciones internas.
            val arWrappedCallback = sharedCamera.createARDeviceStateCallback(myCallback, cameraHandler)
            cm.openCamera(cameraId, arWrappedCallback, cameraHandler)
        } catch (e: CameraAccessException) {
            if (cont.isActive) cont.resumeWithException(e)
        }
    }

    private suspend fun createCaptureSession(
        device: CameraDevice,
        arSurfaces: List<android.view.Surface>,
        sharedCamera: com.google.ar.core.SharedCamera,
    ): CameraCaptureSession = suspendCancellableCoroutine { cont ->
        val myCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                if (cont.isActive) cont.resume(session)
            }
            override fun onConfigureFailed(session: CameraCaptureSession) {
                if (cont.isActive) cont.resumeWithException(Exception("Fallo al configurar CaptureSession"))
            }
        }
        // Wrappear callback con el de ARCore.
        val arWrappedCallback = sharedCamera.createARSessionStateCallback(myCallback, cameraHandler)
        device.createCaptureSession(arSurfaces, arWrappedCallback, cameraHandler)
    }

    private fun submitRepeatingRequest(
        device: CameraDevice,
        capSession: CameraCaptureSession,
        surfaces: List<android.view.Surface>,
    ) {
        val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
        for (surface in surfaces) builder.addTarget(surface)
        builder.set(
            CaptureRequest.FLASH_MODE,
            if (torchEnabled) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF,
        )
        capSession.setRepeatingRequest(builder.build(), null, cameraHandler)
    }

    // ── EGL interno ──────────────────────────────────────────────────────────

    private fun initEgl() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        EGL14.eglInitialize(eglDisplay, version, 0, version, 1)

        val configAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)

        val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0)

        val surfaceAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, configs[0], surfaceAttribs, 0)

        makeCurrent()
        Log.d(TAG, "EGL offscreen inicializado")
    }

    private fun makeCurrent() {
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
    }

    private fun createCameraTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        return textures[0]
    }

    private fun destroyEgl() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglTerminate(eglDisplay)
        }
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE
    }
}
