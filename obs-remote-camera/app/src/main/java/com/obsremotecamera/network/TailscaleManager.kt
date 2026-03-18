package com.obsremotecamera.network

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class TailscaleManager(private val context: Context) {

    companion object {
        const val TAILSCALE_PACKAGE = "com.tailscale.ipn"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    fun isTailscaleInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(TAILSCALE_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getLaunchIntent(): Intent? {
        return context.packageManager.getLaunchIntentForPackage(TAILSCALE_PACKAGE)
    }

    fun getPlayStoreIntent(): Intent {
        return Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$TAILSCALE_PACKAGE")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    suspend fun isServerReachable(host: String, port: Int = 3010): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("http://$host:$port/health")
                    .get()
                    .build()
                val response = httpClient.newCall(request).execute()
                response.use { it.isSuccessful }
            } catch (_: Exception) {
                false
            }
        }
    }
}
