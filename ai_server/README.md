# SmartFit AI Server

A lightweight Python FastAPI server that exposes AI endpoints for the SmartFit platform.
It runs as a **separate process** alongside the main Spring Boot backend (`FitApp/`).

---

## Why a separate server?

- Keeps heavy Python ML dependencies (PyTorch, diffusers, etc.) out of the Java backend.
- Allows the AI models to be scaled, swapped, or restarted independently.
- Lets the backend integrate now with stable request/response contracts while real models are added later.

---

## Setup

### 1. Create a virtual environment

```bash
cd ai_server
python -m venv .venv
```

### 2. Activate the virtual environment

**macOS / Linux:**
```bash
source .venv/bin/activate
```

**Windows PowerShell:**
```powershell
.venv\Scripts\Activate.ps1
```

### 3. Install dependencies

```bash
pip install -r requirements.txt
```

### 4. Configure environment (optional)

```bash
cp .env.example .env
# edit .env if needed
```

### 5. Run the server

```bash
uvicorn main:app --reload --host 127.0.0.1 --port 9000
```

### 6. Open interactive API docs

```
http://127.0.0.1:9000/docs
```

---

## Available endpoints

| Method | Path                        | Description                          |
|--------|-----------------------------|--------------------------------------|
| GET    | `/ai/health`                | Health check — confirms server is up |
| POST   | `/ai/measurements/estimate` | Estimate body measurements from image |
| POST   | `/ai/tryon/generate`        | Generate virtual try-on result        |
| POST   | `/ai/images/validate`       | Validate image quality / person check |

---

## Example curl commands

### Health check
```bash
curl http://127.0.0.1:9000/ai/health
```

### Estimate measurements
```bash
curl -X POST http://127.0.0.1:9000/ai/measurements/estimate \
  -H "Content-Type: application/json" \
  -d '{"image_path": "/path/to/user.jpg", "height_cm": 178}'
```

### Generate try-on
```bash
curl -X POST http://127.0.0.1:9000/ai/tryon/generate \
  -H "Content-Type: application/json" \
  -d '{"user_image_path": "/path/to/user.jpg", "clothing_image_path": "/path/to/shirt.jpg"}'
```

### Validate image
```bash
curl -X POST http://127.0.0.1:9000/ai/images/validate \
  -H "Content-Type: application/json" \
  -d '{"image_path": "/path/to/user.jpg"}'
```

---

## How the backend talks to this server

The Spring Boot backend (`FitApp/`) reads the AI server URL from:

```
AI_SERVER_URL=http://127.0.0.1:9000
```

Set this in `FitApp/.env` or as an environment variable before starting the backend.

The Java client is at:
```
FitApp/src/main/java/com/example/FitApp/ai/AiClient.java
```

It exposes four methods:
- `checkAiHealth()`
- `estimateMeasurements(imagePath, heightCm)`
- `generateTryOn(userImagePath, clothingImagePath, outputDir)`
- `validateImage(imagePath)`

All methods return `null` if the AI server is unreachable, so the backend degrades gracefully.

The backend also exposes a health check endpoint (requires JWT auth):
```
GET /api/v1/health/ai
```

---

## What is placeholder now

| Feature                   | Status         |
|---------------------------|----------------|
| Body measurements         | Placeholder — proportional formula based on height |
| Virtual try-on            | Placeholder — returns the user image unchanged |
| Image validation          | Placeholder — always returns `is_valid: true` |
| Person detection          | Not implemented |
| Blur / quality scoring    | Not implemented |

---

## What AI models can be added later

| Model type               | Libraries to add                          |
|--------------------------|-------------------------------------------|
| Body measurement         | MediaPipe Pose, OpenCV, custom regression |
| Virtual try-on           | Diffusion models (e.g. IDM-VTON), OOTDiffusion |
| Segmentation             | SAM (Segment Anything Model)              |
| Person / full-body check | YOLO, MediaPipe Pose                      |
| Image quality            | OpenCV blur detection, BRISQUE            |

To add a real model: replace the placeholder function in the relevant `services/` file.
The request/response contracts in `schemas/` are stable and will not need to change.
