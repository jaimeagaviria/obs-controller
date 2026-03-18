import base64
import logging

import cv2
import numpy as np
import obsws_python as obs

logger = logging.getLogger(__name__)


class FrameCapture:
    """Captures frames from OBS sources via WebSocket screenshot API."""

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
            logger.info("FrameCapture connected to OBS")
            return True
        except Exception as e:
            logger.error(f"FrameCapture OBS connection failed: {e}")
            self._client = None
            return False

    def disconnect(self):
        self._client = None

    def capture_frame(self, scene_name: str, width: int = 640) -> np.ndarray | None:
        if not self._client:
            if not self.connect():
                return None
        try:
            response = self._client.get_source_screenshot(
                source_name=scene_name,
                image_format="jpg",
                image_width=width,
                image_compression_quality=75,
            )
            b64_data = response.image_data
            if "," in b64_data:
                b64_data = b64_data.split(",", 1)[1]
            img_bytes = base64.b64decode(b64_data)
            nparr = np.frombuffer(img_bytes, np.uint8)
            frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
            return frame
        except Exception as e:
            logger.warning(f"Screenshot failed for {scene_name}: {e}")
            self._client = None
            return None

    def capture_all(
        self, cameras: list[int], scene_template: str, width: int = 640
    ) -> dict[int, np.ndarray]:
        frames = {}
        for cam_id in cameras:
            scene_name = scene_template.format(n=cam_id)
            frame = self.capture_frame(scene_name, width)
            if frame is not None:
                frames[cam_id] = frame
        return frames
