package com.obsremotecamera.streaming

sealed class StreamState {
    data object Idle : StreamState()
    data object Connecting : StreamState()
    data class Streaming(val bitrateKbps: Long = 0) : StreamState()
    data class Error(val message: String) : StreamState()
    data object Reconnecting : StreamState()
}
