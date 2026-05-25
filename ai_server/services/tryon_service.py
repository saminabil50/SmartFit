from typing import Optional

from schemas.tryon_schema import TryOnGenerateResponse
from services.fal_tryon_service import generate_tryon_with_fal


def generate_tryon_image(
    user_image_path: str,
    clothing_image_path: str,
    output_dir: Optional[str],
) -> TryOnGenerateResponse:
    # Backward-compatible wrapper for callers that still import this function.
    # The `output_dir` parameter is kept for the stable AI-server contract.
    return generate_tryon_with_fal(user_image_path, clothing_image_path)
