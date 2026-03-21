package com.obsremotecamera

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import com.pedro.library.view.OpenGlView
import androidx.lifecycle.viewModelScope
import com.obsremotecamera.config.AppConfig
import com.obsremotecamera.config.ConfigRepository
import com.obsremotecamera.network.ObsApiClient
import com.obsremotecamera.streaming.SrtStreamer
import com.obsremotecamera.streaming.StreamState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class StepStatus { PENDING, CHECKING, OK, WARNING, FAILED }

data class PrerequisiteStep(
    val label: String,
    val status: StepStatus,
    val errorMessage: String = ""
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val configRepo = ConfigRepository(application)
    val srtStreamer = SrtStreamer(application)
    private val obsApiClient = ObsApiClient()

    val config: StateFlow<AppConfig> = configRepo.configFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppConfig())

    val streamState: StateFlow<StreamState> = srtStreamer.state
    val currentBitrateKbps: StateFlow<Long> = srtStreamer.currentBitrateKbps
    val apiConnectionStatus = obsApiClient.connectionStatus

    private fun initialSteps() = listOf(
        PrerequisiteStep("Controlador OBS", StepStatus.PENDING),
        PrerequisiteStep("OBS Studio", StepStatus.PENDING)   // informativo, no bloqueante
    )
    private val _prerequisiteSteps = MutableStateFlow(initialSteps())
    val prerequisiteSteps: StateFlow<List<PrerequisiteStep>> = _prerequisiteSteps.asStateFlow()

    private val _prerequisitesDone = MutableStateFlow(false)
    val prerequisitesDone: StateFlow<Boolean> = _prerequisitesDone.asStateFlow()

    // Cámaras habilitadas — espejo reactivo del WebSocket STATE_UPDATE
    private val _enabledCameras = MutableStateFlow<List<Int>>(emptyList())
    val enabledCameras: StateFlow<List<Int>> = _enabledCameras.asStateFlow()

    private val _streamDurationSeconds = MutableStateFlow(0L)
    val streamDurationSeconds: StateFlow<Long> = _streamDurationSeconds.asStateFlow()

    private var durationJob: Job? = null

    init {
        // Mantener _enabledCameras sincronizado con el WebSocket en todo momento
        viewModelScope.launch {
            obsApiClient.enabledCameras.collect { cameras ->
                if (cameras.isNotEmpty()) {
                    _enabledCameras.value = cameras
                }
            }
        }

        // Reiniciar stream si cambia el número de cámara mientras se transmite
        viewModelScope.launch {
            var previousConfig: AppConfig? = null
            config.collect { newConfig ->
                val prev = previousConfig
                previousConfig = newConfig
                val isActive = streamState.value.let {
                    it is StreamState.Streaming || it is StreamState.Connecting || it is StreamState.Reconnecting
                }
                if (prev != null && isActive && prev.cameraNumber != newConfig.cameraNumber) {
                    srtStreamer.stopStream()
                    delay(350)
                    srtStreamer.startStream(newConfig)
                }
            }
        }

        // Contador de duración del stream
        viewModelScope.launch {
            streamState.collect { state ->
                when (state) {
                    is StreamState.Streaming -> {
                        if (durationJob == null) {
                            _streamDurationSeconds.value = 0
                            durationJob = viewModelScope.launch {
                                while (true) {
                                    delay(1000)
                                    _streamDurationSeconds.value++
                                }
                            }
                        }
                    }
                    is StreamState.Idle, is StreamState.Error -> {
                        durationJob?.cancel()
                        durationJob = null
                        _streamDurationSeconds.value = 0
                    }
                    else -> {}
                }
            }
        }
    }

    fun saveConfig(newConfig: AppConfig) {
        viewModelScope.launch {
            configRepo.saveConfig(newConfig)
        }
    }

    fun attachSurface(surfaceView: OpenGlView) {
        srtStreamer.attachSurface(surfaceView)
        srtStreamer.startPreview()
    }

    fun detachSurface() {
        srtStreamer.detachSurface()
    }

    fun startStream() {
        val cfg = config.value
        if (cfg.srtHost.isBlank()) return

        // Reconectar WebSocket con el host de streaming
        // (ya estaba conectado desde startup, esto lo refresca)
        obsApiClient.connect(cfg.obsApiHost)
        srtStreamer.startStream(cfg)

        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        obsApiClient.sendCameraOnline(cfg.cameraNumber, deviceName)
    }

    fun stopStream() {
        val cfg = config.value
        obsApiClient.sendCameraOffline(cfg.cameraNumber, "user_stop")
        srtStreamer.stopStream()
        obsApiClient.disconnect()
    }

    fun runPrerequisiteChecks() {
        viewModelScope.launch {
            _prerequisitesDone.value = false
            _prerequisiteSteps.value = initialSteps()
            delay(300)

            // Step 1: Controlador OBS alcanzable (HTTP — rápido y directo)
            _prerequisiteSteps.value = updateStep(0, StepStatus.CHECKING)
            val host = config.value.obsApiHost
            if (!obsApiClient.isServerReachable(host)) {
                _prerequisiteSteps.value = updateStep(0, StepStatus.FAILED,
                    "No fue posible conectarse con el controlador OBS.\nVerifica que esté ejecutándose.")
                return@launch
            }
            _prerequisiteSteps.value = updateStep(0, StepStatus.OK)

            // Abrir WebSocket — el servidor enviará STATE_UPDATE de inmediato
            // (contiene enabledCameras + obsConnected)
            obsApiClient.connect(host)

            // Esperar primer STATE_UPDATE (máx. 3s) para obtener estado real del servidor
            withTimeoutOrNull(3000) {
                obsApiClient.enabledCameras.first { it.isNotEmpty() }
            } ?: run {
                // Fallback si el servidor tardó o no envió cámaras configuradas
                _enabledCameras.value = (1..6).toList()
            }

            delay(200)

            // Step 2: OBS Studio — informativo desde STATE_UPDATE, no bloquea al camarógrafo
            _prerequisiteSteps.value = updateStep(1, StepStatus.CHECKING)
            val obsReady = obsApiClient.obsConnectedRemote.value
            _prerequisiteSteps.value = updateStep(
                1,
                if (obsReady) StepStatus.OK else StepStatus.WARNING,
                if (obsReady) "" else "OBS Studio no está disponible.\nEl director deberá abrirlo antes de la transmisión."
            )
            delay(200)

            _prerequisitesDone.value = true
        }
    }

    private fun updateStep(index: Int, status: StepStatus, errorMessage: String = ""): List<PrerequisiteStep> {
        return _prerequisiteSteps.value.mapIndexed { i, step ->
            if (i == index) step.copy(status = status, errorMessage = errorMessage) else step
        }
    }

    override fun onCleared() {
        super.onCleared()
        srtStreamer.release()
        obsApiClient.disconnect()
    }
}
