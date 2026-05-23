from pydantic import BaseModel
from typing import Optional, List


class BackgroundRemovalRequest(BaseModel):
    image_path: str
    output_dir: Optional[str] = None


class BackgroundRemovalResponse(BaseModel):
    output_image_path: str
    status: str
    confidence_score: Optional[float] = None
    warnings: List[str]
