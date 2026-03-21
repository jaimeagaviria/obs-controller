<template>
  <div class="preview-view">
    <section class="section py-4">
      <div class="container">

        <div class="is-flex is-align-items-center is-justify-content-space-between mb-3">
          <h1 class="title is-5 has-text-white mb-0">Preview Cámaras</h1>
          <span class="tag is-dark is-small">HLS · ~3s latencia</span>
        </div>

        <!-- Sin cámaras habilitadas -->
        <div v-if="!enabledCameras.length" class="has-text-centered has-text-grey py-6">
          Sin cámaras habilitadas. Configúralas en
          <RouterLink to="/config" class="has-text-info">Config</RouterLink>.
        </div>

        <template v-else>
          <!-- Selector de cámaras -->
          <div class="cam-selector mb-4">
            <button
              v-for="cam in enabledCameras"
              :key="cam"
              class="button is-medium cam-btn"
              :class="selectedCam === cam ? 'is-primary' : 'is-dark'"
              @click="selectCamera(cam)"
            >
              Cam {{ cam }}
            </button>
            <button
              v-if="selectedCam !== null"
              class="button is-medium is-danger cam-btn-close"
              @click="closePreview"
              title="Cerrar preview"
            >
              ✕
            </button>
          </div>

          <!-- Aviso orientación -->
          <div v-if="selectedCam !== null" class="orientation-warning mb-3">
            Si el video aparece en vertical, el colaborador tiene el giro del celular bloqueado.
            Pedirle que <strong>desbloquee la rotación</strong> en la configuración rápida de
            Android y gire el teléfono en horizontal.
          </div>

          <!-- Player HLS -->
          <div v-if="selectedCam !== null" class="preview-frame-wrapper">
            <video
              ref="videoEl"
              class="preview-video"
              autoplay
              muted
              playsinline
              controls
            />
            <div v-if="hlsError" class="preview-error">
              {{ hlsError }}
            </div>
            <div class="preview-label">cam{{ selectedCam }} · HLS via MediaMTX</div>
          </div>

          <!-- Estado sin cámara seleccionada -->
          <div v-else class="preview-placeholder">
            <div class="has-text-grey is-size-6">Selecciona una cámara para ver el preview</div>
            <div class="has-text-grey is-size-7 mt-2">
              El stream llega directo desde el celular → MediaMTX → aquí.<br>
              No pasa por OBS Studio.
            </div>
          </div>
        </template>

      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, watch, onUnmounted, nextTick } from 'vue'
import Hls from 'hls.js'
import { useWebSocket } from '../composables/useWebSocket.js'

const { state } = useWebSocket()

const selectedCam = ref(null)
const videoEl    = ref(null)
const hlsError   = ref(null)
let   hlsInstance = null

const enabledCameras = computed(() =>
  Array.isArray(state.value.enabledCameras) ? state.value.enabledCameras : []
)

// URL HLS proxiada a través del nginx del frontend (mismo puerto 5180)
// nginx reenvía /hls/ → MediaMTX :8888 en el servidor
function hlsUrl(cam) {
  return `/hls/cam${cam}/index.m3u8`
}

function destroyHls() {
  if (hlsInstance) {
    hlsInstance.destroy()
    hlsInstance = null
  }
}

function closePreview() {
  destroyHls()
  selectedCam.value = null
  hlsError.value = null
}

function selectCamera(cam) {
  if (selectedCam.value === cam) return
  closePreview()
  selectedCam.value = cam
}

// Iniciar hls.js cuando el <video> esté montado y haya cámara seleccionada
watch([selectedCam, videoEl], async ([cam, el]) => {
  if (!cam || !el) return
  await nextTick()
  destroyHls()
  hlsError.value = null

  const url = hlsUrl(cam)

  if (Hls.isSupported()) {
    hlsInstance = new Hls({
      lowLatencyMode: true,
      liveSyncDurationCount: 2,
      liveMaxLatencyDurationCount: 5
    })
    hlsInstance.loadSource(url)
    hlsInstance.attachMedia(el)
    hlsInstance.on(Hls.Events.MANIFEST_PARSED, () => {
      el.play().catch(() => {})
    })
    hlsInstance.on(Hls.Events.ERROR, (_, data) => {
      if (data.fatal) {
        if (data.type === Hls.ErrorTypes.NETWORK_ERROR) {
          hlsError.value = 'El celular no está transmitiendo en este momento.'
        } else {
          hlsError.value = `Error de stream: ${data.details}`
        }
      }
    })
  } else if (el.canPlayType('application/vnd.apple.mpegurl')) {
    // Safari — soporta HLS nativo
    el.src = url
    el.play().catch(() => {})
  } else {
    hlsError.value = 'Este navegador no soporta HLS.'
  }
})

onUnmounted(destroyHls)
</script>

<style scoped>
.preview-view {
  min-height: 100vh;
  background-color: #1a1a2e;
}

.cam-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
}

.cam-btn { min-width: 90px; }
.cam-btn-close { margin-left: auto; }

.orientation-warning {
  background: rgba(255, 190, 0, 0.08);
  border: 1px solid rgba(255, 190, 0, 0.3);
  border-radius: 8px;
  padding: 0.6rem 0.9rem;
  font-size: 0.82rem;
  color: #f0c040;
  line-height: 1.4;
}
.orientation-warning strong { color: #ffd060; }

.preview-frame-wrapper {
  position: relative;
  width: 100%;
  background: #000;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #333;
  aspect-ratio: 16 / 9;
}

.preview-video {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: contain;
  background: #000;
}

.preview-error {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,0.7);
  color: #ff6b6b;
  font-size: 0.85rem;
  text-align: center;
  padding: 1rem;
}

.preview-label {
  position: absolute;
  bottom: 8px;
  left: 10px;
  font-size: 0.7rem;
  color: rgba(255,255,255,0.45);
  font-family: monospace;
  background: rgba(0,0,0,0.4);
  padding: 2px 6px;
  border-radius: 4px;
  pointer-events: none;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  border: 1px dashed #333;
  border-radius: 10px;
  padding: 2rem;
  text-align: center;
}
</style>
