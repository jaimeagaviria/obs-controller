<template>
  <div class="preview-view">
    <section class="section py-4">
      <div class="container">

        <div class="is-flex is-align-items-center is-justify-content-space-between mb-4">
          <h1 class="title is-5 has-text-white mb-0">Preview Cámaras</h1>
          <span class="tag is-dark is-medium">VDO.ninja</span>
        </div>

        <!-- Sin cámaras -->
        <div v-if="!enabledCameras.length" class="has-text-centered has-text-grey py-6">
          Sin cámaras habilitadas. Configúralas en <RouterLink to="/config" class="has-text-info">Config</RouterLink>.
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
              @click="selectedCam = null"
              title="Cerrar preview"
            >
              ✕
            </button>
          </div>

          <!-- Aviso de orientación -->
          <div v-if="selectedCam !== null" class="orientation-warning mb-3">
            <span class="icon-text has-text-warning">
              <span>Si el video aparece en vertical, el colaborador tiene el giro del celular bloqueado — pedirle que lo desbloquee en la configuración rápida de Android.</span>
            </span>
          </div>

          <!-- Preview iframe -->
          <div v-if="selectedCam !== null" class="preview-frame-wrapper">
            <iframe
              :key="selectedCam"
              :src="previewUrl"
              class="preview-frame"
              allow="autoplay;fullscreen"
              allowfullscreen
              referrerpolicy="no-referrer"
            />
            <div class="preview-label">jaula_camara{{ selectedCam }}</div>
          </div>

          <!-- Estado inicial -->
          <div v-else class="preview-placeholder">
            <div class="has-text-grey is-size-6">Selecciona una cámara para ver el preview</div>
            <div class="has-text-grey is-size-7 mt-2">Solo se conecta una cámara a la vez para no afectar la transmisión</div>
          </div>
        </template>

      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useWebSocket } from '../composables/useWebSocket.js'

const { state } = useWebSocket()

const selectedCam = ref(null)

const enabledCameras = computed(() =>
  Array.isArray(state.value.enabledCameras) ? state.value.enabledCameras : []
)

const previewUrl = computed(() => {
  if (selectedCam.value === null) return ''
  return `https://vdo.ninja/?view=jaula_camara${selectedCam.value}&bitrate=800&width=640&height=360&cleanoutput&autostart&noaudio`
})

function selectCamera(cam) {
  // Si se pulsa la misma, no hace nada (el iframe ya está cargado)
  if (selectedCam.value === cam) return
  // Cambiar destruye el iframe anterior (v-if + :key) → desconecta WebRTC
  selectedCam.value = null
  // Tick siguiente para que Vue destruya el iframe antes de crear el nuevo
  setTimeout(() => { selectedCam.value = cam }, 50)
}
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

.cam-btn {
  min-width: 90px;
}

.cam-btn-close {
  margin-left: auto;
}

.orientation-warning {
  background: rgba(255, 190, 0, 0.08);
  border: 1px solid rgba(255, 190, 0, 0.3);
  border-radius: 8px;
  padding: 0.6rem 0.9rem;
  font-size: 0.82rem;
  color: #f0c040;
  line-height: 1.4;
}

.preview-frame-wrapper {
  position: relative;
  width: 100%;
  background: #000;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #333;
  /* 16:9 aspect ratio */
  aspect-ratio: 16 / 9;
}

.preview-frame {
  width: 100%;
  height: 100%;
  border: none;
  display: block;
}

.preview-label {
  position: absolute;
  bottom: 8px;
  left: 10px;
  font-size: 0.7rem;
  color: rgba(255,255,255,0.4);
  font-family: monospace;
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
