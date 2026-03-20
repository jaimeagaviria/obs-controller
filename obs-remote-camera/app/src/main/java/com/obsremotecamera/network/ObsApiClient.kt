package com.obsremotecamera.network

import com.obsremotecamera.ui.components.ApiConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _connectionStatus = MutableStateFlow(ApiConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ApiConnectionStatus> = _connectionStatus.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    fun connect(host: String, port: Int = 3010) {
        disconnect()
        _connectionStatus.value = ApiConnectionStatus.CONNECTING
        val request = Request.Builder()
            .url("ws://$host:$port/ws")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionStatus.value = ApiConnectionStatus.CONNECTED
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Handle STATE_UPDATE if needed in future
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionStatus.value = ApiConnectionStatus.DISCONNECTED
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionStatus.value = ApiConnectionStatus.DISCONNECTED
            }
        })
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

    suspend fun isObsConnected(host: String, port: Int = 3010): Boolean {
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
                response.use {
                    if (!it.isSuccessful) return@withContext false
                    val body = it.body?.string() ?: return@withContext false
                    JSONObject(body).optBoolean("obsConnected", false)
                }
            } catch (_: Exception) {
                false
            }
        }
    }
}
