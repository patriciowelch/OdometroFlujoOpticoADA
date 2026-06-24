package com.company.warehousevio.tracking

import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ArSessionManager"

/**
 * Dispatcher de hilo único para todas las operaciones ARCore + EGL.
 * ARCore exige que create/resume/update/pause/destroy corran en el mismo hilo
 * y que el contexto GL esté activo en ese hilo. No usar Dispatchers.Default.
 */
val ArCoreDispatcher: kotlinx.coroutines.CoroutineDispatcher =
    java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "ArCoreThread")
    }.asCoroutineDispatcher()

/**
 * Gestiona el ciclo de vida de ARCore en modo "headless" (sin render visible).
 *
 * ARCore exige un contexto GL y una textura de cámara aunque no se rendericen
 * objetos 3D. Esta clase crea un contexto EGL offscreen mínimo, registra la
 * textura y expone [update] para obtener cada Frame desde el loop de muestreo.
 *
 * Secuencia obligatoria:
 *   1. checkAvailability() → verificar e instalar ARCore si hace falta
 *   2. createSession()     → permiso de cámara ya concedido
 *   3. resume()            → onResume de la Activity
 *   4. update() en loop    → muestrear pose
 *   5. pause()             → onPause de la Activity
 *   6. destroy()           → cleanup final
 */
class ArSessionManager(private val context: Context) {

    private var session: Session? = null
    private var cameraTextureId: Int = 0

    // ── EGL offscreen ────────────────────────────────────────────────────────
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    // ── Estado ───────────────────────────────────────────────────────────────
    var lastTrackingState: TrackingState = TrackingState.STOPPED
        private set
    var lastFailureReason: TrackingFailureReason = TrackingFailureReason.NONE
        private set

    /**
     * Verifica disponibilidad de ARCore. Devuelve true si está listo para usar.
     * Si ARCore necesita instalarse/actualizarse lanza la solicitud al sistema.
     */
    suspend fun checkAvailability(): Boolean = withContext(Dispatchers.Main) {
        val availability = ArCoreApk.getInstance().checkAvailability(context)
        if (availability.isTransient) {
            // Reintentar — ARCore está descargando
            return@withContext false
        }
        availability.isSupported
    }

    /**
     * Solicita al usuario instalar/actualizar ARCore si es necesario.
     * Llamar desde una Activity activa.
     */
    fun requestInstallIfNeeded(activity: android.app.Activity): Boolean {
        return when (ArCoreApk.getInstance().requestInstall(activity, true)) {
            ArCoreApk.InstallStatus.INSTALLED -> true
            ArCoreApk.InstallStatus.INSTALL_REQUESTED -> false
        }
    }

    /**
     * Crea la Session de ARCore y el contexto GL offscreen.
     * Se ejecuta en ArCoreDispatcher para que EGL y ARCore queden en el mismo hilo.
     */
    suspend fun createSession() = withContext(ArCoreDispatcher) {
        check(session == null) { "Session ya existe; llamar destroy() primero" }
        initEgl()
        session = Session(context).also { s ->
            s.configure(
                Config(s).apply {
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    focusMode = Config.FocusMode.AUTO
                    planeFindingMode = Config.PlaneFindingMode.DISABLED
                }
            )
            cameraTextureId = createCameraTexture()
            s.setCameraTextureName(cameraTextureId)
            Log.d(TAG, "ARCore Session creada, textureId=$cameraTextureId")
        }
    }

    suspend fun resume() = withContext(ArCoreDispatcher) {
        session?.resume() ?: Log.w(TAG, "resume() llamado sin session")
    }

    suspend fun pause() = withContext(ArCoreDispatcher) {
        session?.pause()
    }

    /**
     * Llama a session.update() en ArCoreDispatcher — mismo hilo que create/resume.
     * makeCurrent() activa el contexto EGL en este hilo antes de cada update.
     */
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
        session?.close()
        session = null
        destroyEgl()
        Log.d(TAG, "ARCore Session destruida")
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
