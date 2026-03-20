package com.obsremotecamera.config

data class AppConfig(
    val cameraNumber: Int = 1,
    val srtHost: String = "100.51.43.233",
    val srtBasePort: Int = 8890,
    val obsApiHost: String = "100.108.32.54",
    val resolutionWidth: Int = 1920,
    val resolutionHeight: Int = 1080,
    val fps: Int = 30,
    val videoBitrateKbps: Int = 4000,
    val audioBitrateKbps: Int = 128,
    val audioSampleRate: Int = 44100,
    val srtLatencyMs: Int = 200
) {
    val streamId: String get() = "publish:cam$cameraNumber"

    val resolutionLabel: String get() = when {
        resolutionWidth == 1280 -> "720p"
        fps == 60 -> "1080p60"
        else -> "1080p"
    }

    val bitrateLabel: String get() = "${videoBitrateKbps / 1000} Mbps"

    fun buildSrtUrl(): String {
        return "srt://$srtHost:$srtBasePort/publish:cam$cameraNumber"
    }
}
