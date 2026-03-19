package com.obsremotecamera

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.obsremotecamera.ui.ConfigScreen
import com.obsremotecamera.ui.StartupCheckScreen
import com.obsremotecamera.ui.StreamScreen

class MainActivity : ComponentActivity() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            setupContent()
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            setupContent()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    private fun setupContent() {
        setContent {
            val darkColors = darkColorScheme(
                primary = Color(0xFF2196F3),
                onPrimary = Color.White,
                surface = Color(0xFF121212),
                onSurface = Color.White,
                background = Color.Black,
                onBackground = Color.White
            )

            MaterialTheme(colorScheme = darkColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()

    NavHost(navController = navController, startDestination = "startup") {
        composable("startup") {
            StartupCheckScreen(
                viewModel = viewModel,
                onAllDone = {
                    navController.navigate("stream") {
                        popUpTo("startup") { inclusive = true }
                    }
                },
                onExit = {
                    (context as? Activity)?.finishAndRemoveTask()
                }
            )
        }
        composable("stream") {
            StreamScreen(
                viewModel = viewModel,
                onNavigateToConfig = { navController.navigate("config") },
                onExit = {
                    viewModel.stopStream()
                    (context as? Activity)?.finishAndRemoveTask()
                }
            )
        }
        composable("config") {
            ConfigScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
