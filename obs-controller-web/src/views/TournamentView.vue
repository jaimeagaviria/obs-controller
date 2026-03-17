<template>
  <div class="overlay-wrapper">
    <Transition name="sweep">
      <div v-show="visible" class="lower-third">
        <div class="accent-bar"></div>
        <div class="text-block">
          <span class="name">{{ state.tournamentName || 'Torneo' }}</span>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useWebSocket } from '../composables/useWebSocket.js'

const { state } = useWebSocket()

const visible = ref(true)
let timer = null

const SHOW_DURATION  = 10_000  // visible 10 segundos
const HIDE_DURATION  = 15_000  // oculto 15 segundos
const EXIT_DURATION  =   1500  // duración de la animación de salida

function cycle() {
  // Ocultar después de SHOW_DURATION
  timer = setTimeout(() => {
    visible.value = false

    // Esperar que termine la animación de salida + pausa oculto
    timer = setTimeout(() => {
      visible.value = true
      cycle()                  // reiniciar el ciclo
    }, EXIT_DURATION + HIDE_DURATION)
  }, SHOW_DURATION)
}

onMounted(() => cycle())
onUnmounted(() => clearTimeout(timer))
</script>

<style>
html,
body,
#app {
  background: transparent !important;
  margin: 0 !important;
  padding: 0 !important;
}
</style>

<style scoped>
.overlay-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: transparent;
  pointer-events: none;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 0 0 8px 0;
}

.lower-third {
  display: inline-flex;
  align-items: stretch;
  background: rgba(8, 8, 22, 0.92);
  border: 2px solid rgba(255, 255, 255, 0.75);
  border-radius: 8px;
  overflow: hidden;
  white-space: nowrap;
  transform-origin: left center;
}

.accent-bar {
  width: 5px;
  background: linear-gradient(to bottom, #1a6dff, #9b27af);
  flex-shrink: 0;
}

.text-block {
  display: flex;
  align-items: center;
  padding: 10px 22px 10px 16px;
}

.name {
  font-family: 'Arial Black', 'Arial', sans-serif;
  font-size: 1.6rem;
  font-weight: 900;
  color: #ffffff;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  text-shadow:
    -1px -1px 0 #000,
     1px -1px 0 #000,
    -1px  1px 0 #000,
     1px  1px 0 #000;
}

/* ── Entrada: se despliega desde la izquierda ── */
.sweep-enter-active {
  transition:
    transform 0.7s cubic-bezier(0.22, 1, 0.36, 1),
    opacity   0.6s ease;
  transform-origin: left center;
}
.sweep-enter-from {
  transform: scaleX(0);
  opacity: 0;
}
.sweep-enter-to {
  transform: scaleX(1);
  opacity: 1;
}

/* ── Salida: se recoge hacia la izquierda y desaparece ── */
.sweep-leave-active {
  transition:
    transform 0.7s cubic-bezier(0.55, 0, 0.8, 0.45),
    opacity   1.4s ease 0.1s;
  transform-origin: left center;
}
.sweep-leave-from {
  transform: scaleX(1);
  opacity: 1;
}
.sweep-leave-to {
  transform: scaleX(0);
  opacity: 0;
}
</style>
