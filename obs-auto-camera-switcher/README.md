# obs-auto-camera-switcher

Servicio Python que usa visión por computadora (YOLOv8n + OpenVINO) para cambiar automáticamente entre cámaras en OBS Studio, seleccionando la cámara con más acción relevante (balón visible o mayor densidad de jugadores).

## Requisitos

- Python 3.10+
- OBS Studio 28+ con WebSocket v5 (puerto 4455)
- obs-controller-api corriendo en puerto 3010

## Instalación

```bash
pip install -r requirements.txt
```

## Primer uso

El modelo YOLOv8n se descarga y exporta a OpenVINO automáticamente en el primer arranque.

```bash
# Iniciar el servicio
python main.py

# O con uvicorn directamente
uvicorn main:app --host 0.0.0.0 --port 3020
```

## API REST (puerto 3020)

| Endpoint | Método | Descripción |
|---|---|---|
| `/health` | GET | Health check |
| `/status` | GET | Estado actual del switcher |
| `/enable` | POST | Activar auto-switching |
| `/disable` | POST | Desactivar |
| `/config` | POST | Actualizar parámetros (JSON body) |
| `/detections` | GET | Última detección por cámara |

## Configuración

Se genera `config.json` en el primer arranque. Parámetros principales:

- `obs_host` / `obs_port` / `obs_password` — conexión a OBS WebSocket
- `capture_fps` — frames por segundo por cámara (default: 2)
- `switch_cooldown_seconds` — mínimo entre cambios (default: 8s)
- `ball_confidence_threshold` — umbral detección balón (default: 0.35)
- `hysteresis_factor` — factor para evitar oscilación (default: 1.2)
- `manual_override_pause_seconds` — pausa tras cambio manual (default: 30s)

## Lógica de decisión

1. **Balón visible** → cámara con balón más centrado
2. **Sin balón, jugadores detectados** → cámara con mayor densidad de jugadores
3. **Empate en densidad (<10% diferencia)** → "Escena Todas"
4. **Sin detecciones** → "Escena Todas"

## Estructura

```
├── main.py                  # FastAPI + loop de detección
├── config.py                # Configuración persistente
├── detector/
│   ├── frame_capture.py     # Screenshots via OBS WebSocket
│   ├── yolo_detector.py     # YOLOv8n OpenVINO
│   └── camera_scorer.py     # Scoring y decisión
├── switcher/
│   ├── obs_client.py        # Control de escenas OBS
│   └── controller_client.py # Notificación a obs-controller-api
└── api/
    └── routes.py            # Endpoints REST
```
