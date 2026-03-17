import { ref } from 'vue'

const WS_URL = import.meta.env.VITE_WS_URL || `ws://${window.location.hostname}:3010/ws`

const state = ref({
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
  obsConnected: false,
  obsConnectionError: null,
  lastObsCommandError: null,
  homeScore: 0,
  awayScore: 0,
  period: 1,
  matchTimeSeconds: 0,
  matchRunning: false
})

const goalAnimation = ref({ visible: false, team: '' })

let ws = null
let goalAnimationTimer = null

function send(message) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(message))
  }
}

function connect() {
  ws = new WebSocket(WS_URL)

  ws.onopen = () => {
    console.log('WebSocket connected')
  }

  ws.onmessage = (event) => {
    let msg
    try {
      msg = JSON.parse(event.data)
    } catch (_) {
      return
    }

    if (msg.type === 'STATE_UPDATE') {
      state.value = msg.state
    } else if (msg.type === 'GOAL_ANIMATION') {
      goalAnimation.value = { visible: true, team: msg.team }
      if (goalAnimationTimer) clearTimeout(goalAnimationTimer)
      goalAnimationTimer = setTimeout(() => {
        goalAnimation.value = { visible: false, team: '' }
      }, 4000)
    }
  }

  ws.onclose = () => {
    console.log('WebSocket disconnected, reconnecting in 3s...')
    setTimeout(connect, 3000)
  }

  ws.onerror = (err) => {
    console.error('WebSocket error:', err)
  }
}

connect()

export function useWebSocket() {
  return { state, goalAnimation, send }
}
