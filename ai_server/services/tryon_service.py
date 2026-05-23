from typing import Optional
from schemas.tryon_schema import TryOnGenerateResponse


def generate_tryon_placeholder(
    user_image_path: str,
    clothing_image_path: str,
    output_dir: Optional[str],
) -> TryOnGenerateResponse:
    # TODO: replace with real virtual try-on model
    return TryOnGenerateResponse(
        result_image_path=user_image_path,
        status="completed",
        confidence_score=0.40,
        warnings=["Placeholder try-on result. Real virtual try-on model will be connected later."],
    )
