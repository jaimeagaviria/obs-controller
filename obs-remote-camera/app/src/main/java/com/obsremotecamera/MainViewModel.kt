package com.obsremotecamera

import android.app.Application
import android.os.Build
import android.view.SurfaceView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.obsremotecamera.config.AppConfig
import com.obsremotecamera.config.ConfigRepository
import com.obsremotecamera.network.NetworkMonitor
import com.obsremotecamera.network.ObsApiClient
import com.obsremotecamera.network.TailscaleManager
import com.obsremotecamera.streaming.SrtStreamer
import com.obsremotecamera.streaming.StreamState
import kotlinx.coroutines.Job
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

    private val _tailscaleStatus = MutableStateFlow(TailscaleStatus.CHECKING)
    val tailscaleStatus: StateFlow<TailscaleStatus> = _tailscaleStatus.asStateFlow()

    private val _streamDurationSeconds = MutableStateFlow(0L)
    val streamDurationSeconds: StateFlow<Long> = _streamDurationSeconds.asStateFlow()

    private var durationJob: Job? = null
    private var tailscaleCheckJob: Job? = null

    init {
        networkMonitor.startMonitoring()

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

    fun attachSurface(surfaceView: SurfaceView) {
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

            // Step 2: Is VPN active?
            if (!networkMonitor.isVpnActive.value) {
                _tailscaleStatus.value = TailscaleStatus.VPN_OFF
                // Poll every 3 seconds until VPN is active
                while (!networkMonitor.isVpnActive.value) {
                    delay(3000)
                }
            }

            // Step 3: Is server reachable?
            val host = config.value.tailscaleHost
            if (host.isBlank()) {
                _tailscaleStatus.value = TailscaleStatus.SERVER_UNREACHABLE
                return@launch
            }

            val reachable = tailscaleManager.isServerReachable(host)
            _tailscaleStatus.value = if (reachable) {
                TailscaleStatus.CONNECTED
            } else {
                TailscaleStatus.SERVER_UNREACHABLE
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        srtStreamer.release()
        obsApiClient.disconnect()
        networkMonitor.stopMonitoring()
    }
}
