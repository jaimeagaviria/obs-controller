package com.obsremotecamera

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import com.pedro.library.view.OpenGlView
import androidx.lifecycle.viewModelScope
import com.obsremotecamera.config.AppConfig
import com.obsremotecamera.config.ConfigRepository
import com.obsremotecamera.network.NetworkMonitor
import com.obsremotecamera.network.ObsApiClient
import com.obsremotecamera.network.TailscaleManager
import com.obsremotecamera.streaming.SrtStreamer
import com.obsremotecamera.streaming.StreamState
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TailscaleStatus {
    CHECKING, NOT_INSTALLED, VPN_OFF, SERVER_UNREACHABLE, CONNECTED
}

enum class StepStatus { PENDING, CHECKING, OK, FAILED }

data class PrerequisiteStep(
    val label: String,
    val status: StepStatus,
    val errorMessage: String = ""
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val configRepo = ConfigRepository(application)
    val tailscaleManager = TailscaleManager(application)
    val networkMonitor = NetworkMonitor(application)
    val srtStreamer = SrtStreamer(application)
    private val obsApiClient = ObsApiClient()

    val config: StateFlow<AppConfig> = configRepo.configFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppConfig())

    val streamState: StateFlow<StreamState> = srtStreamer.state
    val currentBitrateKbps: StateFlow<Long> = srtStreamer.currentBitrateKbps
    val apiConnectionStatus = obsApiClient.connectionStatus

    private val _tailscaleStatus = MutableStateFlow(TailscaleStatus.CHECKING)
    val tailscaleStatus: StateFlow<TailscaleStatus> = _tailscaleStatus.asStateFlow()

    private fun initialSteps() = listOf(
        PrerequisiteStep("VPN", StepStatus.PENDING),
        PrerequisiteStep("Controlador OBS", StepStatus.PENDING),
        PrerequisiteStep("OBS Studio", StepStatus.PENDING)
    )
    private val _prerequisiteSteps = MutableStateFlow(initialSteps())
    val prerequisiteSteps: StateFlow<List<PrerequisiteStep>> = _prerequisiteSteps.asStateFlow()

    private val _prerequisitesDone = MutableStateFlow(false)
    val prerequisitesDone: StateFlow<Boolean> = _prerequisitesDone.asStateFlow()

    private val _streamDurationSeconds = MutableStateFlow(0L)
    val streamDurationSeconds: StateFlow<Long> = _streamDurationSeconds.asStateFlow()

    private var durationJob: Job? = null
    private var tailscaleCheckJob: Job? = null

    init {
        networkMonitor.startMonitoring()

        // Restart stream when SRT-relevant config changes while streaming
        viewModelScope.launch {
            var previousConfig: AppConfig? = null
            config.collect { newConfig ->
                val prev = previousConfig
                previousConfig = newConfig
                val isActive = streamState.value.let {
                    it is StreamState.Streaming || it is StreamState.Connecting || it is StreamState.Reconnecting
                }
                if (prev != null && isActive && prev.effectivePort != newConfig.effectivePort) {
                    srtStreamer.stopStream()
                    delay(350)
                    srtStreamer.startStream(newConfig)
                }
            }
        }

        // Track stream duration
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
        if (cfg.tailscaleHost.isBlank()) return

        // Connect WebSocket to obs-controller-api
        obsApiClient.connect(cfg.tailscaleHost)

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

    fun checkTailscale() {
        tailscaleCheckJob?.cancel()
        tailscaleCheckJob = viewModelScope.launch {
            _tailscaleStatus.value = TailscaleStatus.CHECKING

            // Step 1: Is Tailscale installed?
            if (!tailscaleManager.isTailscaleInstalled()) {
                _tailscaleStatus.value = TailscaleStatus.NOT_INSTALLED
                return@launch
            }

            // Step 2: Wait for VPN — poll with active refresh every 3s
            if (!networkMonitor.isVpnActive.value) {
                _tailscaleStatus.value = TailscaleStatus.VPN_OFF
                while (!networkMonitor.isVpnActive.value) {
                    delay(3000)
                    networkMonitor.refresh()
                }
            }

            // Step 3: Wait for server reachable — poll every 5s
            val host = config.value.tailscaleHost
            if (host.isBlank()) {
                _tailscaleStatus.value = TailscaleStatus.SERVER_UNREACHABLE
                return@launch
            }

            while (!tailscaleManager.isServerReachable(host)) {
                _tailscaleStatus.value = TailscaleStatus.SERVER_UNREACHABLE
                delay(5000)
            }

            _tailscaleStatus.value = TailscaleStatus.CONNECTED

            // Step 4: Keep monitoring — detect disconnects and reconnect API
            while (isActive) {
                delay(10000)
                networkMonitor.refresh()
                if (!networkMonitor.isVpnActive.value) {
                    _tailscaleStatus.value = TailscaleStatus.VPN_OFF
                    checkTailscale()
                    return@launch
                }
                if (!tailscaleManager.isServerReachable(host)) {
                    _tailscaleStatus.value = TailscaleStatus.SERVER_UNREACHABLE
                } else if (_tailscaleStatus.value != TailscaleStatus.CONNECTED) {
                    _tailscaleStatus.value = TailscaleStatus.CONNECTED
                }
            }
        }
    }

    fun runPrerequisiteChecks() {
        viewModelScope.launch {
            _prerequisitesDone.value = false
            _prerequisiteSteps.value = initialSteps()
            delay(300)

            // Step 1: Tailscale VPN
            _prerequisiteSteps.value = updateStep(0, StepStatus.CHECKING)
            networkMonitor.refresh()
            delay(400)
            if (!tailscaleManager.isTailscaleInstalled()) {
                _prerequisiteSteps.value = updateStep(0, StepStatus.FAILED,
                    "Tailscale no está instalado.\nInstálalo desde Play Store para continuar.")
                return@launch
            }
            if (!networkMonitor.isVpnActive.value) {
                _prerequisiteSteps.value = updateStep(0, StepStatus.FAILED,
                    "Tailscale está instalado pero la VPN no está activa.\nAbre Tailscale y conéctate a tu red.")
                return@launch
            }
            _prerequisiteSteps.value = updateStep(0, StepStatus.OK)
            delay(300)

            // Step 2: Server reachable
            _prerequisiteSteps.value = updateStep(1, StepStatus.CHECKING)
            val host = config.value.tailscaleHost
            if (!tailscaleManager.isServerReachable(host)) {
                _prerequisiteSteps.value = updateStep(1, StepStatus.FAILED,
                    "No fue posible conectarse con el controlador OBS.")
                return@launch
            }
            _prerequisiteSteps.value = updateStep(1, StepStatus.OK)
            delay(300)

            // Step 3: OBS Studio connected
            _prerequisiteSteps.value = updateStep(2, StepStatus.CHECKING)
            if (!tailscaleManager.isObsConnected(host)) {
                _prerequisiteSteps.value = updateStep(2, StepStatus.FAILED,
                    "No fue posible conectarse con el OBS Studio.")
                return@launch
            }
            _prerequisiteSteps.value = updateStep(2, StepStatus.OK)
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
        networkMonitor.stopMonitoring()
    }
}
