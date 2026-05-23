import cv2
from schemas.validation_schema import ImageValidationResponse
from services.pose_service import detect_pose_landmarks

MIN_WIDTH = 300
MIN_HEIGHT = 300
MAX_WIDTH = 5000
MAX_HEIGHT = 5000
BLUR_THRESHOLD = 80.0
MIN_BRIGHTNESS = 50.0
MAX_BRIGHTNESS = 220.0


def validate_image_quality(image_path: str) -> ImageValidationResponse:
    warnings: list[str] = []

    # ── OpenCV quality checks ──────────────────────────────────────────────────
    image = cv2.imread(image_path)
    if image is None:
        return ImageValidationResponse(
            is_valid=False,
            has_person=None,
            full_body_visible=None,
            blur_score=None,
            brightness_score=None,
            width=None,
            height=None,
            pose_quality=None,
            visible_landmarks_count=None,
            warnings=["Image could not be loaded."],
        )

    height, width = image.shape[:2]

    if width < MIN_WIDTH or height < MIN_HEIGHT:
        warnings.append(
            f"Image is too small ({width}x{height}px). Minimum required size is {MIN_WIDTH}x{MIN_HEIGHT}px."
        )
    if width > MAX_WIDTH or height > MAX_HEIGHT:
        warnings.append(
            f"Image is too large ({width}x{height}px). Maximum allowed size is {MAX_WIDTH}x{MAX_HEIGHT}px."
        )

    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    blur_score = round(float(cv2.Laplacian(gray, cv2.CV_64F).var()), 2)
    if blur_score < BLUR_THRESHOLD:
        warnings.append("Image appears blurry. Please upload a clearer image.")

    brightness_score = round(float(gray.mean()), 2)
    if brightness_score < MIN_BRIGHTNESS:
        warnings.append("Image is too dark. Please use better lighting.")
    elif brightness_score > MAX_BRIGHTNESS:
        warnings.append("Image is too bright or overexposed.")

    # ── MediaPipe pose checks ──────────────────────────────────────────────────
    pose = detect_pose_landmarks(image_path)

    has_person = pose["landmarks_detected"]
    full_body_visible = pose["full_body_visible"] if has_person else False
    pose_quality = pose["pose_quality"]
    visible_landmarks_count = pose["visible_landmarks_count"]

    if not has_person:
        warnings.append("No person detected in the image.")
    elif not full_body_visible:
        warnings.append("Full body is not clearly visible.")

    return ImageValidationResponse(
        is_valid=len(warnings) == 0,
        has_person=has_person,
        full_body_visible=full_body_visible,
        blur_score=blur_score,
        brightness_score=brightness_score,
        width=width,
        height=height,
        pose_quality=pose_quality,
        visible_landmarks_count=visible_landmarks_count,
        warnings=warnings,
    )
