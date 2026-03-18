from dataclasses import dataclass, field, asdict
import json
from pathlib import Path

CONFIG_FILE = Path(__file__).parent / "config.json"


@dataclass
class AppConfig:
    enabled: bool = False
    obs_host: str = "localhost"
    obs_port: int = 4455
    obs_password: str = ""
    controller_api_host: str = "localhost"
    controller_api_port: int = 3010
    api_port: int = 3020
    capture_method: str = "obs_screenshot"
    capture_fps: int = 2
    active_cameras: list = field(default_factory=lambda: [1, 2, 3, 4, 5, 6])
    scene_name_template: str = "Escena Camara {n}"
    scene_name_all_cameras: str = "Escena Todas"
    density_tie_threshold_pct: int = 10
    ball_confidence_threshold: float = 0.35
    person_confidence_threshold: float = 0.40
    switch_cooldown_seconds: int = 8
    manual_override_pause_seconds: int = 30
    hysteresis_factor: float = 1.2
    model_path: str = "models/yolov8n_openvino_model/"
    debug_mode: bool = False

    def scene_name_for_camera(self, n: int) -> str:
        return self.scene_name_template.format(n=n)

    @classmethod
    def load(cls) -> "AppConfig":
        if CONFIG_FILE.exists():
            data = json.loads(CONFIG_FILE.read_text())
            known = {k for k in cls.__dataclass_fields__}
            return cls(**{k: v for k, v in data.items() if k in known})
        config = cls()
        config.save()
        return config

    def save(self):
        CONFIG_FILE.write_text(json.dumps(asdict(self), indent=2))

    def update(self, **kwargs):
        for k, v in kwargs.items():
            if hasattr(self, k):
                setattr(self, k, v)
        self.save()
