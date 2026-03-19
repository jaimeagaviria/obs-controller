package com.obsremotecamera.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_config")

class ConfigRepository(private val context: Context) {

    companion object {
        private val KEY_CAMERA_NUMBER = intPreferencesKey("camera_number")
        private val KEY_TAILSCALE_HOST = stringPreferencesKey("tailscale_host")
        private val KEY_SRT_BASE_PORT = intPreferencesKey("srt_base_port")
        private val KEY_RESOLUTION_WIDTH = intPreferencesKey("resolution_width")
        private val KEY_RESOLUTION_HEIGHT = intPreferencesKey("resolution_height")
        private val KEY_FPS = intPreferencesKey("fps")
        private val KEY_VIDEO_BITRATE = intPreferencesKey("video_bitrate_kbps")
    }

    val configFlow: Flow<AppConfig> = context.dataStore.data.map { prefs ->
        AppConfig(
            cameraNumber = prefs[KEY_CAMERA_NUMBER] ?: 1,
            tailscaleHost = prefs[KEY_TAILSCALE_HOST] ?: "100.108.32.54",
            srtBasePort = prefs[KEY_SRT_BASE_PORT] ?: 5000,
            resolutionWidth = prefs[KEY_RESOLUTION_WIDTH] ?: 1920,
            resolutionHeight = prefs[KEY_RESOLUTION_HEIGHT] ?: 1080,
            fps = prefs[KEY_FPS] ?: 30,
            videoBitrateKbps = prefs[KEY_VIDEO_BITRATE] ?: 4000
        )
    }

    suspend fun saveConfig(config: AppConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CAMERA_NUMBER] = config.cameraNumber
            prefs[KEY_TAILSCALE_HOST] = config.tailscaleHost
            prefs[KEY_SRT_BASE_PORT] = config.srtBasePort
            prefs[KEY_RESOLUTION_WIDTH] = config.resolutionWidth
            prefs[KEY_RESOLUTION_HEIGHT] = config.resolutionHeight
            prefs[KEY_FPS] = config.fps
            prefs[KEY_VIDEO_BITRATE] = config.videoBitrateKbps
        }
    }
}
