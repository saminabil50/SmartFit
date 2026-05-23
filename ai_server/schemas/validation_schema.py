from pydantic import BaseModel
from typing import Optional, List


class ImageValidationRequest(BaseModel):
    image_path: str


class ImageValidationResponse(BaseModel):
    is_valid: bool
    has_person: Optional[bool] = None
    full_body_visible: Optional[bool] = None
    blur_score: Optional[float] = None
    brightness_score: Optional[float] = None
    width: Optional[int] = None
    height: Optional[int] = None
    pose_quality: Optional[str] = None
    visible_landmarks_count: Optional[int] = None
    warnings: List[str]
