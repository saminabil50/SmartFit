from typing import Optional
from schemas.measurement_schema import MeasurementValues, MeasurementEstimateResponse
from services.pose_service import calculate_basic_measurements


def estimate_measurements_from_image(
    image_path: str, height_cm: Optional[float]
) -> MeasurementEstimateResponse:
    try:
        result = calculate_basic_measurements(image_path, height_cm)
    except Exception:
        # Last-resort fallback — should not normally be reached
        h = height_cm if height_cm is not None else 170.0
        return MeasurementEstimateResponse(
            measurements=MeasurementValues(
                shoulder_width_cm=round(h * 0.25, 1),
                chest_cm=round(h * 0.54, 1),
                waist_cm=round(h * 0.46, 1),
                hip_cm=round(h * 0.53, 1),
                inseam_cm=round(h * 0.44, 1),
            ),
            confidence_score=0.20,
            landmarks_detected=False,
            full_body_visible=False,
            pose_quality="not_detected",
            warnings=["Measurement estimation failed. Returned fallback approximate values."],
        )

    return MeasurementEstimateResponse(
        measurements=MeasurementValues(**result["measurements"]),
        confidence_score=result["confidence_score"],
        landmarks_detected=result["landmarks_detected"],
        full_body_visible=result["full_body_visible"],
        pose_quality=result["pose_quality"],
        warnings=result["warnings"],
    )
