<template>
  <div class="box mt-4" style="background-color: #1a1a2e; border: 1px solid #333;">
    <div class="level is-mobile mb-3">
      <div class="level-left">
        <span class="title is-6 has-text-white">Auto-Switcher IA</span>
      </div>
      <div class="level-right">
        <div class="field">
          <input
            id="auto-switcher-toggle"
            type="checkbox"
            class="switch is-rounded is-info"
            :checked="enabled"
            @change="toggleSwitcher"
          >
          <label for="auto-switcher-toggle" class="has-text-white">
            {{ enabled ? 'Activo' : 'Inactivo' }}
          </label>
        </div>
      </div>
    </div>

    <div v-if="enabled">
      <!-- Status -->
      <div v-if="status" class="mb-3">
        <p v-if="status.manual_override_active" class="has-text-warning is-size-7">
          Pausa manual ({{ status.manual_override_remaining }}s restantes)
        </p>
        <div v-else-if="status.last_decision" class="is-size-7">
          <p class="has-text-success">
            Cámara {{ status.last_decision.target_camera || 'Todas' }}
            <span
              v-if="status.last_decision.reason === 'ball_detected'"
              class="tag is-small is-info ml-2"
            >Balón</span>
            <span
              v-else-if="status.last_decision.reason === 'player_density'"
              class="tag is-small is-warning ml-2"
            >Jugadores</span>
            <span
              v-else-if="status.last_decision.reason === 'density_tie'"
              class="tag is-small is-light ml-2"
            >Empate</span>
            <span
              v-else
              class="tag is-small is-dark ml-2"
            >Sin detección</span>
          </p>
        </div>
      </div>

      <!-- Auto badge on current scene -->
      <p
        v-if="isAutoScene"
        class="is-size-7 has-text-info mb-3"
      >
        <span class="tag is-info is-light">AUTO</span>
        Escena seleccionada por IA
      </p>

      <!-- Cooldown slider -->
      <div class="field mb-2">
        <label class="label is-small has-text-grey-light">
          Cooldown: {{ cooldown }}s
        </label>
        <input
          type="range"
          class="slider is-fullwidth is-small is-info"
          :value="cooldown"
          min="3"
          max="30"
          step="1"
          @change="updateCooldown($event.target.value)"
        >
      </div>

      <!-- Detection scores (debug) -->
      <div v-if="detections && Object.keys(detections).length > 0">
        <p class="is-size-7 has-text-grey-light mb-1">Scores por cámara:</p>
        <div class="columns is-mobile is-multiline is-gapless">
          <div
            v-for="(det, camId) in detections"
            :key="camId"
            class="column is-2-mobile is-size-7 has-text-centered"
          >
            <span class="has-text-grey-light">{{ camId }}</span>
            <progress
              class="progress is-small is-info"
              :value="det.total_score"
              max="200"
              style="height: 4px;"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Connection error -->
    <p v-if="connectionError" class="is-size-7 has-text-danger mt-2">
      {{ connectionError }}
    </p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'

const API_BASE = 'http://localhost:3020'

const props = defineProps({
  state: { type: Object, default: () => ({}) }
})

const enabled = ref(false)
const status = ref(null)
const detections = ref({})
const cooldown = ref(8)
const connectionError = ref(null)
let pollInterval = null

const isAutoScene = computed(() => {
  return props.state?.lastSceneChange?.source === 'auto_switcher'
})

async function fetchStatus() {
  try {
    const res = await fetch(`${API_BASE}/status`)
    if (res.ok) {
      status.value = await res.json()
      enabled.value = status.value.enabled
      connectionError.value = null
    }
  } catch {
    connectionError.value = 'Auto-switcher no disponible'
  }
}

async function fetchDetections() {
  try {
    const res = await fetch(`${API_BASE}/detections`)
    if (res.ok) {
      detections.value = await res.json()
    }
  } catch {
    // silent
  }
}

async function toggleSwitcher(event) {
  const val = event.target.checked
  try {
    await fetch(`${API_BASE}/${val ? 'enable' : 'disable'}`, { method: 'POST' })
    enabled.value = val
    connectionError.value = null
  } catch {
    connectionError.value = 'No se pudo cambiar estado'
    event.target.checked = !val
  }
}

async function updateCooldown(val) {
  cooldown.value = parseInt(val)
  try {
    await fetch(`${API_BASE}/config`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ switch_cooldown_seconds: parseInt(val) })
    })
  } catch {
    // silent
  }
}

onMounted(() => {
  fetchStatus()
  pollInterval = setInterval(() => {
    fetchStatus()
    if (enabled.value) fetchDetections()
  }, 2000)
})

onUnmounted(() => {
  if (pollInterval) clearInterval(pollInterval)
})
</script>
