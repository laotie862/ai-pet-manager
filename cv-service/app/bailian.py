import json
import logging
import re
from typing import Any

import httpx
from PIL import Image

from app.config import (
    BAILIAN_API_KEY,
    BAILIAN_BASE_URL,
    BAILIAN_IMAGE_JPEG_QUALITY,
    BAILIAN_IMAGE_MAX_EDGE,
    BAILIAN_MODEL,
    BAILIAN_RETRY_COUNT,
    BAILIAN_TIMEOUT_SECONDS,
    BEHAVIORS,
)
from app.fallback import classify_fallback
from app.image_utils import clamp_float, encode_jpeg_base64


logger = logging.getLogger(__name__)


def classify_bailian(image: Image.Image) -> tuple[bool, str, float]:
    if not BAILIAN_API_KEY:
        logger.warning("CV_PROVIDER=bailian but BAILIAN_API_KEY/DASHSCOPE_API_KEY is not configured")
        return False, "uncertain", 0.0

    payload = build_payload(image)
    url = BAILIAN_BASE_URL.rstrip("/") + "/chat/completions"
    last_error: Exception | None = None

    for attempt in range(1, max(1, BAILIAN_RETRY_COUNT) + 1):
        try:
            content = post_chat_completion(url, payload)
            return parse_bailian_result(content)
        except (httpx.HTTPError, KeyError, IndexError, TypeError, ValueError) as exc:
            last_error = exc
            logger.warning("Bailian vision classification failed on attempt %s: %s", attempt, exc)

    logger.warning("Bailian vision classification exhausted retries: %s", last_error)
    return classify_fallback(image)


def build_payload(image: Image.Image) -> dict[str, Any]:
    image_url = "data:image/jpeg;base64," + encode_jpeg_base64(
        image,
        max_edge=BAILIAN_IMAGE_MAX_EDGE,
        jpeg_quality=BAILIAN_IMAGE_JPEG_QUALITY,
    )
    return {
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
                    {"type": "text", "text": behavior_prompt()},
                    {"type": "image_url", "image_url": {"url": image_url}},
                ],
            },
        ],
        "temperature": 0,
    }


def behavior_prompt() -> str:
    return (
        "Classify the visible pet behavior using exactly one label from: "
        "eating, drinking, exercising, sleeping, defecating, uncertain. "
        "Use eating only when the pet is clearly using food or a food bowl. "
        "Use drinking only when the pet is clearly using water or a water bowl. "
        "Use sleeping when the pet is lying down, curled up, resting, or asleep. "
        "Use defecating only near a litter box/toilet posture. "
        "Use exercising for walking, standing, sitting, playing, looking around, "
        "or any normal awake activity that is not eating, drinking, sleeping, or defecating. "
        "Use uncertain only if no pet is visible or the image is too unclear. "
        "Return JSON exactly like {\"found\":true,\"behavior\":\"exercising\",\"confidence\":0.72}."
    )


def post_chat_completion(url: str, payload: dict[str, Any]) -> Any:
    timeout = httpx.Timeout(BAILIAN_TIMEOUT_SECONDS, connect=10.0)
    limits = httpx.Limits(max_connections=4, max_keepalive_connections=0)
    with httpx.Client(timeout=timeout, limits=limits, http2=False, trust_env=False) as client:
        response = client.post(
            url,
            headers={
                "Authorization": f"Bearer {BAILIAN_API_KEY}",
                "Content-Type": "application/json",
                "Connection": "close",
            },
            json=payload,
        )
        response.raise_for_status()
    return response.json()["choices"][0]["message"]["content"]


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
        return False, "uncertain", 0.0
    return found, behavior, confidence
