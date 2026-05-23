"""
MediaPipe Pose Landmarker service.

Uses the MediaPipe Tasks API (0.10.x+) with pose_landmarker_lite.task.
All measurement outputs are approximate prototype estimates — not clinical measurements.
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


def calculate_basic_measurements(image_path: str, height_cm: Optional[float]) -> dict:
    """
    Estimate body measurements using MediaPipe pose landmarks.
    Falls back to height-ratio formulas if pose is not detected.

    All returned values are approximate prototype estimates.
    """
    warnings: list[str] = []
    h_cm = height_cm

    if h_cm is None:
        h_cm = 170.0
        warnings.append("Height not provided. Using 170 cm as fallback for estimates.")

    pose = detect_pose_landmarks(image_path)
    warnings.extend(pose["warnings"])

    if not pose["landmarks_detected"]:
        return {
            "measurements": _ratio_measurements(h_cm),
            "confidence_score": 0.20,
            "landmarks_detected": False,
            "full_body_visible": False,
            "pose_quality": "not_detected",
            "warnings": warnings + [
                "No body pose detected. Returned fallback approximate measurements."
            ],
        }

    lms = pose["raw_landmarks"]
    img_w = pose["image_width"]
    img_h = pose["image_height"]

    ls = lms[_IDX["LEFT_SHOULDER"]]
    rs = lms[_IDX["RIGHT_SHOULDER"]]
    lh = lms[_IDX["LEFT_HIP"]]
    rh = lms[_IDX["RIGHT_HIP"]]
    la = lms[_IDX["LEFT_ANKLE"]]
    ra = lms[_IDX["RIGHT_ANKLE"]]

    # Pixel widths from normalized coords
    shoulder_px = abs(rs.x - ls.x) * img_w
    hip_px = abs(rh.x - lh.x) * img_w

    # Vertical body span: shoulder midpoint → ankle midpoint
    shoulder_mid_y = ((ls.y + rs.y) / 2) * img_h
    ankle_mid_y = ((la.y + ra.y) / 2) * img_h
    body_span_px = ankle_mid_y - shoulder_mid_y

    if body_span_px < 20:
        # Pose found but can't extract reliable scale — use ratio fallback
        warnings.append(
            "Could not reliably estimate pixel scale from pose. Using height-ratio fallback."
        )
        confidence = 0.45 if pose["pose_quality"] == "partial" else 0.20
        return {
            "measurements": _ratio_measurements(h_cm),
            "confidence_score": confidence,
            "landmarks_detected": True,
            "full_body_visible": pose["full_body_visible"],
            "pose_quality": pose["pose_quality"],
            "warnings": warnings + [
                "Approximate measurement based on height ratio (pose scale insufficient).",
                "Results are prototype estimates and may not reflect real-world measurements.",
            ],
        }

    # Shoulder-to-ankle spans ~85% of total body height
    cm_per_px = (h_cm * 0.85) / body_span_px

    shoulder_width_cm = round(shoulder_px * cm_per_px, 1)
    hip_width_cm = round(hip_px * cm_per_px, 1)

    measurements = {
        "shoulder_width_cm": shoulder_width_cm,
        "chest_cm": round(shoulder_width_cm * 2.15, 1),
        "waist_cm": round(shoulder_width_cm * 1.85, 1),
        "hip_cm": round(hip_width_cm * 2.20, 1),
        "inseam_cm": round(h_cm * 0.44, 1),
    }

    if pose["full_body_visible"] and pose["pose_quality"] == "good":
        confidence = 0.80
    elif pose["pose_quality"] == "partial":
        confidence = 0.65
    else:
        confidence = 0.45

    warnings += [
        "Approximate measurement based on MediaPipe pose landmarks.",
        "Results are prototype estimates and may not reflect real-world measurements.",
    ]

    return {
        "measurements": measurements,
        "confidence_score": confidence,
        "landmarks_detected": True,
        "full_body_visible": pose["full_body_visible"],
        "pose_quality": pose["pose_quality"],
        "warnings": warnings,
    }


def _ratio_measurements(h_cm: float) -> dict:
    return {
        "shoulder_width_cm": round(h_cm * 0.25, 1),
        "chest_cm": round(h_cm * 0.54, 1),
        "waist_cm": round(h_cm * 0.46, 1),
        "hip_cm": round(h_cm * 0.53, 1),
        "inseam_cm": round(h_cm * 0.44, 1),
    }
