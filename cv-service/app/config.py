import os


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
BAILIAN_TIMEOUT_SECONDS = float(os.getenv("BAILIAN_TIMEOUT_SECONDS", "30"))
BAILIAN_RETRY_COUNT = int(os.getenv("BAILIAN_RETRY_COUNT", "3"))
BAILIAN_IMAGE_MAX_EDGE = int(os.getenv("BAILIAN_IMAGE_MAX_EDGE", "768"))
BAILIAN_IMAGE_JPEG_QUALITY = int(os.getenv("BAILIAN_IMAGE_JPEG_QUALITY", "75"))

MODEL_VERSION = os.getenv("CV_MODEL_VERSION") or (
    f"bailian:{BAILIAN_MODEL}" if CV_PROVIDER == "bailian" else "local-placeholder"
)

IDENTITY_EMBEDDER = os.getenv("CV_IDENTITY_EMBEDDER", "clip").strip().lower()
IDENTITY_CLIP_MODEL = os.getenv("CV_IDENTITY_CLIP_MODEL", "ViT-B-32")
IDENTITY_CLIP_PRETRAINED = os.getenv("CV_IDENTITY_CLIP_PRETRAINED", "openai")
IDENTITY_CLIP_DEVICE = os.getenv("CV_IDENTITY_CLIP_DEVICE", "cpu")

IDENTITY_MODEL_VERSION = os.getenv("CV_IDENTITY_MODEL_VERSION") or (
    f"clip:{IDENTITY_CLIP_MODEL}:{IDENTITY_CLIP_PRETRAINED}"
    if IDENTITY_EMBEDDER == "clip"
    else "local-color-identity-v1"
)
