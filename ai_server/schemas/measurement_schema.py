from pydantic import BaseModel, Field
from typing import Optional, List


class MeasurementEstimateRequest(BaseModel):
    image_path: str
    height_cm: Optional[float] = Field(None, ge=50, le=250)


class MeasurementValues(BaseModel):
    shoulder_width_cm: float
    chest_cm: float
    waist_cm: float
    hip_cm: float
    inseam_cm: float


class MeasurementEstimateResponse(BaseModel):
    measurements: MeasurementValues
    confidence_score: float
    landmarks_detected: bool = False
    full_body_visible: bool = False
    pose_quality: str = "not_detected"
    warnings: List[str]
