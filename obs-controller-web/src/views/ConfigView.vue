<template>
  <div class="config-view">
    <section class="section py-4">
      <div class="container">
        <h1 class="title has-text-white">Configuración</h1>

        <!-- OBS Status -->
        <div class="mb-4">
          <span class="tag is-medium" :class="state.obsConnected ? 'is-success' : 'is-danger'">
            OBS: {{ state.obsConnected ? 'Conectado' : 'Desconectado' }}
          </span>
          <span v-if="state.obsConnectionError && !state.obsConnected" class="ml-2 has-text-danger is-size-7">
            {{ state.obsConnectionError }}
          </span>
        </div>

        <!-- New Match -->
        <div class="box new-match-box mb-4">
          <div class="is-flex is-align-items-center is-justify-content-space-between">
            <h2 class="title is-5 has-text-white mb-0">Nuevo Partido</h2>
            <button
              v-if="!confirmNewMatch"
              class="button is-warning"
              @click="confirmNewMatch = true"
            >
              ⟳ Reiniciar
            </button>
            <div v-else class="is-flex is-align-items-center" style="gap: 0.5rem;">
              <span class="has-text-warning is-size-7">¿Confirmar?</span>
              <button class="button is-danger is-small" @click="startNewMatch">Sí</button>
              <button class="button is-dark is-small" @click="confirmNewMatch = false">No</button>
            </div>
          </div>
        </div>

        <!-- Match Info -->
        <div class="box has-background-dark mb-4">
          <h2 class="title is-5 has-text-white">Información del Partido</h2>
          <div class="field">
            <label class="label has-text-grey-light">Equipo Local</label>
            <div class="control">
              <input class="input is-dark-input" type="text" v-model="form.homeTeam" placeholder="Nombre del equipo local" />
            </div>
          </div>
          <div class="field">
            <label class="label has-text-grey-light">Equipo Visitante</label>
            <div class="control">
              <input class="input is-dark-input" type="text" v-model="form.awayTeam" placeholder="Nombre del equipo visitante" />
            </div>
          </div>
          <div class="field">
            <label class="label has-text-grey-light">Nombre del Torneo</label>
            <div class="control">
              <input class="input is-dark-input" type="text" v-model="form.tournamentName" placeholder="Nombre del torneo" />
            </div>
          </div>
        </div>

        <!-- Cameras -->
        <div class="box has-background-dark mb-4">
          <h2 class="title is-5 has-text-white mb-1">Cámaras Habilitadas</h2>
          <p class="has-text-grey is-size-7 mb-3">Arrastra para reordenar. El orden determina cómo aparecen los botones en el panel.</p>

          <!-- Cámaras activas — draggable -->
          <div class="cam-drag-list mb-3">
            <div
              v-for="(cam, index) in form.enabledCameras"
              :key="cam"
              class="cam-drag-item"
              :class="{ 'is-dragging': dragIndex === index, 'drag-over': dropIndex === index }"
              draggable="true"
              @dragstart="onDragStart(index)"
              @dragover.prevent="onDragOver(index)"
              @drop.prevent="onDrop(index)"
              @dragend="onDragEnd"
            >
              <span class="drag-handle">⠿</span>
              <span class="cam-label">Cam {{ cam }}</span>
              <span class="cam-pos">pos. {{ index + 1 }}</span>
              <button class="cam-remove" @click="removeCamera(cam)" title="Quitar">✕</button>
            </div>
            <div v-if="form.enabledCameras.length === 0" class="cam-empty">
              Sin cámaras activas
            </div>
          </div>

          <!-- Cámaras disponibles para agregar -->
          <div v-if="availableCameras.length" class="cam-available">
            <span class="has-text-grey-light is-size-7 mr-2">Agregar:</span>
            <button
              v-for="cam in availableCameras"
              :key="cam"
              class="button is-dark is-small mr-1"
              @click="addCamera(cam)"
            >+ Cam {{ cam }}</button>
          </div>
        </div>

        <!-- OBS Connection -->
        <div class="box has-background-dark mb-4">
          <div
            class="is-flex is-align-items-center is-justify-content-space-between collapsible-header"
            @click="obsConnExpanded = !obsConnExpanded"
          >
            <h2 class="title is-5 has-text-white mb-0">Conexión OBS</h2>
            <span class="has-text-grey-light">{{ obsConnExpanded ? '▲' : '▼' }}</span>
          </div>
          <div v-if="obsConnExpanded" class="columns mt-3">
            <div class="column">
              <div class="field">
                <label class="label has-text-grey-light">Host OBS</label>
                <div class="control">
                  <input class="input is-dark-input" type="text" v-model="form.obsHost" placeholder="localhost" />
                </div>
              </div>
            </div>
            <div class="column is-narrow">
              <div class="field">
                <label class="label has-text-grey-light">Puerto OBS</label>
                <div class="control">
                  <input class="input is-dark-input" type="number" v-model.number="form.obsPort" placeholder="4455" style="width: 120px;" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- OBS Scene Names -->
        <div class="box has-background-dark mb-4">
          <div
            class="is-flex is-align-items-center is-justify-content-space-between collapsible-header"
            @click="scenesExpanded = !scenesExpanded"
          >
            <h2 class="title is-5 has-text-white mb-0">Nombres de Escenas OBS</h2>
            <span class="has-text-grey-light">{{ scenesExpanded ? '▲' : '▼' }}</span>
          </div>
          <div v-if="scenesExpanded" class="columns is-multiline mt-3">
            <div
              v-for="(label, key) in sceneLabels"
              :key="key"
              class="column is-half"
            >
              <div class="field">
                <label class="label has-text-grey-light">{{ label }}</label>
                <div class="control">
                  <input
                    class="input is-dark-input"
                    type="text"
                    v-model="form.obsSceneNames[key]"
                    :placeholder="label"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Replay de Goles -->
        <div class="box has-background-dark mb-4">
          <div
            class="is-flex is-align-items-center is-justify-content-space-between collapsible-header"
            @click="replayExpanded = !replayExpanded"
          >
            <div class="is-flex is-align-items-center" style="gap: 0.5rem;">
              <h2 class="title is-5 has-text-white mb-0">Repetición de Goles</h2>
              <span
                class="tag is-size-7"
                :class="state.obsReplayBuffer ? 'is-success' : 'is-dark'"
              >{{ state.obsReplayBuffer ? '● Activo' : '○ Inactivo' }}</span>
            </div>
            <span class="has-text-grey-light">{{ replayExpanded ? '▲' : '▼' }}</span>
          </div>
          <div v-if="replayExpanded" class="mt-3">
            <p class="has-text-grey is-size-7 mb-3">
              El Replay Buffer debe estar activo en OBS. Al marcar un gol, se guarda automáticamente
              y se reproduce en la escena configurada.
            </p>
            <div class="columns">
              <div class="column">
                <div class="field">
                  <label class="label has-text-grey-light">Escena Replay</label>
                  <div class="control">
                    <input class="input is-dark-input" type="text" v-model="form.replaySceneName" placeholder="Replay" />
                  </div>
                </div>
              </div>
              <div class="column">
                <div class="field">
                  <label class="label has-text-grey-light">Nombre del Media Source</label>
                  <div class="control">
                    <input class="input is-dark-input" type="text" v-model="form.replaySourceName" placeholder="ReplayVideo" />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </section>

    <!-- Sticky save bar -->
    <div class="save-bar">
      <button class="button is-primary is-fullwidth" @click="saveConfig">
        Guardar Configuración
      </button>
      <Transition name="fade">
        <div v-if="savedMessage" class="save-feedback">✓ {{ savedMessage }}</div>
      </Transition>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useWebSocket } from '../composables/useWebSocket.js'

const { state, send } = useWebSocket()

const sceneLabels = {
  '1': 'Cámara 1',
  '2': 'Cámara 2',
  '3': 'Cámara 3',
  '4': 'Cámara 4',
  '5': 'Cámara 5',
  '6': 'Cámara 6',
  'ALL_CAMERAS': 'Todas las Cámaras',
  'INTRO': 'Intro'
}

const form = ref({
  homeTeam: '',
  awayTeam: '',
  tournamentName: '',
  enabledCameras: [1, 2, 3, 4],
  obsHost: '192.168.1.53',
  obsPort: 4455,
  obsSceneNames: {
    '1': 'Escena Camara 1',
    '2': 'Escena Camara 2',
    '3': 'Escena Camara 3',
    '4': 'Escena Camara 4',
    '5': 'Escena Camara 5',
    '6': 'Escena Camara 6',
    'ALL_CAMERAS': 'Escena Todas',
    'INTRO': 'Escena Intro'
  },
  replaySceneName: 'Replay',
  replaySourceName: 'ReplayVideo'
})

const savedMessage = ref('')
const confirmNewMatch = ref(false)
const scenesExpanded = ref(false)
const obsConnExpanded = ref(false)
const replayExpanded = ref(false)

// ── Drag & drop cámaras ──
const dragIndex = ref(null)
const dropIndex = ref(null)

const availableCameras = computed(() =>
  [1, 2, 3, 4, 5, 6].filter(c => !form.value.enabledCameras.includes(c))
)

function sendCamerasNow() {
  send({ type: 'SET_MATCH_CONFIG', payload: { enabledCameras: form.value.enabledCameras } })
}

function addCamera(cam) {
  form.value.enabledCameras = [...form.value.enabledCameras, cam]
  sendCamerasNow()
}

function removeCamera(cam) {
  form.value.enabledCameras = form.value.enabledCameras.filter(c => c !== cam)
  sendCamerasNow()
}

function onDragStart(index) {
  dragIndex.value = index
}

function onDragOver(index) {
  dropIndex.value = index
}

function onDrop(index) {
  if (dragIndex.value === null || dragIndex.value === index) return
  const cameras = [...form.value.enabledCameras]
  const [moved] = cameras.splice(dragIndex.value, 1)
  cameras.splice(index, 0, moved)
  form.value.enabledCameras = cameras
  sendCamerasNow()
}

function onDragEnd() {
  dragIndex.value = null
  dropIndex.value = null
}

function populateFromState() {
  const s = state.value
  form.value.homeTeam = s.homeTeam || ''
  form.value.awayTeam = s.awayTeam || ''
  form.value.tournamentName = s.tournamentName || ''
  form.value.enabledCameras = Array.isArray(s.enabledCameras) ? [...s.enabledCameras] : [1, 2, 3, 4]
  form.value.obsHost = s.obsHost || '192.168.1.53'
  form.value.obsPort = s.obsPort || 4455
  form.value.obsSceneNames = s.obsSceneNames ? { ...s.obsSceneNames } : form.value.obsSceneNames
  form.value.replaySceneName = s.replaySceneName || 'Replay'
  form.value.replaySourceName = s.replaySourceName || 'ReplayVideo'
}

onMounted(() => {
  populateFromState()
})

// Re-populate when state first arrives (in case mounted fires before first WS message)
let populated = false
watch(
  () => state.value.obsHost,
  () => {
    if (!populated) {
      populated = true
      populateFromState()
    }
  }
)

function startNewMatch() {
  send({ type: 'NEW_MATCH' })
  confirmNewMatch.value = false
}

function saveConfig() {
  send({
    type: 'SET_MATCH_CONFIG',
    payload: {
      homeTeam: form.value.homeTeam,
      awayTeam: form.value.awayTeam,
      tournamentName: form.value.tournamentName,
      enabledCameras: form.value.enabledCameras,
      obsHost: form.value.obsHost,
      obsPort: form.value.obsPort,
      obsSceneNames: { ...form.value.obsSceneNames },
      replaySceneName: form.value.replaySceneName,
      replaySourceName: form.value.replaySourceName
    }
  })
  savedMessage.value = 'Configuración guardada correctamente.'
  setTimeout(() => { savedMessage.value = '' }, 3000)
}
</script>

<style scoped>
.config-view {
  min-height: 100vh;
  background-color: #1a1a2e;
  padding-bottom: 80px;
}

.save-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 0.75rem 1rem;
  background: #0f0f23;
  border-top: 1px solid #2a2a4a;
  z-index: 100;
}

.save-feedback {
  margin-top: 0.4rem;
  font-size: 0.85rem;
  color: #48c78e;
  text-align: center;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

.box.has-background-dark {
  border: 1px solid #333;
}

.new-match-box {
  background-color: #1a1500;
  border: 1px solid #665500;
}

.collapsible-header {
  cursor: pointer;
  user-select: none;
}

/* ── Drag & drop cámaras ── */
.cam-drag-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.cam-drag-item {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #2a2a40;
  border: 1px solid #444;
  border-radius: 8px;
  padding: 10px 12px;
  cursor: grab;
  transition: background 0.15s, border-color 0.15s;
  user-select: none;
}

.cam-drag-item:active { cursor: grabbing; }
.cam-drag-item.is-dragging { opacity: 0.4; }
.cam-drag-item.drag-over {
  border-color: #1a6dff;
  background: #1a1a3e;
}

.drag-handle {
  color: #555;
  font-size: 1.2rem;
  cursor: grab;
}

.cam-label {
  font-weight: 700;
  color: #fff;
  flex: 1;
}

.cam-pos {
  font-size: 0.75rem;
  color: #666;
}

.cam-remove {
  background: none;
  border: none;
  color: #ff3860;
  cursor: pointer;
  font-size: 0.85rem;
  padding: 2px 6px;
  border-radius: 4px;
  line-height: 1;
}
.cam-remove:hover { background: rgba(255, 56, 96, 0.15); }

.cam-empty {
  color: #555;
  font-size: 0.85rem;
  text-align: center;
  padding: 12px;
}

.cam-available {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  padding-top: 8px;
  border-top: 1px solid #333;
}

.is-dark-input {
  background-color: #2a2a40;
  border-color: #444;
  color: #eee;
}

.is-dark-input::placeholder {
  color: #666;
}

.is-dark-input:focus {
  border-color: #7a7af0;
  box-shadow: 0 0 0 0.125em rgba(122, 122, 240, 0.25);
}
</style>
