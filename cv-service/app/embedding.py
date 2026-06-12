import math
from functools import lru_cache

from PIL import Image, ImageStat

from app.config import (
    IDENTITY_CLIP_DEVICE,
    IDENTITY_CLIP_MODEL,
    IDENTITY_CLIP_PRETRAINED,
    IDENTITY_EMBEDDER,
    IDENTITY_MODEL_VERSION,
)


def extract_identity_embedding(image: Image.Image) -> tuple[list[float], str]:
    if IDENTITY_EMBEDDER == "clip":
        try:
            return extract_clip_embedding(image), IDENTITY_MODEL_VERSION
        except Exception:
            # Keep the product flow alive if CLIP weights are unavailable locally.
            return extract_color_embedding(image), "local-color-identity-v1"
    return extract_color_embedding(image), "local-color-identity-v1"


def extract_color_embedding(image: Image.Image) -> list[float]:
    image = image.convert("RGB").resize((96, 96))
    pixels = list(image.getdata())
    total_pixels = max(1, len(pixels))
    features: list[float] = []

    # Coat color is the strongest low-cost signal before a real ReID model is introduced.
    for channel_index in range(3):
        histogram = [0.0] * 16
        for pixel in pixels:
            histogram[min(15, pixel[channel_index] // 16)] += 1.0
        features.extend(value / total_pixels for value in histogram)

    # Coarse spatial averages help distinguish pets with similar global colors.
    for row in range(4):
        for col in range(4):
            crop = image.crop((col * 24, row * 24, (col + 1) * 24, (row + 1) * 24))
            stat = ImageStat.Stat(crop)
            features.extend(value / 255.0 for value in stat.mean)

    gray = image.convert("L")
    gray_pixels = list(gray.getdata())
    mean_gray = sum(gray_pixels) / total_pixels
    variance = sum((value - mean_gray) ** 2 for value in gray_pixels) / total_pixels
    features.append(mean_gray / 255.0)
    features.append(math.sqrt(variance) / 255.0)

    return normalize(features)


def extract_clip_embedding(image: Image.Image) -> list[float]:
    import torch

    model, _, preprocess = load_clip_model()
    tensor = preprocess(image.convert("RGB")).unsqueeze(0).to(IDENTITY_CLIP_DEVICE)
    with torch.no_grad():
        features = model.encode_image(tensor)
        features = features / features.norm(dim=-1, keepdim=True)
    return [round(float(value), 6) for value in features[0].cpu().tolist()]


@lru_cache(maxsize=1)
def load_clip_model():
    import open_clip

    model, _, preprocess = open_clip.create_model_and_transforms(
        IDENTITY_CLIP_MODEL,
        pretrained=IDENTITY_CLIP_PRETRAINED,
        device=IDENTITY_CLIP_DEVICE,
    )
    model.eval()
    return model, None, preprocess


def normalize(features: list[float]) -> list[float]:
    norm = math.sqrt(sum(value * value for value in features))
    if norm == 0:
        return [0.0 for _ in features]
    return [round(value / norm, 6) for value in features]
