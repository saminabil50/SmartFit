"""
MediaPipe Pose Landmarker service.

Uses the MediaPipe Tasks API (0.10.x+) with pose_landmarker_lite.task.
"""

import os
import cv2
from typing import Optional

try:
    import mediapipe as mp
    _BaseOptions = mp.tasks.BaseOptions
    _PoseLandmarker = mp.tasks.vision.PoseLandmarker
    _PoseLandmarkerOptions = mp.tasks.vision.PoseLandmarkerOptions
    _RunningMode = mp.tasks.vision.RunningMode
    _MP_AVAILABLE = True
except Exception:
    _MP_AVAILABLE = False

# Model path relative to the ai_server/ working directory
_MODEL_PATH = os.path.join(os.path.dirname(__file__), "..", "models", "pose_landmarker_lite.task")

# Landmark indices (same as mp.tasks.vision.PoseLandmark values)
_IDX = {
    "LEFT_SHOULDER": 11,
    "RIGHT_SHOULDER": 12,
    "LEFT_HIP": 23,
    "RIGHT_HIP": 24,
    "LEFT_KNEE": 25,
    "RIGHT_KNEE": 26,
    "LEFT_ANKLE": 27,
    "RIGHT_ANKLE": 28,
}
_FULL_BODY_INDICES = list(_IDX.values())  # all 8 key joints

VISIBILITY_THRESHOLD = 0.5
_TOTAL_LANDMARKS = 33

# Lazy singleton — created once on first use, reused for all subsequent calls.
_landmarker_instance = None


def _get_landmarker():
    global _landmarker_instance
    if _landmarker_instance is None:
        if not _MP_AVAILABLE or not os.path.exists(_MODEL_PATH):
            return None
        options = _PoseLandmarkerOptions(
            base_options=_BaseOptions(model_asset_path=_MODEL_PATH),
            running_mode=_RunningMode.IMAGE,
            num_poses=1,
            min_pose_detection_confidence=0.5,
            min_pose_presence_confidence=0.5,
        )
        _landmarker_instance = _PoseLandmarker.create_from_options(options)
    return _landmarker_instance


def _run_landmarker(image_rgb) -> Optional[list]:
    """Run the pose landmarker and return the NormalizedLandmark list, or None."""
    landmarker = _get_landmarker()
    if landmarker is None:
        return None
    try:
        mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=image_rgb)
        result = landmarker.detect(mp_image)
        if result.pose_landmarks:
            return result.pose_landmarks[0]  # first pose
        return None
    except Exception:
        return None


def detect_pose_landmarks(image_path: str) -> dict:
    """
    Load an image and run MediaPipe Pose Landmarker.
    Always returns a dict — never raises.
    """
    image = cv2.imread(image_path)
    if image is None:
        return {
            "landmarks_detected": False,
            "raw_landmarks": None,
            "visible_landmarks_count": 0,
            "full_body_visible": False,
            "pose_quality": "not_detected",
            "warnings": ["Image could not be loaded."],
            "image_width": 0,
            "image_height": 0,
        }

    h, w = image.shape[:2]
    image_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    landmarks = _run_landmarker(image_rgb)

    if landmarks is None:
        if not _MP_AVAILABLE or not os.path.exists(_MODEL_PATH):
            msg = "MediaPipe model not available. Pose detection skipped."
        else:
            msg = "No person detected in the image."
        return {
            "landmarks_detected": False,
            "raw_landmarks": None,
            "visible_landmarks_count": 0,
            "full_body_visible": False,
            "pose_quality": "not_detected",
            "warnings": [msg],
            "image_width": w,
            "image_height": h,
        }

    visible_count = sum(
        1 for lm in landmarks if lm.visibility is not None and lm.visibility >= VISIBILITY_THRESHOLD
    )

    full_body_visible = all(
        landmarks[idx].visibility is not None and landmarks[idx].visibility >= VISIBILITY_THRESHOLD
        for idx in _FULL_BODY_INDICES
    )

    if full_body_visible and visible_count >= 25:
        pose_quality = "good"
    elif visible_count >= 15:
        pose_quality = "partial"
    elif visible_count > 0:
        pose_quality = "poor"
    else:
        pose_quality = "not_detected"

    warnings: list[str] = []
    if not full_body_visible:
        warnings.append("Full body is not clearly visible.")
    if pose_quality == "poor":
        warnings.append("Pose quality is poor. Few body landmarks detected.")

    return {
        "landmarks_detected": True,
        "raw_landmarks": landmarks,
        "visible_landmarks_count": visible_count,
        "full_body_visible": full_body_visible,
        "pose_quality": pose_quality,
        "warnings": warnings,
        "image_width": w,
        "image_height": h,
    }
