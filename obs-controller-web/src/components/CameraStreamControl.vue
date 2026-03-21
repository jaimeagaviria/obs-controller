<template>
  <div class="camera-stream-control">

    <!-- Modal confirmación detener grabación/stream -->
    <Transition name="modal-fade">
      <div v-if="pendingStop" class="stop-modal-overlay" @click.self="pendingStop = null">
        <div class="stop-modal">
          <div class="stop-modal-icon">{{ pendingStop === 'STOP_RECORDING' ? '⏹' : '📡' }}</div>
          <div class="stop-modal-title">¿Detener {{ pendingStop === 'STOP_RECORDING' ? 'grabación' : 'stream' }}?</div>
          <div class="stop-modal-actions">
            <button class="button is-dark is-medium" @click="pendingStop = null">Cancelar</button>
            <button class="button is-danger is-medium" @click="confirmStop">Detener</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Camera Grid -->
    <div class="box has-background-dark mb-4">
      <div class="is-flex is-align-items-center mb-3" style="gap: 0.5rem;">
        <h2 class="title is-5 has-text-white mb-0">Cámaras</h2>
        <span v-if="props.state.obsCurrentScene" class="tag is-dark is-size-7">
          {{ props.state.obsCurrentScene }}
        </span>
        <button
          class="button is-dark is-small ml-auto"
          style="border-color: #555"
          title="Restablecer distribución"
          @click="resetLayout"
        >↺</button>
      </div>

      <div class="camera-grid">
        <div
          v-for="(key, index) in gridLayout"
          :key="index"
          class="grid-cell"
          :class="{
            'drag-over': dragOverIndex === index && dragSrcIndex !== index,
            'is-dragging-src': dragSrcIndex === index
          }"
          :data-cell-index="index"
          @dragover.prevent="dragOverIndex = index"
          @dragleave="onDragLeave($event)"
          @drop.prevent="onDrop(index)"
        >
          <button
            v-if="key"
            class="button grid-btn"
            :class="buttonClass(key)"
            :disabled="!props.state.obsConnected"
            draggable="true"
            @dragstart="onDragStart(index)"
            @dragend="onDragEnd"
            @click="activateScene(key)"
            @touchstart.passive="onTouchStart($event, index)"
          >
            <span class="btn-inner">
              <span class="btn-label">{{ buttonLabel(key) }}</span>
              <span v-if="activeSceneKey === key" class="live-badge">● AL AIRE</span>
            </span>
          </button>
          <div v-else class="empty-cell"></div>
        </div>
      </div>
    </div>

    <!-- Recording & Streaming -->
    <div class="box has-background-dark">
      <div class="is-flex is-align-items-center mb-3" style="gap: 0.5rem; flex-wrap: wrap;">
        <h2 class="title is-5 has-text-white mb-0">Transmisión y Grabación</h2>
        <span
          class="tag is-size-7"
          :class="props.state.obsReplayBuffer ? 'is-warning' : 'is-dark'"
          :title="props.state.obsReplayBuffer ? 'Replay Buffer activo' : 'Replay Buffer inactivo'"
        >⏺ REPLAY</span>
      </div>
      <div class="columns is-mobile">

        <!-- Recording -->
        <div class="column">
          <div class="status-label mb-2">
            <span class="has-text-grey-light is-size-7">Grabación</span>
            <span v-if="props.state.obsRecording" class="rec-indicator ml-2">● REC</span>
          </div>
          <button
            v-if="!props.state.obsRecording"
            class="button is-success is-fullwidth"
            :disabled="!props.state.obsConnected"
            @click="obsControl('START_RECORDING')"
          >
            ● Grabar
          </button>
          <button
            v-else
            class="button is-danger is-fullwidth"
            @click="pendingStop = 'STOP_RECORDING'"
          >
            ■ Detener Grab.
          </button>
        </div>

        <!-- Streaming -->
        <div class="column">
          <div class="status-label mb-2">
            <span class="has-text-grey-light is-size-7">Streaming</span>
            <span v-if="props.state.obsStreaming" class="live-indicator ml-2">● LIVE</span>
          </div>
          <button
            v-if="!props.state.obsStreaming"
            class="button is-success is-fullwidth"
            :disabled="!props.state.obsConnected"
            @click="obsControl('START_STREAM')"
          >
            ▶ Iniciar Stream
          </button>
          <button
            v-else
            class="button is-danger is-fullwidth"
            @click="pendingStop = 'STOP_STREAM'"
          >
            ■ Detener Stream
          </button>
        </div>

      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'

const pendingStop = ref(null)

function confirmStop() {
  obsControl(pendingStop.value)
  pendingStop.value = null
}

const props = defineProps({
  state: {
    type: Object,
    required: true
  },
  send: {
    type: Function,
    required: true
  }
})

// ── Grid layout ──────────────────────────────────────────────────────────────
const STORAGE_KEY = 'obs-camera-grid-layout'
const GRID_SIZE = 16 // 4 rows × 4 cols

const gridLayout = ref(new Array(GRID_SIZE).fill(null))

function buildDefaultLayout(cameras) {
  const grid = new Array(GRID_SIZE).fill(null)
  cameras.forEach((cam, i) => {
    if (i < 8) grid[i] = String(cam)
  })
  grid[12] = 'ALL_CAMERAS'
  grid[13] = 'INTRO'
  return grid
}

function loadLayout(cameras) {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved) {
    try {
      const parsed = JSON.parse(saved)
      if (Array.isArray(parsed) && parsed.length === GRID_SIZE) {
        const camKeys = cameras.map(String)
        const allKeys = [...camKeys, 'ALL_CAMERAS', 'INTRO']
        // Remove keys no longer valid
        const cleaned = parsed.map(k => {
          if (k === null) return null
          return allKeys.includes(k) ? k : null
        })
        // Add any missing keys to first empty slot
        const inGrid = new Set(cleaned.filter(Boolean))
        const missing = allKeys.filter(k => !inGrid.has(k))
        for (const key of missing) {
          const emptyIdx = cleaned.indexOf(null)
          if (emptyIdx !== -1) cleaned[emptyIdx] = key
        }
        gridLayout.value = cleaned
        return
      }
    } catch { /* ignore invalid saved data */ }
  }
  gridLayout.value = buildDefaultLayout(cameras)
}

function saveLayout() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(gridLayout.value))
}

function resetLayout() {
  gridLayout.value = buildDefaultLayout(props.state.enabledCameras)
  saveLayout()
}

function swapCells(src, dst) {
  const arr = [...gridLayout.value]
  ;[arr[src], arr[dst]] = [arr[dst], arr[src]]
  gridLayout.value = arr
  saveLayout()
}

watch(
  () => props.state.enabledCameras,
  (cameras) => loadLayout(cameras)
)

// ── HTML5 drag-and-drop (desktop) ────────────────────────────────────────────
const dragSrcIndex = ref(null)
const dragOverIndex = ref(null)

function onDragStart(index) {
  dragSrcIndex.value = index
}

function onDragEnd() {
  dragSrcIndex.value = null
  dragOverIndex.value = null
}

function onDragLeave(e) {
  // Only clear if leaving the grid entirely (not entering a child)
  if (!e.currentTarget.contains(e.relatedTarget)) {
    dragOverIndex.value = null
  }
}

function onDrop(index) {
  if (dragSrcIndex.value !== null && dragSrcIndex.value !== index) {
    swapCells(dragSrcIndex.value, index)
  }
  dragSrcIndex.value = null
  dragOverIndex.value = null
}

// ── Touch drag (mobile) ──────────────────────────────────────────────────────
let touchSrc = null
let touchTarget = null

function onTouchStart(e, index) {
  touchSrc = index
  touchTarget = index
}

function handleTouchMove(e) {
  if (touchSrc === null) return
  e.preventDefault()
  const touch = e.touches[0]
  const el = document.elementFromPoint(touch.clientX, touch.clientY)
  if (!el) return
  const cell = el.closest('[data-cell-index]')
  if (cell) {
    const idx = parseInt(cell.dataset.cellIndex)
    if (!isNaN(idx)) {
      dragOverIndex.value = idx
      touchTarget = idx
    }
  }
}

function handleTouchEnd() {
  if (touchSrc !== null && touchTarget !== null && touchSrc !== touchTarget) {
    swapCells(touchSrc, touchTarget)
  }
  touchSrc = null
  touchTarget = null
  dragOverIndex.value = null
}

onMounted(() => {
  loadLayout(props.state.enabledCameras)
  document.addEventListener('touchmove', handleTouchMove, { passive: false })
  document.addEventListener('touchend', handleTouchEnd)
})

onUnmounted(() => {
  document.removeEventListener('touchmove', handleTouchMove)
  document.removeEventListener('touchend', handleTouchEnd)
})

// ── Scene activation ─────────────────────────────────────────────────────────
const activeSceneKey = computed(() => {
  const current = props.state.obsCurrentScene
  const names = props.state.obsSceneNames
  if (!current || !names) return null
  const entry = Object.entries(names).find(([, name]) => name === current)
  return entry ? entry[0] : null
})

function activateScene(key) {
  if (key === 'ALL_CAMERAS') {
    obsControl('SWITCH_SCENE_ALL_CAMERAS')
  } else {
    obsControl('SWITCH_SCENE', { scene: key })
  }
}

function buttonLabel(key) {
  if (key === 'ALL_CAMERAS') return 'Todas'
  if (key === 'INTRO') return 'Intro'
  return `Cam ${key}`
}

function buttonClass(key) {
  const isActive = activeSceneKey.value === key
  if (isActive) return 'is-live-scene'
  if (key === 'ALL_CAMERAS') return 'is-link is-outlined'
  if (key === 'INTRO')       return 'is-primary is-outlined'
  return 'is-info is-outlined'
}

// ── OBS control ───────────────────────────────────────────────────────────────
function obsControl(action, extra = {}) {
  props.send({
    type: 'OBS_CONTROL',
    payload: { action, ...extra }
  })
}
</script>

<style scoped>
.box.has-background-dark {
  border: 1px solid #333;
}

/* ── Camera grid ── */
.camera-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(4, auto);
  gap: 0.4rem;
}

.grid-cell {
  position: relative;
  min-height: 3rem;
  border-radius: 6px;
  transition: background 0.15s;
}

.grid-cell.drag-over {
  background: rgba(255, 255, 255, 0.08);
  outline: 2px dashed rgba(255, 255, 255, 0.35);
  outline-offset: -2px;
}

.grid-cell.is-dragging-src {
  opacity: 0.4;
}

.grid-btn {
  width: 100%;
  height: 100%;
  min-height: 3rem;
  font-weight: 600;
  font-size: 0.85rem;
  cursor: grab;
  touch-action: none;
  user-select: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.2rem;
}

.grid-btn:active {
  cursor: grabbing;
}

.empty-cell {
  width: 100%;
  height: 100%;
  min-height: 3rem;
  border-radius: 6px;
  border: 1px dashed rgba(255, 255, 255, 0.08);
}

/* ── Status indicators ── */
.status-label {
  display: flex;
  align-items: center;
  min-height: 1.4em;
}

.rec-indicator {
  font-size: 0.7rem;
  font-weight: 700;
  color: #ff3860;
  animation: blink 1s step-start infinite;
}

.live-indicator {
  font-size: 0.7rem;
  font-weight: 700;
  color: #ff3860;
  animation: blink 1s step-start infinite;
}

/* ── Escena activa — naranja animado ── */
.is-live-scene {
  background: linear-gradient(90deg, #ff6b00 0%, #ffaa00 50%, #ff6b00 100%);
  background-size: 200% 100%;
  border-color: transparent;
  color: #000;
  font-weight: 900;
  animation: scene-shimmer 1.8s linear infinite, scene-glow 2s ease-in-out infinite;
}

.is-live-scene:hover {
  color: #000;
}

.btn-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  line-height: 1.15;
  pointer-events: none;
}

.btn-label {
  font-size: 0.85rem;
}

.live-badge {
  font-size: 0.52rem;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(0, 0, 0, 0.75);
}

@keyframes scene-shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@keyframes scene-glow {
  0%, 100% {
    box-shadow: 0 0 0 2px #ff6b00, 0 0 10px rgba(255, 107, 0, 0.5);
  }
  50% {
    box-shadow: 0 0 0 2px #ffcc00, 0 0 22px rgba(255, 180, 0, 0.85);
  }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* ── Stop confirmation modal ── */
.stop-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(3px);
}

.stop-modal {
  background: #1a1a2e;
  border: 1px solid #444;
  border-radius: 16px;
  padding: 2rem 2.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  min-width: 260px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.6);
}

.stop-modal-icon {
  font-size: 2.8rem;
  line-height: 1;
}

.stop-modal-title {
  font-size: 1.2rem;
  font-weight: 700;
  color: #ffffff;
  text-align: center;
}

.stop-modal-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 0.5rem;
  width: 100%;
}

.stop-modal-actions .button {
  flex: 1;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s ease;
}
.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
</style>
