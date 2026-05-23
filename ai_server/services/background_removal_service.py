"""
Background removal service using rembg (U²-Net).

The rembg session is initialised lazily on the first request and reused
for subsequent calls — avoids slow model reload on every request.

The U²-Net model (~176 MB) is downloaded automatically by rembg on first
use to ~/.u2net/u2net.onnx.  The first request will therefore be slow
(~30-120 s depending on network and hardware).  Subsequent requests are
typically 1-5 s on CPU.
"""

import os
from pathlib import Path
from typing import Optional
from uuid import uuid4

from PIL import Image

from schemas.background_removal_schema import BackgroundRemovalResponse

_DEFAULT_OUTPUT_DIR = "outputs/background_removed"

# Lazy-loaded rembg session — initialised once per process.
_rembg_session = None


def _get_session():
    global _rembg_session
    if _rembg_session is None:
        from rembg import new_session
        _rembg_session = new_session("u2net")
    return _rembg_session


def remove_background_from_image(
    image_path: str,
    output_dir: Optional[str] = None,
) -> BackgroundRemovalResponse:
    """
    Remove the background from a clothing/catalog image using rembg (U²-Net).

    - Saves the result as a PNG with an alpha (transparency) channel.
    - The original image is never modified.
    - Raises FileNotFoundError if image_path does not exist.
    - Raises RuntimeError on processing failure.
    """
    if not os.path.isfile(image_path):
        raise FileNotFoundError(f"Image not found: {image_path}")

    out_dir = Path(output_dir).resolve() if output_dir else Path(_DEFAULT_OUTPUT_DIR).resolve()
    out_dir.mkdir(parents=True, exist_ok=True)

    output_filename = f"bg_removed_{uuid4().hex}.png"
    output_path = out_dir / output_filename

    try:
        from rembg import remove as rembg_remove

        with Image.open(image_path) as img:
            result: Image.Image = rembg_remove(img, session=_get_session())

        result.save(str(output_path), format="PNG")
    except Exception as exc:
        raise RuntimeError(f"Background removal processing failed: {exc}") from exc

    return BackgroundRemovalResponse(
        output_image_path=str(output_path),
        status="completed",
        confidence_score=None,
        warnings=[
            "Background removed using rembg (U²-Net)."
            " Result quality depends on image clarity and contrast."
        ],
    )
