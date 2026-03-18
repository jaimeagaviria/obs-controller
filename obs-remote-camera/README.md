# obs-remote-camera

Aplicación Android nativa para transmitir video en vivo desde un celular hacia OBS Studio usando SRT sobre Tailscale.

## Requisitos

- **JDK 17+** (detectado: OpenJDK 21)
- **Android SDK** con:
  - `platforms;android-34`
  - `build-tools;34.0.0`
- **Tailscale** instalado en el celular y en el PC con OBS

## Setup

```bash
# 1. Instalar Android SDK CLI tools (si no tienes Android Studio)
# https://developer.android.com/studio#command-line-tools-only

# 2. Configurar local.properties
echo "sdk.dir=/ruta/a/tu/Android/Sdk" > local.properties

# 3. Instalar componentes SDK necesarios
sdkmanager "platforms;android-34" "build-tools;34.0.0"
```

## Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# Instalar en celular conectado por USB
./gradlew installDebug
```

## Estructura

```
app/src/main/java/com/obsremotecamera/
├── MainActivity.kt              # Entry point, NavHost, permisos
├── MainViewModel.kt             # ViewModel central con StateFlow
├── ObsRemoteCameraApp.kt        # Application class
├── config/
│   ├── AppConfig.kt             # Data class de configuración
│   └── ConfigRepository.kt      # DataStore persistencia
├── streaming/
│   ├── SrtStreamer.kt            # RootEncoder + SRT wrapper
│   └── StreamState.kt           # Estados del stream
├── network/
│   ├── TailscaleManager.kt      # Detección VPN + Intent lanzador
│   ├── ObsApiClient.kt          # WebSocket a obs-controller-api
│   └── NetworkMonitor.kt        # ConnectivityManager observer
└── ui/
    ├── StreamScreen.kt           # Preview + overlays (landscape)
    ├── ConfigScreen.kt           # Configuración cámara/red
    └── components/
        ├── TailscaleIndicator.kt # Badge verde/rojo/amarillo
        ├── LiveBadge.kt          # LIVE con punto parpadeante
        └── BitrateOverlay.kt     # Bitrate + resolución
```
