import base64
import binascii
import io
from typing import Any

from PIL import Image, UnidentifiedImageError


def decode_image(image_base64: str) -> Image.Image | None:
    try:
        raw = base64.b64decode(image_base64, validate=True)
        image = Image.open(io.BytesIO(raw))
        image.load()
        return image.convert("RGB")
    except (binascii.Error, UnidentifiedImageError, OSError, ValueError):
        return None


def encode_jpeg_base64(image: Image.Image, max_edge: int, jpeg_quality: int) -> str:
    image = image.convert("RGB")
    width, height = image.size
    longest_edge = max(width, height)
    if longest_edge > max_edge:
        scale = max_edge / longest_edge
        image = image.resize((max(1, int(width * scale)), max(1, int(height * scale))))

    buffer = io.BytesIO()
    image.save(buffer, format="JPEG", quality=jpeg_quality, optimize=True)
    return base64.b64encode(buffer.getvalue()).decode("ascii")


def clamp_float(value: Any, minimum: float, maximum: float) -> float:
    try:
        number = float(value)
    except (TypeError, ValueError):
        return minimum
    return max(minimum, min(maximum, number))
