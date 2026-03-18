import json
import logging
import threading

import websocket

logger = logging.getLogger(__name__)


class ControllerClient:
    """WebSocket client to obs-controller-api for notifying scene changes."""

    def __init__(self, host: str, port: int):
        self._url = f"ws://{host}:{port}/ws"
        self._ws: websocket.WebSocket | None = None
        self._lock = threading.Lock()

    def connect(self):
        try:
            self._ws = websocket.create_connection(self._url, timeout=5)
            logger.info(f"Connected to obs-controller-api at {self._url}")
        except Exception as e:
            logger.warning(f"Controller WS connection failed: {e}")
            self._ws = None

    def disconnect(self):
        with self._lock:
            if self._ws:
                try:
                    self._ws.close()
                except Exception:
                    pass
                self._ws = None

    def notify_scene_switch(
        self, scene_name: str, reason: str, confidence: float
    ):
        msg = {
            "type": "OBS_CONTROL",
            "payload": {
                "action": "SWITCH_SCENE",
                "sceneName": scene_name,
                "source": "auto_switcher",
                "reason": reason,
                "confidence": round(confidence, 2),
            },
        }
        self._send(msg)

    def _send(self, msg: dict):
        with self._lock:
            if not self._ws:
                self.connect()
            if not self._ws:
                return
            try:
                self._ws.send(json.dumps(msg))
            except Exception as e:
                logger.warning(f"Failed to send to controller: {e}")
                self._ws = None
