import logging
from dataclasses import dataclass, field

logger = logging.getLogger(__name__)


@dataclass
class CameraScore:
    camera_id: int
    ball_score: float = 0.0
    density_score: float = 0.0
    total_score: float = 0.0
    ball_detected: bool = False
    person_count: int = 0
    best_ball_confidence: float = 0.0
    reason: str = ""


@dataclass
class SwitchDecision:
    should_switch: bool
    target_scene: str
    target_camera: int | None
    reason: str
    confidence: float
    scores: dict = field(default_factory=dict)


class CameraScorer:
    """Scores cameras based on detections and decides which scene to show."""

    def __init__(
        self,
        scene_template: str,
        scene_all: str,
        density_tie_pct: int = 10,
        hysteresis: float = 1.2,
    ):
        self.scene_template = scene_template
        self.scene_all = scene_all
        self.density_tie_pct = density_tie_pct
        self.hysteresis = hysteresis

    def score_camera(self, camera_id: int, detections: dict) -> CameraScore:
        score = CameraScore(camera_id=camera_id)
        balls = detections.get("balls", [])
        persons = detections.get("persons", [])

        if balls:
            best_ball = max(balls, key=lambda b: b["confidence"])
            score.ball_detected = True
            score.best_ball_confidence = best_ball["confidence"]
            cx, cy = best_ball["center_x"], best_ball["center_y"]
            centrality = 1.0 - (abs(cx - 0.5) + abs(cy - 0.5))
            score.ball_score = 100 + best_ball["confidence"] * 50 + centrality * 20

        score.person_count = len(persons)
        total_area = sum(p["area_pct"] for p in persons)
        score.density_score = len(persons) * 10 + total_area * 5

        score.total_score = score.ball_score + score.density_score
        return score

    def decide(
        self, all_detections: dict[int, dict], current_scene: str
    ) -> SwitchDecision:
        scores: dict[int, CameraScore] = {}
        for cam_id, dets in all_detections.items():
            scores[cam_id] = self.score_camera(cam_id, dets)

        scores_dict = {cid: s for cid, s in scores.items()}

        if not scores:
            return SwitchDecision(
                should_switch=current_scene != self.scene_all,
                target_scene=self.scene_all,
                target_camera=None,
                reason="no_cameras",
                confidence=0.0,
                scores=scores_dict,
            )

        # Priority 1: Camera with ball detected
        ball_cameras = {cid: s for cid, s in scores.items() if s.ball_detected}
        if ball_cameras:
            best = max(ball_cameras.values(), key=lambda s: s.ball_score)
            target = self.scene_template.format(n=best.camera_id)
            return self._apply_hysteresis(
                target,
                best.camera_id,
                "ball_detected",
                best.best_ball_confidence,
                current_scene,
                scores_dict,
            )

        # Priority 2: Camera with most players
        density_cameras = {
            cid: s for cid, s in scores.items() if s.person_count > 0
        }
        if density_cameras:
            sorted_by_density = sorted(
                density_cameras.values(), key=lambda s: s.density_score, reverse=True
            )
            best = sorted_by_density[0]

            if len(sorted_by_density) > 1:
                second = sorted_by_density[1]
                if best.density_score > 0:
                    diff_pct = (
                        (best.density_score - second.density_score)
                        / best.density_score
                        * 100
                    )
                    if diff_pct < self.density_tie_pct:
                        return SwitchDecision(
                            should_switch=current_scene != self.scene_all,
                            target_scene=self.scene_all,
                            target_camera=None,
                            reason="density_tie",
                            confidence=round(best.density_score / 100.0, 2),
                            scores=scores_dict,
                        )

            target = self.scene_template.format(n=best.camera_id)
            return self._apply_hysteresis(
                target,
                best.camera_id,
                "player_density",
                round(best.density_score / 100.0, 2),
                current_scene,
                scores_dict,
            )

        # Priority 3: No detections
        return SwitchDecision(
            should_switch=current_scene != self.scene_all,
            target_scene=self.scene_all,
            target_camera=None,
            reason="no_detection",
            confidence=0.0,
            scores=scores_dict,
        )

    def _apply_hysteresis(
        self,
        target: str,
        camera_id: int,
        reason: str,
        confidence: float,
        current_scene: str,
        scores: dict,
    ) -> SwitchDecision:
        should_switch = target != current_scene

        # Hysteresis only between specific cameras (not to/from "Todas")
        if (
            should_switch
            and current_scene != self.scene_all
            and target != self.scene_all
        ):
            current_cam_id = None
            for cid in scores:
                if self.scene_template.format(n=cid) == current_scene:
                    current_cam_id = cid
                    break

            if current_cam_id and current_cam_id in scores:
                current_score = scores[current_cam_id].total_score
                new_score = scores[camera_id].total_score
                if current_score > 0 and new_score < current_score * self.hysteresis:
                    should_switch = False

        return SwitchDecision(
            should_switch=should_switch,
            target_scene=target,
            target_camera=camera_id,
            reason=reason,
            confidence=min(confidence, 1.0),
            scores=scores,
        )
