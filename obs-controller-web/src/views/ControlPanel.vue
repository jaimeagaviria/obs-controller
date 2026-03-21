<template>
  <div class="control-panel">

    <!-- Modal confirmación Parar -->
    <Transition name="modal-fade">
      <div v-if="pendingMatchAction" class="goal-modal-overlay" @click.self="pendingMatchAction = null">
        <div class="goal-modal">
          <div class="goal-modal-icon">{{ pendingMatchAction === 'STOP_MATCH' ? '⏸' : '↺' }}</div>
          <div class="goal-modal-title">{{ pendingMatchAction === 'STOP_MATCH' ? 'Parar el partido' : 'Reiniciar el tiempo' }}</div>
          <div class="goal-modal-actions">
            <button class="button is-dark is-medium" @click="pendingMatchAction = null">Cancelar</button>
            <button
              class="button is-medium"
              :class="pendingMatchAction === 'STOP_MATCH' ? 'is-warning' : 'is-info'"
              @click="confirmMatchAction"
            >Confirmar</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Modal confirmación de gol -->
    <Transition name="modal-fade">
      <div v-if="pendingGoal" class="goal-modal-overlay" @click.self="pendingGoal = null">
        <div class="goal-modal">
          <div class="goal-modal-icon">{{ pendingGoal.startsWith('UNDO') ? '↩' : '⚽' }}</div>
          <div class="goal-modal-title">{{ pendingGoal.startsWith('UNDO') ? 'Anular gol' : 'Marcar gol' }}</div>
          <div class="goal-modal-team">
            {{ pendingGoal.includes('HOME') ? (state.homeTeam || 'Local') : (state.awayTeam || 'Visitante') }}
          </div>
          <div class="goal-modal-actions">
            <button class="button is-dark is-medium" @click="pendingGoal = null">Cancelar</button>
            <button
              class="button is-medium"
              :class="pendingGoal.startsWith('UNDO') ? 'is-warning' : 'is-success'"
              @click="confirmGoal"
            >Confirmar</button>
          </div>
        </div>
      </div>
    </Transition>

    <section class="section py-4">
      <div class="container">

        <!-- OBS Status -->
        <div class="mb-4">
          <span class="tag is-medium" :class="state.obsConnected ? 'is-success' : 'is-danger'">
            OBS: {{ state.obsConnected ? 'Conectado' : 'Desconectado' }}
          </span>
          <span v-if="state.lastObsCommandError" class="ml-2 has-text-warning is-size-7">
            Cmd error: {{ state.lastObsCommandError }}
          </span>
        </div>

        <!-- OBS Disconnected Banner -->
        <div v-if="!state.obsConnected" class="obs-disconnected-banner mb-4">
          <span class="obs-disconnected-icon">⚠️</span>
          <div>
            <div class="obs-disconnected-title">OBS Studio no está disponible</div>
            <div class="obs-disconnected-reason">{{ obsConnectionReason }}</div>
          </div>
        </div>

        <!-- Match Control -->
        <div class="box has-background-dark mb-4">
          <h2 class="title is-5 has-text-white mb-3">Control del Partido</h2>

          <!-- Start/Stop -->
          <div class="buttons is-centered mb-3">
            <button
              class="button is-success"
              :disabled="state.matchRunning || !state.obsConnected"
              @click="send({ type: 'START_MATCH' })"
            >
              ▶ Iniciar
            </button>
            <button
              class="button is-warning"
              :disabled="!state.matchRunning || !state.obsConnected"
              @click="pendingMatchAction = 'STOP_MATCH'"
            >
              ⏸ Parar
            </button>
            <button
              class="button is-info"
              :disabled="!state.obsConnected"
              @click="pendingMatchAction = 'RESET_MATCH'"
            >
              ↺ Reset
            </button>
          </div>

          <!-- Period -->
          <div class="mb-3 has-text-centered">
            <div class="has-text-grey-light is-size-7 mb-1">Tiempo</div>
            <div class="buttons has-addons is-centered">
              <button
                v-for="p in [1, 2, 3]"
                :key="p"
                class="button is-small"
                :class="state.period === p ? 'is-primary' : 'is-dark'"
                :disabled="!state.obsConnected"
                @click="send({ type: 'SET_PERIOD', payload: { period: p } })"
              >
                {{ p }}
              </button>
            </div>
          </div>

          <!-- Timer display -->
          <div class="timer-display has-text-white has-text-centered">
            <span class="is-size-2 has-text-weight-bold">{{ formattedTime }}</span>
            <span class="is-size-2 has-text-weight-bold has-text-grey-light ml-3">T{{ state.period }}</span>
          </div>
        </div>

        <!-- Score -->
        <div class="box has-background-dark mb-4">
          <h2 class="title is-5 has-text-white mb-3">Marcador</h2>
          <div class="columns is-mobile is-vcentered">
            <!-- Home Team -->
            <div class="column has-text-centered">
              <div class="has-text-white has-text-weight-bold is-size-6 mb-2">
                {{ state.homeTeam || 'Local' }}
              </div>
              <div class="score-control">
                <button
                  class="button is-danger"
                  :disabled="!state.obsConnected"
                  @click="requestGoal('UNDO_GOAL_HOME')"
                >−</button>
                <span class="score-number">{{ state.homeScore }}</span>
                <button
                  class="button is-success"
                  :disabled="!state.obsConnected"
                  @click="requestGoal('GOAL_HOME')"
                >+</button>
              </div>
            </div>

            <div class="column is-narrow has-text-centered">
              <span class="has-text-grey is-size-4">vs</span>
            </div>

            <!-- Away Team -->
            <div class="column has-text-centered">
              <div class="has-text-white has-text-weight-bold is-size-6 mb-2">
                {{ state.awayTeam || 'Visitante' }}
              </div>
              <div class="score-control">
                <button
                  class="button is-danger"
                  :disabled="!state.obsConnected"
                  @click="requestGoal('UNDO_GOAL_AWAY')"
                >−</button>
                <span class="score-number">{{ state.awayScore }}</span>
                <button
                  class="button is-success"
                  :disabled="!state.obsConnected"
                  @click="requestGoal('GOAL_AWAY')"
                >+</button>
              </div>
            </div>
          </div>
        </div>

        <!-- Camera & Stream Control -->
        <CameraStreamControl :state="state" :send="send" />

        <!-- Auto-Switcher AI Panel -->
        <AutoSwitcherPanel :state="state" />

      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useWebSocket } from '../composables/useWebSocket.js'
import CameraStreamControl from '../components/CameraStreamControl.vue'
import AutoSwitcherPanel from '../components/AutoSwitcherPanel.vue'
import NoSleep from 'nosleep.js'

const { state, send } = useWebSocket()

// ── Screen Wake Lock — evita que el celular se bloquee ──
// Usa nosleep.js como fallback para HTTP (Wake Lock API requiere HTTPS)
const noSleep = new NoSleep()

function enableNoSleep() {
  noSleep.enable()
}

function onVisibilityChange() {
  if (document.visibilityState === 'visible') noSleep.enable()
  else noSleep.disable()
}

onMounted(() => {
  document.addEventListener('visibilitychange', onVisibilityChange)
  // nosleep.js requiere un gesto del usuario para activarse en algunos navegadores;
  // lo activamos en el primer toque/click de la pantalla
  document.addEventListener('touchstart', enableNoSleep, { once: true })
  document.addEventListener('click', enableNoSleep, { once: true })
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', onVisibilityChange)
  noSleep.disable()
})

const pendingGoal = ref(null)
const pendingMatchAction = ref(null)

function confirmMatchAction() {
  if (pendingMatchAction.value === 'STOP_MATCH') {
    send({ type: 'STOP_MATCH' })
  } else {
    resetAndAdvancePeriod()
  }
  pendingMatchAction.value = null
}

function requestGoal(type) {
  pendingGoal.value = type
}

function confirmGoal() {
  send({ type: pendingGoal.value })
  pendingGoal.value = null
}

function resetAndAdvancePeriod() {
  send({ type: 'RESET_MATCH' })
  const nextPeriod = Math.min((state.value.period ?? 1) + 1, 3)
  send({ type: 'SET_PERIOD', payload: { period: nextPeriod } })
}

const obsConnectionReason = computed(() => {
  const err = state.value.obsConnectionError
  if (!err) return 'Verificando conexión...'
  if (err.includes('ECONNREFUSED')) return 'OBS Studio no está abierto o el puerto WebSocket no está activo.'
  if (err.includes('ETIMEDOUT') || err.includes('timeout')) return 'Tiempo de conexión agotado. Verifica la IP y el puerto en Configuración.'
  if (err.includes('ENOTFOUND') || err.includes('getaddrinfo')) return 'Host no encontrado. Verifica la dirección IP en Configuración.'
  return 'No se puede conectar con OBS. Verifica la configuración.'
})

const formattedTime = computed(() => {
  const s = state.value.matchTimeSeconds
  const minutes = Math.floor(s / 60)
  const seconds = s % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})
</script>

<style scoped>
.control-panel {
  min-height: 100vh;
  background-color: #1a1a2e;
}


.timer-display {
  font-family: 'Courier New', monospace;
}

.score-control {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.score-number {
  font-size: 2rem;
  font-weight: 900;
  color: #ffffff;
  min-width: 2rem;
  text-align: center;
  line-height: 1;
}

.obs-disconnected-banner {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  background: rgba(255, 56, 96, 0.12);
  border: 1px solid rgba(255, 56, 96, 0.4);
  border-radius: 8px;
  padding: 0.85rem 1rem;
  color: #ff6b88;
}

.obs-disconnected-icon {
  font-size: 1.4rem;
  line-height: 1.2;
  flex-shrink: 0;
}

.obs-disconnected-title {
  font-weight: 700;
  font-size: 0.9rem;
  color: #ff6b88;
  margin-bottom: 0.15rem;
}

.obs-disconnected-reason {
  font-size: 0.78rem;
  color: #cc8899;
  line-height: 1.4;
}

.box.has-background-dark {
  border: 1px solid #333;
}

.goal-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(3px);
}

.goal-modal {
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

.goal-modal-icon {
  font-size: 2.8rem;
  line-height: 1;
}

.goal-modal-title {
  font-size: 0.85rem;
  font-weight: 700;
  color: #888;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.goal-modal-team {
  font-size: 1.6rem;
  font-weight: 900;
  color: #ffffff;
  text-align: center;
}

.goal-modal-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 0.5rem;
  width: 100%;
}

.goal-modal-actions .button {
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
