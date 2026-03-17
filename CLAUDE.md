# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OBS Studio Football Scoreboard Controller — a local web app for live football broadcast production. An operator uses a mobile-friendly control panel to manage OBS scenes, scores, match timer, and scoreboard overlay across synchronized browser sessions.

Full specification: `obs-controller-requirements.md` (738 lines, authoritative).

## Repository Structure

```
obs-controller/
├── obs-controller-web/     # Vue 3 + Vite frontend (port 5180)
├── obs-controller-api/     # Node.js + Express backend (port 3010)
├── docker-compose.yml      # (to be created)
└── obs-controller-requirements.md
```

## Development Commands

### Frontend (`obs-controller-web/`)
```bash
npm install
npm run dev       # Vite dev server on port 5180
npm run build     # Output to dist/
npm run lint
```

### Backend (`obs-controller-api/`)
```bash
npm install
npm run dev       # nodemon auto-reload
npm start         # Production
npm run lint
npm test
```

### Docker
```bash
docker-compose up --build   # Build and start all services
docker-compose down
docker-compose logs -f
```

## Architecture

### Communication Flow
```
Frontend (Vue 3)
  ↕ WebSocket ws://localhost:3010/ws
Backend (Node.js/Express)
  ↕ obs-websocket 5.x protocol (port 4455)
OBS Studio
```

### Backend: Central State (single source of truth in memory)
State object holds: `homeTeam`, `awayTeam`, `tournamentName`, `homeScore`, `awayScore`, `matchTimeSeconds`, `matchRunning`, `period`, `enabledCameras[]`, OBS connection settings, OBS scene name mappings, `obsConnected`, `obsConnectionError`, `lastCommandError`.

On every connect and state change, backend broadcasts `STATE_UPDATE` (full state) to all WebSocket clients. Timer increments server-side every second while `matchRunning === true`.

### Frontend: 4 Views
- `/` — Control Panel (mobile-first: match controls, scores, cameras, stream/recording)
- `/config` — Configuration (teams, tournament, cameras, OBS host/port, scene name mappings)
- `/scoreboard` — Scoreboard Overlay (ESPN-style, no controls, for OBS browser source)
- `/tournament` — Tournament name display (for OBS browser source)

### WebSocket Protocol

**Frontend → Backend:**
- `SET_MATCH_CONFIG` — full config (teams, tournament, cameras, OBS settings)
- `GOAL_HOME` / `GOAL_AWAY` / `UNDO_GOAL_HOME` / `UNDO_GOAL_AWAY`
- `START_MATCH` / `STOP_MATCH` / `RESET_MATCH`
- `OBS_CONTROL` — `{ action: "START_STREAM" | "STOP_STREAM" | "START_RECORDING" | "STOP_RECORDING" | "SWITCH_SCENE", camera?: number | "ALL_CAMERAS" }`

**Backend → Frontend:**
- `STATE_UPDATE` — full state object
- `GOAL_ANIMATION` — trigger goal animation with team info

### OBS Integration (obs-websocket 5.x)
- Use `SetCurrentProgramScene` with `{ sceneName }` — not hotkeys
- Every request needs a `requestId` (UUID); verify `requestStatus.result === true` in response
- Scene names are centrally configured (not hardcoded); default names: `"Escena Camara 1"` through `"Escena Camara 6"`, `"Escena Todas"`, `"Escena Intro"`
- Logical scene keys: `"1"`–`"6"` (cameras), `"ALL_CAMERAS"`, `"INTRO"`
- OBS host/port come from config state (never hardcoded)

### Docker Deployment
- Frontend: build Vue app with `npm run build`, serve `dist/` with nginx on port 5180
- Backend: run Node.js server on port 3010
- Both services on a single Docker network (e.g., `obs-controller-net`)

## Key Implementation Constraints
- No Vite/Vue default template content anywhere
- All OBS scene names administered from a single place (Config page → backend state)
- Scoreboard: stroke/outline text, high contrast (4.5:1), semi-transparent backdrop — no text truncation
- Camera buttons on Control Panel show only `enabledCameras` entries
- OBS camera/stream controls must be a standalone reusable component