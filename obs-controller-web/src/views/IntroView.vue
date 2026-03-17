<template>
  <div class="intro-wrapper">

    <!-- Partículas de fondo -->
    <div class="particles">
      <span v-for="n in 20" :key="n" class="particle" :style="particleStyle(n)"></span>
    </div>

    <!-- Líneas diagonales decorativas -->
    <div class="deco-lines">
      <div class="deco-line left"></div>
      <div class="deco-line right"></div>
    </div>

    <!-- Contenido principal -->
    <div class="intro-content">

      <!-- Nombre del torneo -->
      <div class="tournament-label">
        <div class="label-line"></div>
        <span class="label-text">{{ state.tournamentName || 'TORNEO' }}</span>
        <div class="label-line"></div>
      </div>

      <!-- Enfrentamiento -->
      <div class="matchup">

        <!-- Equipo local -->
        <div class="team-block home-block">
          <div class="team-name home-name">{{ state.homeTeam || 'LOCAL' }}</div>
          <div class="team-tag">LOCAL</div>
        </div>

        <!-- VS -->
        <div class="vs-block">
          <div class="vs-ring"></div>
          <span class="vs-text">VS</span>
        </div>

        <!-- Equipo visitante -->
        <div class="team-block away-block">
          <div class="team-name away-name">{{ state.awayTeam || 'VISITANTE' }}</div>
          <div class="team-tag">VISITANTE</div>
        </div>

      </div>

      <!-- Mensaje de espera -->
      <div class="waiting-msg">
        <span class="waiting-text">En instantes inicia la transmisión</span>
        <span class="waiting-dots"><span>.</span><span>.</span><span>.</span></span>
      </div>

      <!-- Barra de acento inferior -->
      <div class="accent-bottom">
        <div class="accent-line"></div>
        <span class="live-badge">EN VIVO</span>
        <div class="accent-line"></div>
      </div>

    </div>

  </div>
</template>

<script setup>
import { useWebSocket } from '../composables/useWebSocket.js'

const { state } = useWebSocket()

function particleStyle(n) {
  const size = 2 + (n % 4)
  const left = (n * 47) % 100
  const delay = (n * 0.4) % 6
  const duration = 6 + (n % 5)
  const top = (n * 31) % 100
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    top: `${top}%`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`,
  }
}
</script>

<style>
html, body, #app {
  background: transparent !important;
  margin: 0 !important;
  padding: 0 !important;
}
</style>

<style scoped>
/* ── Wrapper ── */
.intro-wrapper {
  position: fixed;
  inset: 0;
  background: radial-gradient(ellipse at center, #0a0a2e 0%, #03030f 70%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-family: 'Arial Black', 'Arial', sans-serif;
}

/* ── Partículas flotantes ── */
.particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.particle {
  position: absolute;
  border-radius: 50%;
  background: rgba(100, 160, 255, 0.4);
  animation: float linear infinite;
}

@keyframes float {
  0%   { transform: translateY(0)   scale(1);   opacity: 0; }
  10%  { opacity: 1; }
  90%  { opacity: 0.6; }
  100% { transform: translateY(-100vh) scale(0.5); opacity: 0; }
}

/* ── Líneas diagonales ── */
.deco-lines {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.deco-line {
  position: absolute;
  width: 3px;
  height: 200%;
  top: -50%;
  background: linear-gradient(to bottom, transparent, rgba(26, 109, 255, 0.15), transparent);
  animation: line-slide 8s ease-in-out infinite;
}

.deco-line.left  { left: 20%; animation-delay: 0s; }
.deco-line.right { right: 20%; animation-delay: 4s; }

@keyframes line-slide {
  0%, 100% { opacity: 0.3; transform: rotate(-20deg) translateX(0); }
  50%       { opacity: 0.8; transform: rotate(-20deg) translateX(30px); }
}

/* ── Contenido principal ── */
.intro-content {
  position: relative;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 48px;
  width: 100%;
  padding: 0 60px;
  animation: content-in 1s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes content-in {
  from { opacity: 0; transform: translateY(30px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ── Torneo label ── */
.tournament-label {
  display: flex;
  align-items: center;
  gap: 20px;
  animation: fade-in 1.2s ease both;
  animation-delay: 0.3s;
  opacity: 0;
}

.label-line {
  height: 1px;
  width: 80px;
  background: linear-gradient(to right, transparent, rgba(170, 212, 255, 0.6));
}
.tournament-label .label-line:last-child {
  background: linear-gradient(to left, transparent, rgba(170, 212, 255, 0.6));
}

.label-text {
  font-size: 1rem;
  font-weight: 700;
  color: #aad4ff;
  letter-spacing: 0.25em;
  text-transform: uppercase;
  white-space: nowrap;
}

/* ── Matchup ── */
.matchup {
  display: flex;
  align-items: center;
  gap: 60px;
  width: 100%;
  justify-content: center;
}

/* ── Bloques de equipo ── */
.team-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  flex: 1;
  max-width: 420px;
}

.home-block {
  align-items: flex-end;
  animation: slide-from-left 1s cubic-bezier(0.22, 1, 0.36, 1) both;
  animation-delay: 0.5s;
  opacity: 0;
}

.away-block {
  align-items: flex-start;
  animation: slide-from-right 1s cubic-bezier(0.22, 1, 0.36, 1) both;
  animation-delay: 0.5s;
  opacity: 0;
}

.team-name {
  font-size: clamp(1.4rem, 3.5vw, 3.2rem);
  font-weight: 900;
  color: #ffffff;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  white-space: normal;
  word-break: break-word;
  line-height: 1.15;
  text-shadow:
    0 0 40px rgba(26, 109, 255, 0.6),
    -2px -2px 0 #000,
     2px -2px 0 #000,
    -2px  2px 0 #000,
     2px  2px 0 #000;
}

.home-name { text-align: right; }
.away-name { text-align: left; }

.team-tag {
  font-size: 0.75rem;
  font-weight: 700;
  color: rgba(255,255,255,0.35);
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

/* ── VS ── */
.vs-block {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 110px;
  height: 110px;
  animation: vs-in 1s cubic-bezier(0.34, 1.56, 0.64, 1) both;
  animation-delay: 0.8s;
  opacity: 0;
}

.vs-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 2px solid rgba(255, 215, 0, 0.5);
  background: rgba(255, 215, 0, 0.05);
  animation: ring-pulse 2.5s ease-in-out infinite;
  animation-delay: 1.8s;
}

@keyframes ring-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(255, 215, 0, 0.4); }
  50%       { box-shadow: 0 0 0 16px rgba(255, 215, 0, 0); }
}

.vs-text {
  position: relative;
  z-index: 1;
  font-size: 2.4rem;
  font-weight: 900;
  color: #ffd700;
  letter-spacing: 0.05em;
  text-shadow:
    0 0 30px rgba(255, 215, 0, 0.8),
    -1px -1px 0 #000,
     1px -1px 0 #000,
    -1px  1px 0 #000,
     1px  1px 0 #000;
}

/* ── Mensaje de espera ── */
.waiting-msg {
  display: flex;
  align-items: baseline;
  gap: 4px;
  animation: fade-in 1s ease both;
  animation-delay: 1.5s;
  opacity: 0;
}

.waiting-text {
  font-size: 1.1rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.55);
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.waiting-dots span {
  font-size: 1.3rem;
  font-weight: 900;
  color: #1a6dff;
  animation: dot-bounce 1.4s ease-in-out infinite;
}
.waiting-dots span:nth-child(2) { animation-delay: 0.2s; }
.waiting-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes dot-bounce {
  0%, 80%, 100% { opacity: 0.2; transform: translateY(0); }
  40%            { opacity: 1;   transform: translateY(-5px); }
}

/* ── Barra inferior ── */
.accent-bottom {
  display: flex;
  align-items: center;
  gap: 24px;
  width: 100%;
  max-width: 700px;
  animation: fade-in 1s ease both;
  animation-delay: 1.2s;
  opacity: 0;
}

.accent-line {
  flex: 1;
  height: 2px;
  background: linear-gradient(to right, transparent, rgba(26, 109, 255, 0.8), rgba(155, 39, 175, 0.8), transparent);
}

.live-badge {
  font-size: 0.85rem;
  font-weight: 900;
  color: #ff3860;
  letter-spacing: 0.25em;
  text-transform: uppercase;
  padding: 4px 14px;
  border: 1px solid rgba(255, 56, 96, 0.6);
  border-radius: 4px;
  background: rgba(255, 56, 96, 0.1);
  white-space: nowrap;
  animation: badge-blink 1.5s step-start infinite;
  animation-delay: 2s;
}

@keyframes badge-blink {
  0%, 100% { opacity: 1; }
  50%       { opacity: 0.4; }
}

/* ── Animaciones de entrada ── */
@keyframes fade-in {
  from { opacity: 0; }
  to   { opacity: 1; }
}

@keyframes slide-from-left {
  from { opacity: 0; transform: translateX(-80px); }
  to   { opacity: 1; transform: translateX(0); }
}

@keyframes slide-from-right {
  from { opacity: 0; transform: translateX(80px); }
  to   { opacity: 1; transform: translateX(0); }
}

@keyframes vs-in {
  from { opacity: 0; transform: scale(0.3); }
  to   { opacity: 1; transform: scale(1); }
}
</style>
