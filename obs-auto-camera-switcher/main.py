import logging
import threading
import time
from contextlib import asynccontextmanager
from dataclasses import asdict

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from api import routes
from config import AppConfig
from detector.camera_scorer import CameraScorer, SwitchDecision
from detector.frame_capture import FrameCapture
from detector.yolo_detector import YoloDetector
from switcher.controller_client import ControllerClient
from switcher.obs_client import ObsClient

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(name)s] %(levelname)s: %(message)s",
)
logger = logging.getLogger("auto-switcher")


class SwitcherEngine:
    """Main detection and switching loop running in a background thread."""

    def __init__(self, config: AppConfig):
        self.config = config
        self.frame_capture = FrameCapture(
            config.obs_host, config.obs_port, config.obs_password
        )
        self.detector = YoloDetector(
            config.model_path,
            config.ball_confidence_threshold,
            config.person_confidence_threshold,
        )
        self.scorer = CameraScorer(
            config.scene_name_template,
            config.scene_name_all_cameras,
            config.density_tie_threshold_pct,
            config.hysteresis_factor,
        )
        self.obs_client = ObsClient(
            config.obs_host, config.obs_port, config.obs_password
        )
        self.controller_client = ControllerClient(
            config.controller_api_host, config.controller_api_port
        )

        self._running = False
        self._thread: threading.Thread | None = None
        self._last_switch_time: float = 0
        self._last_auto_scene: str | None = None
        self._manual_override_until: float = 0
        self._current_scene: str | None = None
        self._last_decision: SwitchDecision | None = None
        self._last_detections: dict = {}
        self._lock = threading.Lock()

    def start(self):
        logger.info("Loading YOLO model...")
        self.detector.load()
        logger.info("Starting detection loop")
        self._running = True
        self._thread = threading.Thread(target=self._run, daemon=True)
        self._thread.start()

    def stop(self):
        self._running = False
        if self._thread:
            self._thread.join(timeout=10)
        self.frame_capture.disconnect()
        self.obs_client.disconnect()
        self.controller_client.disconnect()

    def apply_config(self, config: AppConfig):
        """Apply runtime config changes."""
        self.config = config
        self.detector.ball_threshold = config.ball_confidence_threshold
        self.detector.person_threshold = config.person_confidence_threshold
        self.scorer.density_tie_pct = config.density_tie_threshold_pct
        self.scorer.hysteresis = config.hysteresis_factor

    def get_status(self) -> dict:
        with self._lock:
            decision = self._last_decision
        return {
            "enabled": self.config.enabled,
            "running": self._running,
            "current_scene": self._current_scene,
            "last_switch_time": self._last_switch_time,
            "manual_override_active": time.time() < self._manual_override_until,
            "manual_override_remaining": max(
                0, int(self._manual_override_until - time.time())
            ),
            "last_decision": {
                "target_scene": decision.target_scene,
                "target_camera": decision.target_camera,
                "reason": decision.reason,
                "confidence": decision.confidence,
                "should_switch": decision.should_switch,
            }
            if decision
            else None,
        }

    def get_last_detections(self) -> dict:
        with self._lock:
            detections = self._last_detections.copy()
            decision = self._last_decision
        result = {}
        for cam_id, dets in detections.items():
            score = decision.scores.get(cam_id) if decision else None
            result[str(cam_id)] = {
                "persons_count": len(dets.get("persons", [])),
                "balls_count": len(dets.get("balls", [])),
                "ball_score": score.ball_score if score else 0,
                "density_score": score.density_score if score else 0,
                "total_score": score.total_score if score else 0,
            }
        return result

    def _run(self):
        while self._running:
            interval = 1.0 / max(1, self.config.capture_fps)

            if not self.config.enabled:
                time.sleep(1)
                continue

            try:
                self._tick()
            except Exception as e:
                logger.error(f"Detection loop error: {e}", exc_info=True)

            time.sleep(interval)

    def _tick(self):
        # Get current scene from OBS
        current = self.obs_client.get_current_scene()
        if current is None:
            return
        self._current_scene = current

        # Detect manual override: scene changed externally
        if (
            self._last_auto_scene is not None
            and current != self._last_auto_scene
        ):
            logger.info(
                f"Manual override detected: {self._last_auto_scene} -> {current}"
            )
            self._manual_override_until = (
                time.time() + self.config.manual_override_pause_seconds
            )
            self._last_auto_scene = current

        # Skip if manual override is active
        if time.time() < self._manual_override_until:
            return

        # Capture frames from all active cameras
        frames = self.frame_capture.capture_all(
            self.config.active_cameras, self.config.scene_name_template
        )
        if not frames:
            return

        # Run detection on each frame
        all_detections = {}
        for cam_id, frame in frames.items():
            all_detections[cam_id] = self.detector.detect(frame)

        with self._lock:
            self._last_detections = all_detections

        # Score and decide
        decision = self.scorer.decide(all_detections, current)

        with self._lock:
            self._last_decision = decision

        if not decision.should_switch:
            return

        # Check cooldown (except for switching to "Todas" which is immediate)
        now = time.time()
        if decision.target_scene != self.config.scene_name_all_cameras:
            elapsed = now - self._last_switch_time
            if elapsed < self.config.switch_cooldown_seconds:
                return

        # Execute switch
        if self.obs_client.switch_scene(decision.target_scene):
            self._last_switch_time = now
            self._last_auto_scene = decision.target_scene

            # Notify obs-controller-api
            self.controller_client.notify_scene_switch(
                decision.target_scene, decision.reason, decision.confidence
            )

            logger.info(
                f"Auto-switched to {decision.target_scene} "
                f"(reason={decision.reason}, conf={decision.confidence:.2f})"
            )


# --- FastAPI app ---

config = AppConfig.load()
engine = SwitcherEngine(config)


@asynccontextmanager
async def lifespan(app: FastAPI):
    routes.init(engine, config)
    engine.start()
    yield
    engine.stop()


app = FastAPI(
    title="obs-auto-camera-switcher",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(routes.router)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("main:app", host="0.0.0.0", port=config.api_port, reload=False)
