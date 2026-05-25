from pydantic import BaseModel
from typing import Optional, List, Any


class TryOnGenerateRequest(BaseModel):
    user_image_path: str
    clothing_image_path: str
    output_dir: Optional[str] = None
    category: str = "auto"
    mode: str = "quality"
    garment_photo_type: str = "auto"
    moderation_level: str = "permissive"
    num_samples: int = 1
    segmentation_free: bool = True
    output_format: str = "png"
    seed: Optional[int] = None


class TryOnGenerateResponse(BaseModel):
    result_image_path: Optional[str] = None
    result_image_url: Optional[str] = None
    status: str
    provider: str = "fal.ai"
    model: Optional[str] = None
    confidence_score: Optional[float] = None
    warnings: List[str] = []
    raw_images: List[Any] = []
