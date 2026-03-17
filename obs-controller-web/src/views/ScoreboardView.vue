<template>
  <div class="scoreboard-wrapper">
    <Transition name="goal-fade">
      <div v-if="goalAnimation.visible" class="goal-overlay">
        <div class="goal-content">
          <div class="goal-text">¡Gool!</div>
          <div class="goal-team">{{ goalAnimation.team === 'HOME' ? state.homeTeam || 'Local' : state.awayTeam || 'Visitante' }}</div>
        </div>
      </div>
    </Transition>

    <div class="scoreboard">
      <!-- Línea de acento superior (estilo UEFA/La Liga) -->
      <div class="accent-bar"></div>

      <div class="scoreboard-body">
        <!-- Equipo local -->
        <div class="team-block home">
          <span class="team-name">{{ state.homeTeam || 'Local' }}</span>
        </div>

        <!-- Marcador central -->
        <div class="score-center">
          <span class="score-num">{{ state.homeScore }}</span>
          <span class="score-divider">–</span>
          <span class="score-num">{{ state.awayScore }}</span>
        </div>

        <!-- Equipo visitante -->
        <div class="team-block away">
          <span class="team-name">{{ state.awayTeam || 'Visitante' }}</span>
        </div>

        <!-- Separador vertical -->
        <div class="v-divider"></div>

        <!-- Tiempo y período -->
        <div class="match-info">
          <span class="time-block">{{ formattedTime }}</span>
          <span class="period-block">{{ state.period }}T</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useWebSocket } from '../composables/useWebSocket.js'

const { state, goalAnimation } = useWebSocket()

const formattedTime = computed(() => {
  const s = state.value.matchTimeSeconds
  const minutes = Math.floor(s / 60)
  const seconds = s % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})
</script>

<style>
/*
  OBS Browser Source — transparent background requirements:
  - "Allow transparency" must be checked in OBS browser source properties
  - html, body AND #app must all be transparent (Vue mounts inside #app)
*/
html,
body,
#app {
  background: transparent !important;
  margin: 0 !important;
  padding: 0 !important;
}
</style>

<style scoped>
.scoreboard-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: transparent;
  pointer-events: none;
}

/* Contenedor principal */
.scoreboard {
  position: fixed;
  top: 16px;
  left: 16px;
  display: inline-flex;
  flex-direction: column;
  background: rgba(10, 10, 10, 0.90);
  border: 2px solid rgba(255, 255, 255, 0.12);
  border-radius: 10px;
  overflow: hidden;
  white-space: nowrap;
  font-family: 'Arial Black', 'Arial', sans-serif;
  font-weight: 900;
  color: #ffffff;
  text-shadow:
    -1px -1px 0 #000,
     1px -1px 0 #000,
    -1px  1px 0 #000,
     1px  1px 0 #000;
  box-shadow:
    0 0 0 1px rgba(255, 107, 0, 0.35),
    0 8px 40px rgba(0, 0, 0, 0.7),
    0 0 50px rgba(255, 107, 0, 0.12);
}

/* Línea de acento superior — gradiente naranja */
.accent-bar {
  height: 5px;
  background: linear-gradient(to right, #ff6b00, #ffaa00, #ff6b00);
  background-size: 200% 100%;
  animation: shimmer 2.5s linear infinite;
  flex-shrink: 0;
}

/* Fila de contenido */
.scoreboard-body {
  display: inline-flex;
  align-items: center;
  gap: 0;
  padding: 10px 20px;
}

/* Bloques de equipo */
.team-block {
  display: flex;
  align-items: center;
}

.team-name {
  font-size: 1.6rem;
  font-weight: 700;
  letter-spacing: 0.03em;
  white-space: nowrap;
}

/* Score central — elemento más prominente */
.score-center {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 18px;
}

.score-num {
  font-size: 2.6rem;
  font-weight: 900;
  min-width: 1.2ch;
  text-align: center;
  line-height: 1;
}

.score-divider {
  font-size: 2rem;
  color: rgba(255, 255, 255, 0.5);
  font-weight: 400;
}

/* Separador vertical entre marcador y tiempo */
.v-divider {
  width: 1px;
  height: 2rem;
  background: rgba(255, 255, 255, 0.25);
  margin: 0 16px;
  flex-shrink: 0;
}

/* Bloque de tiempo */
.match-info {
  display: flex;
  flex-direction: row;
  align-items: baseline;
  gap: 10px;
}

.time-block {
  font-size: 2.2rem;
  font-family: 'Courier New', monospace;
  color: #ffaa00;
  white-space: nowrap;
  text-shadow:
    -1px -1px 0 #000,
     1px -1px 0 #000,
    -1px  1px 0 #000,
     1px  1px 0 #000,
     0 0 14px rgba(255, 150, 0, 0.5);
}

.period-block {
  font-size: 2rem;
  color: #ff9a00;
  font-weight: 700;
  letter-spacing: 0.05em;
  white-space: nowrap;
}

/* ── Animación de gol ── */
.goal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.75);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  pointer-events: none;
}

.goal-content {
  text-align: center;
}

.goal-text {
  font-size: 5rem;
  font-weight: 900;
  color: #ffdd57;
  text-shadow: -3px -3px 0 #000, 3px -3px 0 #000, -3px 3px 0 #000, 3px 3px 0 #000;
  line-height: 1;
}

.goal-team {
  font-size: 2.5rem;
  font-weight: 700;
  color: #fff;
  text-shadow: -2px -2px 0 #000, 2px -2px 0 #000, -2px 2px 0 #000, 2px 2px 0 #000;
  margin-top: 0.5rem;
}

.goal-fade-enter-active,
.goal-fade-leave-active {
  transition: opacity 0.4s ease;
}

.goal-fade-enter-from,
.goal-fade-leave-to {
  opacity: 0;
}

@keyframes shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
