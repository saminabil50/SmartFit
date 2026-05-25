from dotenv import load_dotenv
load_dotenv()

import os
from fastapi import FastAPI, HTTPException

from schemas.tryon_schema import TryOnGenerateRequest, TryOnGenerateResponse
from schemas.validation_schema import ImageValidationRequest, ImageValidationResponse
from schemas.background_removal_schema import BackgroundRemovalRequest, BackgroundRemovalResponse
from services.fal_tryon_service import generate_tryon_with_fal
from services.image_validation_service import validate_image_quality
from services.background_removal_service import remove_background_from_image

app = FastAPI(
    title="SmartFit AI Server",
    description="AI endpoints for fal.ai virtual try-on, image validation, and background removal.",
    version="0.2.0",
)


@app.get("/ai/health")
def health_check():
    return {
        "status": "ok",
        "service": "smart-fit-ai-server",
        "version": "0.2.0",
        "models_loaded": {
            "tryon": os.getenv("FAL_TRYON_MODEL", "fal-ai/fashn/tryon/v1.5"),
            "image_validation": "opencv_mediapipe",
            "background_removal": "rembg_u2net",
        },
        "tryon_provider": "fal.ai",
        "fal_key_configured": bool(os.getenv("FAL_KEY", "").strip()),
    }


@app.post("/ai/tryon/generate", response_model=TryOnGenerateResponse)
def generate_tryon(request: TryOnGenerateRequest):
    if not os.getenv("FAL_KEY", "").strip():
        raise HTTPException(status_code=500, detail="FAL_KEY is not configured")
    try:
        return generate_tryon_with_fal(
            user_image_path=request.user_image_path,
            clothing_image_path=request.clothing_image_path,
            category=request.category,
            mode=request.mode,
            garment_photo_type=request.garment_photo_type,
            moderation_level=request.moderation_level,
            num_samples=request.num_samples,
            segmentation_free=request.segmentation_free,
            output_format=request.output_format,
            seed=request.seed,
        )
    except FileNotFoundError as exc:
        raise HTTPException(status_code=404, detail=str(exc))
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))
    except RuntimeError as exc:
        raise HTTPException(status_code=500, detail=str(exc))
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"fal.ai try-on failed: {exc}")


@app.post("/ai/images/validate", response_model=ImageValidationResponse)
def validate_image(request: ImageValidationRequest):
    try:
        return validate_image_quality(request.image_path)
    except Exception:
        raise HTTPException(status_code=500, detail="Image validation failed due to an unexpected error.")


@app.post("/ai/images/remove-background", response_model=BackgroundRemovalResponse)
def remove_background(request: BackgroundRemovalRequest):
    try:
        return remove_background_from_image(request.image_path, request.output_dir)
    except FileNotFoundError:
        raise HTTPException(status_code=404, detail=f"Image not found: {request.image_path}")
    except RuntimeError as exc:
        raise HTTPException(status_code=500, detail=str(exc))
    except Exception:
        raise HTTPException(status_code=500, detail="Background removal failed due to an unexpected error.")


if __name__ == "__main__":
    import uvicorn

    host = os.getenv("AI_SERVER_HOST", "127.0.0.1")
    port = int(os.getenv("AI_SERVER_PORT", "9000"))
    debug = os.getenv("AI_SERVER_DEBUG", "true").lower() == "true"
    uvicorn.run("main:app", host=host, port=port, reload=debug)
