from typing import Any

from pydantic import BaseModel, Field


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


class EmbedRequest(BaseModel):
    image_base64: str = Field(min_length=1)


class EmbedResponse(BaseModel):
    embedding: list[float]
    model_version: str
    dimension: int
