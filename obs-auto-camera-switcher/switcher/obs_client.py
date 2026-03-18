import logging

import obsws_python as obs

logger = logging.getLogger(__name__)


class ObsClient:
    """OBS WebSocket v5 client for scene switching."""

    def __init__(self, host: str, port: int, password: str):
        self._host = host
        self._port = port
        self._password = password
        self._client: obs.ReqClient | None = None

    def connect(self) -> bool:
        try:
            self._client = obs.ReqClient(
                host=self._host,
                port=self._port,
                password=self._password,
                timeout=3,
            )
            logger.info(f"ObsClient connected to OBS at {self._host}:{self._port}")
            return True
        except Exception as e:
            logger.error(f"ObsClient connection failed: {e}")
            self._client = None
            return False

    def disconnect(self):
        self._client = None

    def get_current_scene(self) -> str | None:
        if not self._client:
            if not self.connect():
                return None
        try:
            response = self._client.get_current_program_scene()
            return response.current_program_scene_name
        except Exception as e:
            logger.warning(f"Failed to get current scene: {e}")
            self._client = None
            return None

    def switch_scene(self, scene_name: str) -> bool:
        if not self._client:
            if not self.connect():
                return False
        try:
            self._client.set_current_program_scene(scene_name)
            logger.info(f"Switched to: {scene_name}")
            return True
        except Exception as e:
            logger.error(f"Failed to switch to {scene_name}: {e}")
            self._client = None
            return False
