"""
fal.ai FASHN Virtual Try-On service.

Calls fal-ai/fashn/tryon/v1.5 via the official fal-client.
FAL_KEY must be set in the environment (via .env or shell).

Local file paths are uploaded to fal's storage using fal_client.upload_file()
so the external API can read them. Public URLs and base64 data URIs are
passed through directly.
"""

import os
import base64
import logging
import mimetypes
from typing import Optional

from schemas.tryon_schema import TryOnGenerateResponse

ALLOWED_CATEGORIES = {"tops", "bottoms", "one-pieces", "auto"}
ALLOWED_MODES = {"performance", "balanced", "quality"}
ALLOWED_GARMENT_PHOTO_TYPES = {"auto", "model", "flat-lay"}
ALLOWED_MODERATION_LEVELS = {"none", "permissive", "conservative"}
ALLOWED_OUTPUT_FORMATS = {"png", "jpeg"}
MAX_NUM_SAMPLES = 2

logger = logging.getLogger(__name__)


def get_default_fal_tryon_arguments() -> dict:
    return {
        "category": "auto",
        "mode": "quality",
        "garment_photo_type": "auto",
        "moderation_level": "permissive",
        "num_samples": 1,
        "segmentation_free": True,
        "output_format": "png",
    }


def prepare_fal_input(value: str) -> str:
    """
    Convert a local file path to a base64 data URI.
    Public https:// URLs and data: URIs are returned as-is.

    We intentionally avoid fal_client.upload_file() here because it requires fal
    storage/CDN permissions. Data URIs only require model inference access.
    """
    if value.startswith(("http://", "https://", "data:")):
        return value

    if not os.path.isfile(value):
        raise FileNotFoundError(f"Image file not found: {value}")

    mime_type, _ = mimetypes.guess_type(value)
    if not mime_type or not mime_type.startswith("image/"):
        mime_type = "image/jpeg"

    with open(value, "rb") as image_file:
        encoded = base64.b64encode(image_file.read()).decode("ascii")
    return f"data:{mime_type};base64,{encoded}"


def generate_tryon_with_fal(
    user_image_path: str,
    clothing_image_path: str,
    category: str = "auto",
    mode: str = "quality",
    garment_photo_type: str = "auto",
    moderation_level: str = "permissive",
    num_samples: int = 1,
    segmentation_free: bool = True,
    output_format: str = "png",
    seed: Optional[int] = None,
) -> TryOnGenerateResponse:
    fal_key = os.getenv("FAL_KEY", "").strip()
    if not fal_key:
        raise RuntimeError("FAL_KEY is not configured")

    os.environ["FAL_KEY"] = fal_key

    model = os.getenv("FAL_TRYON_MODEL", "fal-ai/fashn/tryon/v1.5")
    defaults = _get_configured_fal_tryon_arguments()
    overrides = _validate_fal_tryon_overrides(
        {
            "category": category,
            "mode": mode,
            "garment_photo_type": garment_photo_type,
            "moderation_level": moderation_level,
            "num_samples": num_samples,
            "segmentation_free": segmentation_free,
            "output_format": output_format,
            "seed": seed,
        }
    )
    final_args = {**defaults, **overrides}

    import fal_client

    model_url = prepare_fal_input(user_image_path)
    garment_url = prepare_fal_input(clothing_image_path)

    arguments = {
        "model_image": model_url,
        "garment_image": garment_url,
        **final_args,
    }

    logger.info(
        "Calling fal.ai try-on provider=fal model=%s arguments=%s",
        model,
        _sanitize_arguments_for_logging(arguments),
    )

    result = fal_client.subscribe(
        model,
        arguments=arguments,
        with_logs=True,
    )

    provider_error = _extract_provider_error(result)
    if provider_error:
        raise RuntimeError(provider_error)

    images = result.get("images") if isinstance(result, dict) else getattr(result, "images", None)
    if not images:
        raise RuntimeError("fal.ai try-on returned no images")

    first = images[0]
    image_url = first.get("url") if isinstance(first, dict) else getattr(first, "url", None)
    if not image_url:
        raise RuntimeError("fal.ai try-on image has no URL")

    return TryOnGenerateResponse(
        result_image_path=None,
        result_image_url=image_url,
        status="completed",
        provider="fal.ai",
        model=model,
        confidence_score=None,
        warnings=[],
        raw_images=images if isinstance(images[0], dict) else [{"url": i.url} for i in images],
    )


def _validated_choice(value: str, allowed: set[str], field: str) -> str:
    if not isinstance(value, str):
        raise ValueError(f"Invalid {field}. Allowed values: {', '.join(sorted(allowed))}")
    normalized = value.strip().lower()
    if normalized not in allowed:
        raise ValueError(f"Invalid {field}. Allowed values: {', '.join(sorted(allowed))}")
    return normalized


def _get_configured_fal_tryon_arguments() -> dict:
    defaults = get_default_fal_tryon_arguments()
    configured = {
        "category": os.getenv("FAL_TRYON_CATEGORY", defaults["category"]),
        "mode": os.getenv("FAL_TRYON_MODE", defaults["mode"]),
        "garment_photo_type": os.getenv("FAL_GARMENT_PHOTO_TYPE", defaults["garment_photo_type"]),
        "moderation_level": os.getenv("FAL_MODERATION_LEVEL", defaults["moderation_level"]),
        "num_samples": int(os.getenv("FAL_NUM_SAMPLES", str(defaults["num_samples"]))),
        "segmentation_free": _parse_bool(
            os.getenv("FAL_SEGMENTATION_FREE", str(defaults["segmentation_free"]))
        ),
        "output_format": os.getenv("FAL_OUTPUT_FORMAT", defaults["output_format"]),
    }
    return _validate_fal_tryon_overrides(configured)


def _validate_fal_tryon_overrides(values: dict) -> dict:
    validated = {}
    if "category" in values and values["category"] is not None:
        validated["category"] = _validated_choice(values["category"], ALLOWED_CATEGORIES, "category")
    if "mode" in values and values["mode"] is not None:
        validated["mode"] = _validated_choice(values["mode"], ALLOWED_MODES, "mode")
    if "garment_photo_type" in values and values["garment_photo_type"] is not None:
        validated["garment_photo_type"] = _validated_choice(
            values["garment_photo_type"],
            ALLOWED_GARMENT_PHOTO_TYPES,
            "garment_photo_type",
        )
    if "moderation_level" in values and values["moderation_level"] is not None:
        validated["moderation_level"] = _validated_choice(
            values["moderation_level"],
            ALLOWED_MODERATION_LEVELS,
            "moderation_level",
        )
    if "output_format" in values and values["output_format"] is not None:
        validated["output_format"] = _validated_choice(
            values["output_format"],
            ALLOWED_OUTPUT_FORMATS,
            "output_format",
        )
    if "num_samples" in values and values["num_samples"] is not None:
        validated["num_samples"] = _validated_num_samples(values["num_samples"])
    if "segmentation_free" in values and values["segmentation_free"] is not None:
        if not isinstance(values["segmentation_free"], bool):
            raise ValueError("segmentation_free must be a boolean")
        validated["segmentation_free"] = values["segmentation_free"]
    if "seed" in values and values["seed"] is not None:
        if not isinstance(values["seed"], int):
            raise ValueError("seed must be an integer")
        validated["seed"] = values["seed"]
    return validated


def _validated_num_samples(value: int) -> int:
    if not isinstance(value, int):
        raise ValueError("num_samples must be an integer")
    if value < 1 or value > MAX_NUM_SAMPLES:
        raise ValueError(f"num_samples must be between 1 and {MAX_NUM_SAMPLES}")
    return value


def _parse_bool(value: str) -> bool:
    normalized = value.strip().lower()
    if normalized in {"true", "1", "yes", "y"}:
        return True
    if normalized in {"false", "0", "no", "n"}:
        return False
    raise ValueError("FAL_SEGMENTATION_FREE must be true or false")


def _sanitize_arguments_for_logging(arguments: dict) -> dict:
    sanitized = dict(arguments)
    if "model_image" in sanitized:
        sanitized["model_image"] = "<image>"
    if "garment_image" in sanitized:
        sanitized["garment_image"] = "<image>"
    return sanitized


def _extract_provider_error(result) -> Optional[str]:
    if not isinstance(result, dict):
        return None
    error = result.get("error") or result.get("detail") or result.get("message")
    if not error:
        return None
    if isinstance(error, dict):
        return str(error.get("message") or error.get("detail") or error)
    return str(error)
