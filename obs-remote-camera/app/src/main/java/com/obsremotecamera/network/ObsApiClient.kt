package com.obsremotecamera.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    fun connect(host: String, port: Int = 3010) {
        disconnect()
        val request = Request.Builder()
            .url("ws://$host:$port/ws")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Connected to obs-controller-api
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Handle STATE_UPDATE if needed in future
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // Reconnect handled by caller
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                // Connection closed
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
    }
}
