from pydantic import BaseModel
from typing import Optional, List


class TryOnGenerateRequest(BaseModel):
    user_image_path: str
    clothing_image_path: str
    output_dir: Optional[str] = None


class TryOnGenerateResponse(BaseModel):
    result_image_path: str
    status: str
    confidence_score: float
    warnings: List[str]
