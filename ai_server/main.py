from dotenv import load_dotenv
load_dotenv()

import os
from fastapi import FastAPI, HTTPException

from schemas.measurement_schema import MeasurementEstimateRequest, MeasurementEstimateResponse
from schemas.tryon_schema import TryOnGenerateRequest, TryOnGenerateResponse
from schemas.validation_schema import ImageValidationRequest, ImageValidationResponse
from schemas.background_removal_schema import BackgroundRemovalRequest, BackgroundRemovalResponse
from services.measurement_service import estimate_measurements_from_image
from services.tryon_service import generate_tryon_placeholder
from services.image_validation_service import validate_image_quality
from services.background_removal_service import remove_background_from_image

app = FastAPI(
    title="SmartFit AI Server",
    description="Placeholder AI endpoints for body measurement, virtual try-on, and image validation.",
    version="0.1.0",
)


@app.get("/ai/health")
def health_check():
    return {
        "status": "ok",
        "service": "smart-fit-ai-server",
        "version": "0.1.0",
        "models_loaded": {
            "measurement": "mediapipe_pose",
            "tryon": "placeholder",
            "image_validation": "opencv_mediapipe",
            "background_removal": "rembg_u2net",
        },
    }


@app.post("/ai/measurements/estimate", response_model=MeasurementEstimateResponse)
def estimate_measurements(request: MeasurementEstimateRequest):
    return estimate_measurements_from_image(request.image_path, request.height_cm)


@app.post("/ai/tryon/generate", response_model=TryOnGenerateResponse)
def generate_tryon(request: TryOnGenerateRequest):
    return generate_tryon_placeholder(
        request.user_image_path, request.clothing_image_path, request.output_dir
    )


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
