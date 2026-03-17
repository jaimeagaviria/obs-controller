<template>
  <div class="replay-wrapper">
    <div class="replay-bar">
      <div class="accent-bar"></div>
      <div class="replay-body">
        <div class="chevrons" aria-hidden="true">
          <span class="ch ch1">&rsaquo;</span>
          <span class="ch ch2">&rsaquo;</span>
          <span class="ch ch3">&rsaquo;</span>
        </div>
        <div class="replay-label">
          <span class="replay-pre">EN</span>
          <span class="replay-main">REPETICIÓN</span>
        </div>
        <div class="replay-badge">REPLAY</div>
      </div>
    </div>
  </div>
</template>

<script setup>
// Overlay estático — sin estado, solo gráfico para browser source de OBS
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
.replay-wrapper {
  position: fixed;
  inset: 0;
  background: transparent;
  pointer-events: none;
  display: flex;
  align-items: flex-end;
  justify-content: flex-start;
  padding: 0 0 48px 48px;
}

/* ── Barra principal ── */
.replay-bar {
  display: inline-flex;
  flex-direction: column;
  background: rgba(10, 10, 10, 0.90);
  border: 2px solid rgba(255, 255, 255, 0.12);
  border-radius: 10px;
  overflow: hidden;
  font-family: 'Arial Black', 'Arial', Impact, sans-serif;
  font-weight: 900;
  white-space: nowrap;
  box-shadow:
    0 0 0 1px rgba(255, 107, 0, 0.4),
    0 8px 40px rgba(0, 0, 0, 0.7),
    0 0 60px rgba(255, 107, 0, 0.15);
  animation: bar-in 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
}

/* ── Barra de acento superior — gradiente naranja ── */
.accent-bar {
  height: 5px;
  background: linear-gradient(to right, #ff6b00, #ffaa00, #ff6b00);
  background-size: 200% 100%;
  animation: shimmer 2.5s linear infinite;
  flex-shrink: 0;
}

/* ── Fila de contenido ── */
.replay-body {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 24px 14px 20px;
}

/* ── Chevrons animados ── */
.chevrons {
  display: flex;
  gap: 2px;
  align-items: center;
}

.ch {
  font-size: 2.8rem;
  line-height: 1;
  color: #ff6b00;
  text-shadow:
    0 0 12px rgba(255, 107, 0, 0.8),
    0 0 24px rgba(255, 107, 0, 0.4);
  opacity: 0;
  animation: chevron-pulse 1.2s ease-in-out infinite;
}

.ch1 { animation-delay: 0s;    font-size: 1.9rem; color: rgba(255, 107, 0, 0.45); }
.ch2 { animation-delay: 0.18s; font-size: 2.4rem; color: rgba(255, 107, 0, 0.72); }
.ch3 { animation-delay: 0.36s; font-size: 2.9rem; }

/* ── Texto ── */
.replay-label {
  display: flex;
  flex-direction: column;
  line-height: 1;
  gap: 2px;
}

.replay-pre {
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.35em;
  color: #ff9a00;
  text-transform: uppercase;
}

.replay-main {
  font-size: 2.4rem;
  font-weight: 900;
  letter-spacing: 0.06em;
  color: #ffffff;
  text-shadow:
    -2px -2px 0 #000,
     2px -2px 0 #000,
    -2px  2px 0 #000,
     2px  2px 0 #000,
     0 0 20px rgba(255, 107, 0, 0.5);
}

/* ── Badge lateral ── */
.replay-badge {
  font-size: 0.65rem;
  font-weight: 900;
  letter-spacing: 0.2em;
  color: #000;
  background: linear-gradient(135deg, #ff9a00, #ff6b00);
  border-radius: 4px;
  padding: 4px 8px;
  align-self: center;
  text-transform: uppercase;
  box-shadow: 0 2px 8px rgba(255, 107, 0, 0.5);
  animation: badge-pulse 2s ease-in-out infinite;
}

/* ── Keyframes ── */
@keyframes bar-in {
  from {
    opacity: 0;
    transform: translateX(-30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@keyframes chevron-pulse {
  0%   { opacity: 0;    transform: translateX(-6px); }
  40%  { opacity: 1;    transform: translateX(0); }
  70%  { opacity: 1;    transform: translateX(0); }
  100% { opacity: 0.15; transform: translateX(4px); }
}

@keyframes badge-pulse {
  0%, 100% { box-shadow: 0 2px 8px rgba(255, 107, 0, 0.5); }
  50%       { box-shadow: 0 2px 18px rgba(255, 107, 0, 0.9); }
}
</style>
