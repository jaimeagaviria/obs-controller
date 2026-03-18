from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

router = APIRouter()

# These will be set by main.py at startup
_engine = None
_config = None


def init(engine, config):
    global _engine, _config
    _engine = engine
    _config = config


class ConfigUpdate(BaseModel):
    switch_cooldown_seconds: int | None = None
    ball_confidence_threshold: float | None = None
    person_confidence_threshold: float | None = None
    hysteresis_factor: float | None = None
    capture_fps: int | None = None
    manual_override_pause_seconds: int | None = None
    density_tie_threshold_pct: int | None = None
    active_cameras: list[int] | None = None
    debug_mode: bool | None = None


@router.get("/health")
def health():
    return {"status": "ok", "service": "obs-auto-camera-switcher"}


@router.get("/status")
def status():
    if not _engine:
        raise HTTPException(503, "Engine not initialized")
    return _engine.get_status()


@router.post("/enable")
def enable():
    if not _engine or not _config:
        raise HTTPException(503, "Engine not initialized")
    _config.enabled = True
    _config.save()
    return {"enabled": True}


@router.post("/disable")
def disable():
    if not _engine or not _config:
        raise HTTPException(503, "Engine not initialized")
    _config.enabled = False
    _config.save()
    return {"enabled": False}


@router.post("/config")
def update_config(update: ConfigUpdate):
    if not _config:
        raise HTTPException(503, "Config not initialized")
    changes = update.model_dump(exclude_none=True)
    if not changes:
        raise HTTPException(400, "No config values provided")
    _config.update(**changes)
    if _engine:
        _engine.apply_config(_config)
    return {"updated": list(changes.keys()), "config": changes}


@router.get("/detections")
def detections():
    if not _engine:
        raise HTTPException(503, "Engine not initialized")
    return _engine.get_last_detections()
