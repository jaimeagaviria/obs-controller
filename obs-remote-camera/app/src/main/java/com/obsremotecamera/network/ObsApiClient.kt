package com.obsremotecamera.network

import com.obsremotecamera.ui.components.ApiConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ObsApiClient {

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastHost: String? = null
    private var reconnectJob: Job? = null
    private var intentionalDisconnect = false

    private val _connectionStatus = MutableStateFlow(ApiConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ApiConnectionStatus> = _connectionStatus.asStateFlow()

    // Estado recibido del servidor vía WebSocket STATE_UPDATE
    private val _enabledCameras = MutableStateFlow<List<Int>>(emptyList())
    val enabledCameras: StateFlow<List<Int>> = _enabledCameras.asStateFlow()

    private val _obsConnectedRemote = MutableStateFlow(false)
    val obsConnectedRemote: StateFlow<Boolean> = _obsConnectedRemote.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)   // sin timeout: WebSocket controlado por pingInterval
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    fun connect(host: String, port: Int = 3010) {
        intentionalDisconnect = false
        lastHost = host
        reconnectJob?.cancel()
        webSocket?.close(1000, "Reconnecting")
        webSocket = null
        _connectionStatus.value = ApiConnectionStatus.CONNECTING
        val request = Request.Builder()
            .url("ws://$host:$port/ws")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionStatus.value = ApiConnectionStatus.CONNECTED
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = JSONObject(text)
                    if (msg.optString("type") == "STATE_UPDATE") {
                        val state = msg.optJSONObject("state") ?: return

                        // Cámaras habilitadas
                        val arr = state.optJSONArray("enabledCameras")
                        if (arr != null) {
                            _enabledCameras.value = (0 until arr.length()).map { arr.getInt(it) }
                        }

                        // Estado de conexión OBS Studio (informativo)
                        _obsConnectedRemote.value = state.optBoolean("obsConnected", false)
                    }
                } catch (_: Exception) { /* ignorar mensajes malformados */ }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionStatus.value = ApiConnectionStatus.DISCONNECTED
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionStatus.value = ApiConnectionStatus.DISCONNECTED
                if (!intentionalDisconnect) scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        val host = lastHost ?: return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(3000)
            if (!intentionalDisconnect) connect(host)
        }
    }

    fun sendCameraOnline(cameraId: Int, deviceName: String) {
        val msg = JSONObject().apply {
            put("type", "CAMERA_ONLINE")
            put("cameraId", cameraId)
            put("device", deviceName)
        }
        webSocket?.send(msg.toString())
    }

    fun sendCameraOffline(cameraId: Int, reason: String = "user_stop") {
        val msg = JSONObject().apply {
            put("type", "CAMERA_OFFLINE")
            put("cameraId", cameraId)
            put("reason", reason)
        }
        webSocket?.send(msg.toString())
    }

    fun sendCameraError(cameraId: Int, error: String) {
        val msg = JSONObject().apply {
            put("type", "CAMERA_ERROR")
            put("cameraId", cameraId)
            put("error", error)
        }
        webSocket?.send(msg.toString())
    }

    fun disconnect() {
        intentionalDisconnect = true
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "App closing")
        webSocket = null
        _connectionStatus.value = ApiConnectionStatus.DISCONNECTED
    }

    suspend fun isServerReachable(host: String, port: Int = 3010): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("http://$host:$port/health")
                    .get()
                    .build()
                val response = OkHttpClient.Builder()
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .readTimeout(3, TimeUnit.SECONDS)
                    .build()
                    .newCall(request).execute()
                response.use { it.isSuccessful }
            } catch (_: Exception) {
                false
            }
        }
    }
}
