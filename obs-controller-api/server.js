import express from 'express';
import http from 'http';
import { WebSocketServer } from 'ws';
import OBSWebSocket from 'obs-websocket-js';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const CONFIG_FILE = path.join(__dirname, 'config.json');

// Campos que se persisten en disco (configuración del operador)
const PERSIST_KEYS = [
  'homeTeam', 'awayTeam', 'tournamentName',
  'enabledCameras', 'obsHost', 'obsPort', 'obsSceneNames',
  'replaySceneName', 'replaySourceName'
];

function loadConfig() {
  try {
    const raw = fs.readFileSync(CONFIG_FILE, 'utf8');
    return JSON.parse(raw);
  } catch (_) {
    return {};
  }
}

function saveConfig(state) {
  const data = {};
  for (const key of PERSIST_KEYS) {
    data[key] = state[key];
  }
  fs.writeFileSync(CONFIG_FILE, JSON.stringify(data, null, 2));
}

const app = express();
app.use(express.json());

const savedConfig = loadConfig();

const state = {
  homeTeam: savedConfig.homeTeam ?? "",
  awayTeam: savedConfig.awayTeam ?? "",
  tournamentName: savedConfig.tournamentName ?? "",
  enabledCameras: savedConfig.enabledCameras ?? [1, 2, 3, 4],
  obsHost: savedConfig.obsHost ?? "localhost",
  obsPort: savedConfig.obsPort ?? 4455,
  obsSceneNames: savedConfig.obsSceneNames ?? {
    "1": "Escena Camara 1",
    "2": "Escena Camara 2",
    "3": "Escena Camara 3",
    "4": "Escena Camara 4",
    "5": "Escena Camara 5",
    "6": "Escena Camara 6",
    "ALL_CAMERAS": "Escena Todas",
    "INTRO": "Escena Intro"
  },
  replaySceneName: savedConfig.replaySceneName ?? "Replay",
  replaySourceName: savedConfig.replaySourceName ?? "ReplayVideo",
  obsConnected: false,
  obsConnectionError: null,
  lastObsCommandError: null,
  obsRecording: false,
  obsStreaming: false,
  obsReplayBuffer: false,
  obsCurrentScene: null,
  homeScore: 0,
  awayScore: 0,
  period: 1,
  matchTimeSeconds: 0,
  matchRunning: false,
  lastSceneChange: null
};

let obs = null;
let reconnectTimer = null;
let goalSceneTimer = null;
let replaySavedHandler = null;
let replayEndHandler = null;
const RECONNECT_INTERVAL = 1000; // reintentar cada 1s (red local)

async function handleGoalReplay() {
  if (!obs || !state.obsConnected) return;

  const returnScene = state.obsCurrentScene;

  // Cancelar cualquier secuencia pendiente de un gol anterior
  clearTimeout(goalSceneTimer);
  if (replaySavedHandler) {
    obs.off('ReplayBufferSaved', replaySavedHandler);
    replaySavedHandler = null;
  }
  if (replayEndHandler) {
    obs.off('MediaInputPlaybackEnded', replayEndHandler);
    replayEndHandler = null;
  }

  const replayScene = state.replaySceneName;
  const replaySource = state.replaySourceName;
  const canUseReplay = !!(replayScene && replaySource);

  console.log('[REPLAY] Goal triggered:', { replayScene, replaySource, canUseReplay, returnScene });

  if (!canUseReplay) {
    // Sin configuración — fallback a ALL_CAMERAS
    const allCamerasScene = state.obsSceneNames['ALL_CAMERAS'];
    if (!allCamerasScene) return;
    goalSceneTimer = setTimeout(async () => {
      try { await obs.call('SetCurrentProgramScene', { sceneName: allCamerasScene }); } catch (_) { return; }
      goalSceneTimer = setTimeout(async () => {
        if (!returnScene || !obs || !state.obsConnected) return;
        try { await obs.call('SetCurrentProgramScene', { sceneName: returnScene }); } catch (_) {}
      }, 8000);
    }, 4000);
    return;
  }

  // Banderas para sincronizar: la escena Replay se activa solo cuando
  // AMBAS condiciones se cumplen (source actualizado + animación terminada)
  let sourceUpdated = false;
  let animationFinished = false;

  async function switchToReplay() {
    if (!sourceUpdated || !animationFinished) return;
    if (!obs || !state.obsConnected) return;

    try { await obs.call('SetCurrentProgramScene', { sceneName: replayScene }); } catch (_) { return; }

    // Escuchar el fin de reproducción del media source
    replayEndHandler = ({ inputName }) => {
      if (inputName !== replaySource) return;
      obs.off('MediaInputPlaybackEnded', replayEndHandler);
      replayEndHandler = null;
      clearTimeout(goalSceneTimer);
      if (returnScene && obs && state.obsConnected) {
        obs.call('SetCurrentProgramScene', { sceneName: returnScene }).catch(() => {});
      }
    };
    obs.on('MediaInputPlaybackEnded', replayEndHandler);

    // Fallback 35s por si MediaInputPlaybackEnded no dispara
    goalSceneTimer = setTimeout(() => {
      if (replayEndHandler) {
        obs.off('MediaInputPlaybackEnded', replayEndHandler);
        replayEndHandler = null;
      }
      if (returnScene && obs && state.obsConnected) {
        obs.call('SetCurrentProgramScene', { sceneName: returnScene }).catch(() => {});
      }
    }, 35000);
  }

  // Paso 1: guardar replay buffer (corre en paralelo con la animación de 4s)
  replaySavedHandler = async ({ savedReplayPath }) => {
    obs.off('ReplayBufferSaved', replaySavedHandler);
    replaySavedHandler = null;
    console.log('[REPLAY] ReplayBufferSaved, path:', savedReplayPath);
    try {
      await obs.call('SetInputSettings', {
        inputName: replaySource,
        inputSettings: { local_file: savedReplayPath }
      });
      console.log('[REPLAY] SetInputSettings OK');
    } catch (err) {
      console.error('[REPLAY] SetInputSettings error:', err.message);
    }
    sourceUpdated = true;
    switchToReplay();
  };
  obs.on('ReplayBufferSaved', replaySavedHandler);

  try {
    await obs.call('SaveReplayBuffer');
    console.log('[REPLAY] SaveReplayBuffer called OK');
  } catch (err) {
    console.error('[REPLAY] SaveReplayBuffer error:', err.message);
    // Replay buffer inactivo o error — fallback a ALL_CAMERAS
    obs.off('ReplayBufferSaved', replaySavedHandler);
    replaySavedHandler = null;
    const allCamerasScene = state.obsSceneNames['ALL_CAMERAS'];
    if (!allCamerasScene) return;
    goalSceneTimer = setTimeout(async () => {
      try { await obs.call('SetCurrentProgramScene', { sceneName: allCamerasScene }); } catch (_) { return; }
      goalSceneTimer = setTimeout(async () => {
        if (!returnScene || !obs || !state.obsConnected) return;
        try { await obs.call('SetCurrentProgramScene', { sceneName: returnScene }); } catch (_) {}
      }, 8000);
    }, 4000);
    return;
  }

  // Paso 2: esperar animación de gol (4s)
  goalSceneTimer = setTimeout(() => {
    animationFinished = true;
    switchToReplay();
  }, 4000);
}

function scheduleReconnect() {
  clearTimeout(reconnectTimer);
  reconnectTimer = setTimeout(() => connectOBS(), RECONNECT_INTERVAL);
}

function broadcastAll(message) {
  const data = JSON.stringify(message);
  wss.clients.forEach((client) => {
    if (client.readyState === 1) {
      client.send(data);
    }
  });
}

async function connectOBS() {
  clearTimeout(reconnectTimer);

  if (obs) {
    try {
      obs.removeAllListeners();
      await obs.disconnect();
    } catch (_) {
      // ignore errors on cleanup
    }
  }

  obs = new OBSWebSocket();

  obs.on('ConnectionOpened', async () => {
    state.obsConnected = true;
    state.obsConnectionError = null;

    try {
      const recordStatus = await obs.call('GetRecordStatus');
      state.obsRecording = recordStatus.outputActive;
    } catch (_) {
      state.obsRecording = false;
    }

    try {
      const streamStatus = await obs.call('GetStreamStatus');
      state.obsStreaming = streamStatus.outputActive;
    } catch (_) {
      state.obsStreaming = false;
    }

    try {
      const sceneStatus = await obs.call('GetCurrentProgramScene');
      state.obsCurrentScene = sceneStatus.sceneName;
    } catch (_) {
      state.obsCurrentScene = null;
    }

    broadcastAll({ type: 'STATE_UPDATE', state });
  });

  obs.on('CurrentProgramSceneChanged', ({ sceneName }) => {
    state.obsCurrentScene = sceneName;
    broadcastAll({ type: 'STATE_UPDATE', state });
  });

  obs.on('RecordStateChanged', ({ outputActive }) => {
    state.obsRecording = outputActive;
    broadcastAll({ type: 'STATE_UPDATE', state });
  });

  obs.on('StreamStateChanged', ({ outputActive }) => {
    state.obsStreaming = outputActive;
    broadcastAll({ type: 'STATE_UPDATE', state });
  });

  obs.on('ReplayBufferStateChanged', ({ outputActive }) => {
    state.obsReplayBuffer = outputActive;
    broadcastAll({ type: 'STATE_UPDATE', state });
  });

  obs.on('ConnectionClosed', (event) => {
    state.obsConnected = false;
    state.obsRecording = false;
    state.obsStreaming = false;
    state.obsCurrentScene = null;
    state.obsConnectionError = event && event.message ? event.message : 'Disconnected';
    broadcastAll({ type: 'STATE_UPDATE', state });
    scheduleReconnect();
  });

  obs.on('ConnectionError', (error) => {
    state.obsConnected = false;
    state.obsRecording = false;
    state.obsStreaming = false;
    state.obsCurrentScene = null;
    state.obsConnectionError = error && error.message ? error.message : 'Connection error';
    broadcastAll({ type: 'STATE_UPDATE', state });
    scheduleReconnect();
  });

  const connectTimeout = new Promise((_, reject) =>
    setTimeout(() => reject(new Error('Connection timeout')), 2000)
  );

  try {
    await Promise.race([
      obs.connect(`ws://${state.obsHost}:${state.obsPort}`),
      connectTimeout
    ]);
    // connect() resuelve DESPUÉS del handshake de identificación — aquí ya es seguro llamar
    try {
      const replayStatus = await obs.call('GetReplayBufferStatus');
      state.obsReplayBuffer = replayStatus.outputActive;
      broadcastAll({ type: 'STATE_UPDATE', state });
    } catch (_) {
      // El replay buffer puede no estar habilitado en OBS — no es un error crítico
    }
  } catch (err) {
    state.obsConnected = false;
    state.obsConnectionError = err && err.message ? err.message : 'Failed to connect';
    broadcastAll({ type: 'STATE_UPDATE', state });
    scheduleReconnect();
  }
}

async function handleObsControl(action, params) {
  if (!obs || !state.obsConnected) {
    state.lastObsCommandError = 'OBS not connected';
    broadcastAll({ type: 'STATE_UPDATE', state });
    return;
  }

  try {
    switch (action) {
      case 'START_STREAM':
        await obs.call('StartStream');
        break;
      case 'STOP_STREAM':
        await obs.call('StopStream');
        break;
      case 'START_RECORDING':
        await obs.call('StartRecord');
        try {
          const replayStatus = await obs.call('GetReplayBufferStatus');
          if (!replayStatus.outputActive) {
            await obs.call('StartReplayBuffer');
          }
        } catch (_) {}
        break;
      case 'STOP_RECORDING':
        await obs.call('StopRecord');
        try {
          const replayStatus = await obs.call('GetReplayBufferStatus');
          if (replayStatus.outputActive) {
            await obs.call('StopReplayBuffer');
          }
        } catch (_) {}
        break;
      case 'SWITCH_SCENE': {
        const key = params && params.scene ? String(params.scene) : null;
        if (!key) {
          state.lastObsCommandError = 'SWITCH_SCENE requires scene param';
          broadcastAll({ type: 'STATE_UPDATE', state });
          return;
        }
        const sceneName = state.obsSceneNames[key];
        if (!sceneName) {
          state.lastObsCommandError = `No scene mapped for key: ${key}`;
          broadcastAll({ type: 'STATE_UPDATE', state });
          return;
        }
        await obs.call('SetCurrentProgramScene', { sceneName });
        break;
      }
      case 'SWITCH_SCENE_ALL_CAMERAS': {
        const sceneName = state.obsSceneNames['ALL_CAMERAS'];
        await obs.call('SetCurrentProgramScene', { sceneName });
        break;
      }
      case 'SWITCH_SCENE_INTRO': {
        const sceneName = state.obsSceneNames['INTRO'];
        await obs.call('SetCurrentProgramScene', { sceneName });
        break;
      }
      default:
        state.lastObsCommandError = `Unknown OBS action: ${action}`;
        broadcastAll({ type: 'STATE_UPDATE', state });
        return;
    }
    state.lastObsCommandError = null;
  } catch (err) {
    state.lastObsCommandError = err && err.message ? err.message : 'OBS command error';
    broadcastAll({ type: 'STATE_UPDATE', state });
  }
}

const server = http.createServer(app);
const wss = new WebSocketServer({ noServer: true });

server.on('upgrade', (request, socket, head) => {
  const url = new URL(request.url, `http://${request.headers.host}`);
  if (url.pathname === '/ws') {
    wss.handleUpgrade(request, socket, head, (ws) => {
      wss.emit('connection', ws, request);
    });
  } else {
    socket.destroy();
  }
});

wss.on('connection', (ws) => {
  ws.send(JSON.stringify({ type: 'STATE_UPDATE', state }));

  ws.on('message', async (raw) => {
    let msg;
    try {
      msg = JSON.parse(raw.toString());
    } catch (_) {
      return;
    }

    const { type, payload } = msg;

    switch (type) {
      case 'SET_MATCH_CONFIG': {
        if (payload.homeTeam !== undefined) state.homeTeam = payload.homeTeam;
        if (payload.awayTeam !== undefined) state.awayTeam = payload.awayTeam;
        if (payload.tournamentName !== undefined) state.tournamentName = payload.tournamentName;
        if (payload.enabledCameras !== undefined) state.enabledCameras = payload.enabledCameras;
        if (payload.obsSceneNames !== undefined) state.obsSceneNames = { ...state.obsSceneNames, ...payload.obsSceneNames };
        if (payload.replaySceneName !== undefined) state.replaySceneName = payload.replaySceneName;
        if (payload.replaySourceName !== undefined) state.replaySourceName = payload.replaySourceName;

        const obsChanged =
          (payload.obsHost !== undefined && payload.obsHost !== state.obsHost) ||
          (payload.obsPort !== undefined && payload.obsPort !== state.obsPort);

        if (payload.obsHost !== undefined) state.obsHost = payload.obsHost;
        if (payload.obsPort !== undefined) state.obsPort = payload.obsPort;

        saveConfig(state);
        broadcastAll({ type: 'STATE_UPDATE', state });

        if (obsChanged) {
          await connectOBS();
        }
        break;
      }

      case 'SET_TEAMS':
        if (payload.homeTeam !== undefined) state.homeTeam = payload.homeTeam;
        if (payload.awayTeam !== undefined) state.awayTeam = payload.awayTeam;
        broadcastAll({ type: 'STATE_UPDATE', state });
        break;

      case 'GOAL_HOME':
        state.homeScore += 1;
        broadcastAll({ type: 'STATE_UPDATE', state });
        broadcastAll({ type: 'GOAL_ANIMATION', team: 'HOME' });
        handleGoalReplay();
        break;

      case 'GOAL_AWAY':
        state.awayScore += 1;
        broadcastAll({ type: 'STATE_UPDATE', state });
        broadcastAll({ type: 'GOAL_ANIMATION', team: 'AWAY' });
        handleGoalReplay();
        break;

      case 'UNDO_GOAL_HOME':
        state.homeScore = Math.max(0, state.homeScore - 1);
        broadcastAll({ type: 'STATE_UPDATE', state });
        break;

      case 'UNDO_GOAL_AWAY':
        state.awayScore = Math.max(0, state.awayScore - 1);
        broadcastAll({ type: 'STATE_UPDATE', state });
        break;

      case 'START_MATCH':
        state.matchRunning = true;
        broadcastAll({ type: 'STATE_UPDATE', state });
        break;

      case 'STOP_MATCH':
        state.matchRunning = false;
        broadcastAll({ type: 'STATE_UPDATE', state });
        break;

      case 'RESET_MATCH':
        state.matchTimeSeconds = 0;
        broadcastAll({ type: 'STATE_UPDATE', state });
        break;

      case 'NEW_MATCH':
        state.homeScore = 0;
        state.awayScore = 0;
        state.period = 1;
        state.matchTimeSeconds = 0;
        state.matchRunning = false;
        broadcastAll({ type: 'STATE_UPDATE', state });
        break;

      case 'SET_PERIOD':
        if (payload && payload.period !== undefined) {
          state.period = payload.period;
          broadcastAll({ type: 'STATE_UPDATE', state });
        }
        break;

      case 'OBS_CONTROL':
        if (payload && payload.action) {
          if (payload.source === 'auto_switcher') {
            // Auto-switcher already changed the scene via OBS directly
            state.lastSceneChange = {
              sceneName: payload.sceneName || null,
              source: 'auto_switcher',
              reason: payload.reason || null,
              confidence: payload.confidence || 0,
              timestamp: Date.now()
            };
            broadcastAll({ type: 'STATE_UPDATE', state });
          } else {
            state.lastSceneChange = {
              sceneName: null,
              source: 'manual',
              reason: null,
              confidence: null,
              timestamp: Date.now()
            };
            await handleObsControl(payload.action, payload);
          }
        }
        break;

      default:
        break;
    }
  });

  ws.on('error', (err) => {
    console.error('WS client error:', err.message);
  });
});

// Match timer
setInterval(() => {
  if (state.matchRunning) {
    state.matchTimeSeconds += 1;
    broadcastAll({ type: 'STATE_UPDATE', state });
  }
}, 1000);

app.get('/health', (_req, res) => {
  res.json({ status: 'ok', obsConnected: state.obsConnected });
});

// Configuración pública consultada por las cámaras remotas al iniciar
app.get('/state', (_req, res) => {
  res.json({ enabledCameras: state.enabledCameras });
});

const PORT = process.env.PORT || 3010;
server.listen(PORT, () => {
  console.log(`OBS Controller API running on port ${PORT}`);
  connectOBS();
});