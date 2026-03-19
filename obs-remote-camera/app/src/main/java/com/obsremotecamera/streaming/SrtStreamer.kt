package com.obsremotecamera.streaming

import android.content.Context
import android.media.MediaCodecList
import android.util.Log
import com.obsremotecamera.config.AppConfig
import com.pedro.common.ConnectChecker
import com.pedro.library.srt.SrtCamera2
import com.pedro.library.view.OpenGlView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SrtStreamer(private val context: Context) : ConnectChecker {

    private var srtCamera: SrtCamera2? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var reconnectJob: Job? = null
    private var currentConfig: AppConfig? = null

    private val _state = MutableStateFlow<StreamState>(StreamState.Idle)
    val state: StateFlow<StreamState> = _state.asStateFlow()

    private val _currentBitrateKbps = MutableStateFlow(0L)
    val currentBitrateKbps: StateFlow<Long> = _currentBitrateKbps.asStateFlow()

    fun attachSurface(surfaceView: OpenGlView) {
        srtCamera = SrtCamera2(surfaceView, this)
    }

    fun detachSurface() {
        stopStream()
        srtCamera?.stopPreview()
        srtCamera = null
    }

    fun startPreview() {
        val camera = srtCamera ?: return
        if (!camera.isOnPreview) {
            camera.startPreview()
        }
    }

    fun startStream(config: AppConfig) {
        val camera = srtCamera ?: run {
            _state.value = StreamState.Error("Cámara no inicializada")
            return
        }

        currentConfig = config
        reconnectJob?.cancel()

        val preparedVideo = camera.prepareVideo(
            config.resolutionWidth,
            config.resolutionHeight,
            config.fps,
            config.videoBitrateKbps * 1000,
            0
        )

        val preparedAudio = camera.prepareAudio(
            config.audioBitrateKbps * 1000,
            config.audioSampleRate,
            true
        )

        if (!preparedVideo || !preparedAudio) {
            _state.value = StreamState.Error("Error al preparar encoder. Intenta menor resolución.")
            return
        }

        _state.value = StreamState.Connecting
        val url = config.buildSrtUrl()
        camera.startStream(url)
    }

    fun stopStream() {
        reconnectJob?.cancel()
        reconnectJob = null
        currentConfig = null
        _state.value = StreamState.Idle
        _currentBitrateKbps.value = 0
        val camera = srtCamera ?: return
        if (camera.isStreaming) {
            camera.stopStream()
        }
    }

    fun isStreaming(): Boolean = srtCamera?.isStreaming == true

    fun release() {
        stopStream()
        srtCamera?.stopPreview()
        srtCamera = null
        scope.cancel()
    }

    fun isH265Supported(): Boolean {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        return codecList.codecInfos.any { info ->
            info.isEncoder && info.supportedTypes.any { type ->
                type.equals("video/hevc", ignoreCase = true)
            }
        }
    }

    // --- ConnectChecker callbacks ---

    override fun onConnectionStarted(url: String) {
        Log.d(TAG, "onConnectionStarted: $url")
        _state.value = StreamState.Connecting
    }

    override fun onConnectionSuccess() {
        Log.d(TAG, "onConnectionSuccess")
        _state.value = StreamState.Streaming()
        reconnectJob?.cancel()
    }

    override fun onConnectionFailed(reason: String) {
        Log.d(TAG, "onConnectionFailed: $reason")
        if (_state.value !is StreamState.Idle) {
            _state.value = StreamState.Reconnecting
            scheduleReconnect()
        }
    }

    override fun onNewBitrate(bitrate: Long) {
        _currentBitrateKbps.value = bitrate / 1000
        val current = _state.value
        if (current is StreamState.Streaming) {
            _state.value = StreamState.Streaming(bitrateKbps = bitrate / 1000)
        }
    }

    override fun onDisconnect() {
        Log.d(TAG, "onDisconnect — state=${_state.value}")
        if (_state.value is StreamState.Streaming) {
            _state.value = StreamState.Reconnecting
            scheduleReconnect()
        }
    }

    override fun onAuthError() {
        _state.value = StreamState.Error("Error de autenticación SRT")
    }

    override fun onAuthSuccess() {
        // SRT auth OK
    }

    // Programa UN solo intento tras un delay.
    // onConnectionFailed/onDisconnect vuelven a llamar scheduleReconnect si hace falta.
    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        val config = currentConfig ?: return
        reconnectJob = scope.launch {
            delay(800L)
            if (!isActive) return@launch
            val camera = srtCamera ?: return@launch
            Log.d(TAG, "reconnect attempt — isStreaming=${camera.isStreaming} isOnPreview=${camera.isOnPreview}")

            // Siempre detener para resetear los encoders al estado inicial
            try { camera.stopStream() } catch (_: Exception) {}
            delay(200L)
            if (!isActive) return@launch

            // Re-preparar encoders (obligatorio tras stopStream)
            val videoOk = camera.prepareVideo(
                config.resolutionWidth, config.resolutionHeight,
                config.fps, config.videoBitrateKbps * 1000, 0
            )
            val audioOk = camera.prepareAudio(
                config.audioBitrateKbps * 1000, config.audioSampleRate, true
            )
            Log.d(TAG, "reconnect prepare — videoOk=$videoOk audioOk=$audioOk")

            if (!camera.isOnPreview) camera.startPreview()

            if (videoOk && audioOk) {
                camera.startStream(config.buildSrtUrl())
                Log.d(TAG, "reconnect startStream: ${config.buildSrtUrl()}")
            }
        }
    }

    // Mantener attemptReconnect como alias para compatibilidad
    private fun attemptReconnect() = scheduleReconnect()

    companion object {
        private const val TAG = "SrtStreamer"
    }
}
