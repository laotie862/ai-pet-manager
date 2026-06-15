from typing import Any

from fastapi import FastAPI

from app.bailian import classify_bailian
from app.config import BAILIAN_API_KEY, BEHAVIORS, CV_PROVIDER, IDENTITY_EMBEDDER, IDENTITY_MODEL_VERSION, MODEL_VERSION
from app.embedding import extract_identity_embedding
from app.fallback import classify_placeholder
from app.image_utils import decode_image
from app.schemas import DetectRequest, DetectResponse, EmbedRequest, EmbedResponse


app = FastAPI(title="AI Pet Care CV Service", version=MODEL_VERSION)


@app.get("/cv/health")
def health() -> dict[str, Any]:
    return {
        "status": "ok",
        "provider": CV_PROVIDER,
        "model_version": MODEL_VERSION,
        "identity_embedder": IDENTITY_EMBEDDER,
        "identity_model_version": IDENTITY_MODEL_VERSION,
        "behaviors": BEHAVIORS,
        "bailian_configured": bool(BAILIAN_API_KEY),
    }


@app.post("/cv/detect", response_model=DetectResponse)
def detect(request: DetectRequest) -> DetectResponse:
    image = decode_image(request.image_base64)
    if image is None:
        return no_detection()

    if CV_PROVIDER == "bailian":
        found, behavior, confidence = classify_bailian(image)
        boxes = []
    else:
        found = True
        behavior, confidence = classify_placeholder(image)
        boxes = full_frame_pet_box(image.width, image.height, confidence)

    return DetectResponse(
        found=found,
        behavior=behavior,
        confidence=confidence,
        boxes=boxes,
        model_version=MODEL_VERSION,
    )


@app.post("/cv/embed", response_model=EmbedResponse)
def embed(request: EmbedRequest) -> EmbedResponse:
    image = decode_image(request.image_base64)
    if image is None:
        return EmbedResponse(embedding=[], model_version=IDENTITY_MODEL_VERSION, dimension=0)

    embedding, model_version = extract_identity_embedding(image)
    return EmbedResponse(
        embedding=embedding,
        model_version=model_version,
        dimension=len(embedding),
    )


def no_detection() -> DetectResponse:
    return DetectResponse(
        found=False,
        behavior="uncertain",
        confidence=0.0,
        boxes=[],
        model_version=MODEL_VERSION,
    )


def full_frame_pet_box(width: int, height: int, confidence: float) -> list[dict[str, Any]]:
    return [{"x": 0, "y": 0, "w": width, "h": height, "label": "pet", "confidence": confidence}]
