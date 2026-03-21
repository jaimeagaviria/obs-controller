<template>
  <div class="dl-page">
    <!-- ── Header ─────────────────────────────────────────────────────── -->
    <div class="dl-header">
      <div class="dl-logo">
        <span class="dl-logo-icon">⚽</span>
        <span class="dl-logo-cam">📷</span>
      </div>
      <h1 class="dl-title">OBS Remote Camera</h1>
      <p class="dl-subtitle">App para camarógrafos · Transmisión en vivo</p>

      <div v-if="version" class="dl-badge">
        <span class="badge-dot"></span>
        v{{ version.version }} &nbsp;·&nbsp; {{ version.date }} &nbsp;·&nbsp; {{ version.size }}
      </div>
      <div v-else-if="loadError" class="dl-badge dl-badge--warn">
        APK no disponible aún
      </div>
    </div>

    <!-- ── Botón de descarga ───────────────────────────────────────────── -->
    <a
      :href="apkUrl"
      class="dl-button"
      :class="{ 'dl-button--disabled': loadError }"
      download="obs-remote-camera.apk"
    >
      <svg class="dl-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
        <polyline points="7 10 12 15 17 10"/>
        <line x1="12" y1="15" x2="12" y2="3"/>
      </svg>
      Descargar APK
    </a>

    <!-- ── QR ─────────────────────────────────────────────────────────── -->
    <div class="dl-qr-wrap">
      <canvas ref="qrCanvas" class="dl-qr"></canvas>
      <p class="dl-qr-hint">Escanea con la cámara de tu Android</p>
    </div>

    <!-- ── Instrucciones ──────────────────────────────────────────────── -->
    <div class="dl-steps">
      <h2 class="dl-steps-title">Cómo instalar</h2>

      <div class="dl-step">
        <div class="step-num">1</div>
        <div class="step-body">
          <strong>Descarga el archivo APK</strong>
          <p>Toca el botón de descarga o escanea el QR. El archivo se guardará en tu carpeta de Descargas.</p>
        </div>
      </div>

      <div class="dl-step">
        <div class="step-num">2</div>
        <div class="step-body">
          <strong>Permite instalar apps externas</strong>
          <p>Cuando aparezca el aviso, toca <em>"Configuración"</em> y activa
          <em>"Permitir de esta fuente"</em>. Esto solo se hace una vez.</p>
          <div class="step-tip">
            Si no aparece el aviso: ve a <strong>Ajustes → Aplicaciones → Instalar apps desconocidas</strong>
            y actívalo para tu navegador o gestor de archivos.
          </div>
        </div>
      </div>

      <div class="dl-step">
        <div class="step-num">3</div>
        <div class="step-body">
          <strong>Instala la app</strong>
          <p>Abre el archivo descargado desde la notificación o desde la carpeta Descargas y toca <em>"Instalar"</em>.</p>
        </div>
      </div>

      <div class="dl-step">
        <div class="step-num">4</div>
        <div class="step-body">
          <strong>¡Listo para transmitir!</strong>
          <p>Abre la app, selecciona tu número de cámara y toca <em>"Transmitir"</em>. El director verá tu señal en el panel de control.</p>
        </div>
      </div>
    </div>

    <!-- ── Footer ─────────────────────────────────────────────────────── -->
    <div class="dl-footer">
      Requiere Android 8.0 o superior &nbsp;·&nbsp; Solo para red autorizada
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import QRCode from 'qrcode'

const qrCanvas = ref(null)
const version  = ref(null)
const loadError = ref(false)

const apkUrl = computed(() => `${window.location.origin}/downloads/obs-remote-camera.apk`)

onMounted(async () => {
  // Cargar version.json
  try {
    const res = await fetch('/downloads/version.json')
    if (res.ok) version.value = await res.json()
    else loadError.value = true
  } catch {
    loadError.value = true
  }

  // Generar QR
  if (qrCanvas.value) {
    await QRCode.toCanvas(qrCanvas.value, apkUrl.value, {
      width: 180,
      margin: 2,
      color: { dark: '#1a1a1a', light: '#f5f5f5' }
    })
  }
})

</script>

<style scoped>
.dl-page {
  min-height: 100vh;
  background: #0d0d12;
  color: #e8eaf0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px 60px;
  gap: 36px;
}

/* ── Header ─────────────────────────────────────────────── */
.dl-header {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.dl-logo {
  font-size: 52px;
  line-height: 1;
  display: flex;
  gap: 4px;
}

.dl-title {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.2px;
  margin: 0;
  color: #fff;
}

.dl-subtitle {
  font-size: 14px;
  color: #5a6070;
  margin: 0;
}

.dl-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #141820;
  border: 1px solid #252a35;
  border-radius: 20px;
  padding: 6px 16px;
  font-size: 12px;
  color: #8090a8;
}

.dl-badge--warn { border-color: #5a3000; color: #c07030; }

.badge-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #4caf50;
  flex-shrink: 0;
}

/* ── Botón descarga ──────────────────────────────────────── */
.dl-button {
  display: flex;
  align-items: center;
  gap: 12px;
  background: linear-gradient(135deg, #1565c0, #1e88e5);
  color: #fff;
  text-decoration: none;
  font-size: 17px;
  font-weight: 600;
  padding: 16px 36px;
  border-radius: 14px;
  box-shadow: 0 6px 24px rgba(25, 118, 210, 0.35);
  transition: transform 0.15s, box-shadow 0.15s;
  letter-spacing: 0.2px;
}

.dl-button:active { transform: scale(0.97); }
.dl-button:hover  { box-shadow: 0 8px 32px rgba(25, 118, 210, 0.5); }
.dl-button--disabled {
  background: #2a2a35;
  box-shadow: none;
  color: #555;
  pointer-events: none;
}

.dl-icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

/* ── QR ──────────────────────────────────────────────────── */
.dl-qr-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.dl-qr {
  border-radius: 12px;
  border: 3px solid #1e2535;
}

.dl-qr-hint {
  font-size: 12px;
  color: #3a4555;
  margin: 0;
}

/* ── Pasos ───────────────────────────────────────────────── */
.dl-steps {
  width: 100%;
  max-width: 540px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dl-steps-title {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 2px;
  text-transform: uppercase;
  color: #3a4555;
  margin: 0 0 12px;
}

.dl-step {
  display: flex;
  gap: 16px;
  background: #111520;
  border: 1px solid #1c2030;
  border-radius: 12px;
  padding: 16px;
}

.step-num {
  width: 30px;
  height: 30px;
  min-width: 30px;
  border-radius: 50%;
  background: #1a2545;
  border: 1px solid #253560;
  color: #5590e0;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 2px;
}

.step-body {
  flex: 1;
  font-size: 14px;
  line-height: 1.55;
  color: #8090a8;
}

.step-body strong {
  display: block;
  color: #c8d4e8;
  font-size: 15px;
  margin-bottom: 4px;
}

.step-body em {
  color: #90b8f0;
  font-style: normal;
  font-weight: 500;
}

.step-tip {
  margin-top: 8px;
  padding: 8px 12px;
  background: #0e1520;
  border-left: 3px solid #253560;
  border-radius: 0 6px 6px 0;
  font-size: 12px;
  color: #6070a0;
  line-height: 1.5;
}


/* ── Footer ──────────────────────────────────────────────── */
.dl-footer {
  font-size: 11px;
  color: #2a3040;
  text-align: center;
}
</style>
