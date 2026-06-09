import base64
import binascii
import io
import json
import logging
import os
import re
from typing import Any

import httpx
from fastapi import FastAPI
from PIL import Image, ImageStat, UnidentifiedImageError
from pydantic import BaseModel, Field


BEHAVIORS = ["eating", "drinking", "exercising", "sleeping", "defecating", "uncertain"]
PROVIDER_ALIASES = {
    "placeholder": "placeholder",
    "local-placeholder": "placeholder",
    "bailian": "bailian",
    "aliyun": "bailian",
    "dashscope": "bailian",
    "qwen": "bailian",
}
CV_PROVIDER = PROVIDER_ALIASES.get(os.getenv("CV_PROVIDER", "placeholder").strip().lower(), "placeholder")

BAILIAN_API_KEY = os.getenv("BAILIAN_API_KEY") or os.getenv("DASHSCOPE_API_KEY")
BAILIAN_BASE_URL = os.getenv("BAILIAN_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")
BAILIAN_MODEL = os.getenv("BAILIAN_MODEL", "qwen3-vl-plus")
BAILIAN_TIMEOUT_SECONDS = float(os.getenv("BAILIAN_TIMEOUT_SECONDS", "20"))
BAILIAN_IMAGE_MAX_EDGE = int(os.getenv("BAILIAN_IMAGE_MAX_EDGE", "1024"))
BAILIAN_IMAGE_JPEG_QUALITY = int(os.getenv("BAILIAN_IMAGE_JPEG_QUALITY", "85"))

MODEL_VERSION = os.getenv("CV_MODEL_VERSION") or (
    f"bailian:{BAILIAN_MODEL}" if CV_PROVIDER == "bailian" else "local-placeholder"
)

logger = logging.getLogger(__name__)

app = FastAPI(title="AI Pet Care CV Service", version=MODEL_VERSION)


class RoiPoint(BaseModel):
    x: float
    y: float


class DetectRequest(BaseModel):
    image_base64: str = Field(min_length=1)
    device_id: str | None = None
    roi_polygon: list[RoiPoint] = Field(default_factory=list)


class DetectResponse(BaseModel):
    found: bool
    behavior: str
    confidence: float
    boxes: list[dict[str, Any]] = Field(default_factory=list)
    model_version: str


@app.get("/cv/health")
def health() -> dict[str, Any]:
    return {
        "status": "ok",
        "provider": CV_PROVIDER,
        "model_version": MODEL_VERSION,
        "behaviors": BEHAVIORS,
        "bailian_configured": bool(BAILIAN_API_KEY),
    }


@app.post("/cv/detect", response_model=DetectResponse)
def detect(request: DetectRequest) -> DetectResponse:
    image = decode_image(request.image_base64)
    if image is None:
        return DetectResponse(
            found=False,
            behavior="uncertain",
            confidence=0.0,
            boxes=[],
            model_version=MODEL_VERSION,
        )

    width, height = image.size

    if CV_PROVIDER == "bailian":
        found, behavior, confidence = classify_bailian(image)
        boxes = [] if found else []
    else:
        found = True
        behavior, confidence = classify_placeholder(image)
        boxes = [{"x": 0, "y": 0, "w": width, "h": height, "label": "pet", "confidence": confidence}]

    return DetectResponse(
        found=found,
        behavior=behavior,
        confidence=confidence,
        boxes=boxes,
        model_version=MODEL_VERSION,
    )


def decode_image(image_base64: str) -> Image.Image | None:
    try:
        raw = base64.b64decode(image_base64, validate=True)
        image = Image.open(io.BytesIO(raw))
        image.load()
        return image.convert("RGB")
    except (binascii.Error, UnidentifiedImageError, OSError, ValueError):
        return None


def classify_placeholder(image: Image.Image) -> tuple[str, float]:
    # Deterministic placeholder: enough for integration, easy to replace with a trained model.
    small = image.resize((32, 32))
    stat = ImageStat.Stat(small)
    r, g, b = stat.mean
    brightness = (r + g + b) / 3
    color_bias = max(r, g, b) - min(r, g, b)

    if brightness < 35:
        return "sleeping", 0.76
    if b > r + 18 and b > g + 8:
        return "drinking", 0.73
    if g > r + 12 and g > b + 6:
        return "exercising", 0.72
    if r > g + 12 and r > b + 6:
        return "eating", 0.74
    if color_bias < 10 and brightness < 120:
        return "defecating", 0.69
    return "uncertain", 0.45


def classify_bailian(image: Image.Image) -> tuple[bool, str, float]:
    if not BAILIAN_API_KEY:
        logger.warning("CV_PROVIDER=bailian but BAILIAN_API_KEY/DASHSCOPE_API_KEY is not configured")
        return False, "uncertain", 0.0

    image_url = "data:image/jpeg;base64," + encode_jpeg_base64(image)
    payload = {
        "model": BAILIAN_MODEL,
        "messages": [
            {
                "role": "system",
                "content": (
                    "You are a pet behavior classifier for a home pet-care camera. "
                    "Return strict JSON only. Do not include markdown."
                ),
            },
            {
                "role": "user",
                "content": [
                    {
                        "type": "text",
                        "text": (
                            "Please inspect the image and classify what the visible pet is doing. "
                            "Use only one behavior from: eating, drinking, exercising, sleeping, "
                            "defecating, uncertain. Return JSON exactly like "
                            "{\"found\":true,\"behavior\":\"eating\",\"confidence\":0.82}. "
                            "Set found=false if no pet is visible. Use uncertain when the action is unclear."
                        ),
                    },
                    {"type": "image_url", "image_url": {"url": image_url}},
                ],
            },
        ],
        "temperature": 0,
    }

    url = BAILIAN_BASE_URL.rstrip("/") + "/chat/completions"
    try:
        with httpx.Client(timeout=BAILIAN_TIMEOUT_SECONDS) as client:
            response = client.post(
                url,
                headers={
                    "Authorization": f"Bearer {BAILIAN_API_KEY}",
                    "Content-Type": "application/json",
                },
                json=payload,
            )
            response.raise_for_status()
        content = response.json()["choices"][0]["message"]["content"]
        return parse_bailian_result(content)
    except (httpx.HTTPError, KeyError, IndexError, TypeError, ValueError) as exc:
        logger.warning("Bailian vision classification failed: %s", exc)
        return False, "uncertain", 0.0


def encode_jpeg_base64(image: Image.Image) -> str:
    image = image.convert("RGB")
    width, height = image.size
    longest_edge = max(width, height)
    if longest_edge > BAILIAN_IMAGE_MAX_EDGE:
        scale = BAILIAN_IMAGE_MAX_EDGE / longest_edge
        image = image.resize((max(1, int(width * scale)), max(1, int(height * scale))))

    buffer = io.BytesIO()
    image.save(buffer, format="JPEG", quality=BAILIAN_IMAGE_JPEG_QUALITY, optimize=True)
    return base64.b64encode(buffer.getvalue()).decode("ascii")


def parse_bailian_result(content: Any) -> tuple[bool, str, float]:
    if isinstance(content, list):
        content = "\n".join(
            item.get("text", "") for item in content if isinstance(item, dict) and item.get("type") == "text"
        )
    if not isinstance(content, str):
        return False, "uncertain", 0.0

    match = re.search(r"\{.*\}", content, flags=re.DOTALL)
    if not match:
        return False, "uncertain", 0.0

    data = json.loads(match.group(0))
    behavior = str(data.get("behavior", "uncertain")).strip().lower()
    if behavior not in BEHAVIORS:
        behavior = "uncertain"

    confidence = clamp_float(data.get("confidence", 0.0), 0.0, 1.0)
    found = bool(data.get("found", behavior != "uncertain" and confidence > 0))
    if not found:
        behavior = "uncertain"
        confidence = 0.0

    return found, behavior, confidence


def clamp_float(value: Any, minimum: float, maximum: float) -> float:
    try:
        number = float(value)
    except (TypeError, ValueError):
        return minimum
    return max(minimum, min(maximum, number))
