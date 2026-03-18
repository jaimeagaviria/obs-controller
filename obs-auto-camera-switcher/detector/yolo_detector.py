import logging
import shutil
from pathlib import Path

import numpy as np

logger = logging.getLogger(__name__)


class YoloDetector:
    """YOLOv8n with OpenVINO backend for person and ball detection."""

    PERSON_CLASS = 0
    BALL_CLASS = 32

    def __init__(
        self,
        model_path: str,
        ball_threshold: float = 0.35,
        person_threshold: float = 0.40,
    ):
        self._model = None
        self._model_path = model_path
        self.ball_threshold = ball_threshold
        self.person_threshold = person_threshold

    def load(self):
        from ultralytics import YOLO

        ov_path = Path(self._model_path)
        if not ov_path.exists():
            logger.info("Exporting YOLOv8n to OpenVINO INT8 format...")
            base_model = YOLO("yolov8n.pt")
            base_model.export(format="openvino", dynamic=True, int8=True)
            exported = Path("yolov8n_openvino_model")
            if exported.exists():
                ov_path.parent.mkdir(parents=True, exist_ok=True)
                if ov_path.exists():
                    shutil.rmtree(ov_path)
                shutil.move(str(exported), str(ov_path))

        logger.info(f"Loading OpenVINO model from {ov_path}")
        self._model = YOLO(str(ov_path))
        # Warmup inference
        dummy = np.zeros((640, 640, 3), dtype=np.uint8)
        self._model.predict(dummy, device="cpu", verbose=False)
        logger.info("YOLOv8n OpenVINO model loaded and warmed up")

    def detect(self, frame: np.ndarray) -> dict:
        if self._model is None:
            return {"persons": [], "balls": []}

        results = self._model.predict(
            frame,
            device="cpu",
            classes=[self.PERSON_CLASS, self.BALL_CLASS],
            conf=min(self.ball_threshold, self.person_threshold),
            verbose=False,
        )

        persons = []
        balls = []
        h, w = frame.shape[:2]

        for result in results:
            for box in result.boxes:
                cls_id = int(box.cls[0])
                conf = float(box.conf[0])
                x1, y1, x2, y2 = box.xyxy[0].tolist()

                detection = {
                    "confidence": round(conf, 3),
                    "bbox": [round(v, 1) for v in [x1, y1, x2, y2]],
                    "center_x": round((x1 + x2) / 2 / w, 3),
                    "center_y": round((y1 + y2) / 2 / h, 3),
                    "area_pct": round((x2 - x1) * (y2 - y1) / (w * h) * 100, 2),
                }

                if cls_id == self.PERSON_CLASS and conf >= self.person_threshold:
                    persons.append(detection)
                elif cls_id == self.BALL_CLASS and conf >= self.ball_threshold:
                    balls.append(detection)

        return {"persons": persons, "balls": balls}
