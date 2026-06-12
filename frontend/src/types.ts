export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  traceId?: string
}

export interface User {
  id: number
  email?: string
  phone?: string
  nickname: string
  role: string
  status: string
  vipUntil?: string
}

export interface TokenResponse {
  tokenType: string
  accessToken: string
  expiresIn: number
  refreshToken: string
  user: User
}

export interface Pet {
  id: number
  name: string
  species: string
  breed?: string
  gender?: string
  birthday?: string
  avatarUrl?: string
  weightKg?: number
  createdAt?: string
  updatedAt?: string
}

export interface PetRequest {
  name: string
  species: string
  breed?: string
  gender?: string
  birthday?: string
  avatarUrl?: string
  weightKg?: number
}

export interface Device {
  id: number
  petId?: number
  boundPets?: Pet[]
  name: string
  rtspUrl: string
  username?: string
  status: string
  streamPath?: string
  lastOnlineAt?: string
  lastHeartbeatAt?: string
  lastError?: string
}

export interface DeviceRequest {
  name: string
  petId?: number
  rtspUrl: string
  username?: string
  password?: string
}

export interface DeviceStatus {
  deviceId: number
  status: string
  running: boolean
  frameReady: boolean
  lastFrameAt?: string
  lastError?: string
}

export interface BehaviorCurrent {
  petId: number
  deviceId?: number
  behaviorType: string
  confidence: number
  found: boolean
  startedAt?: string
  lastSeenAt?: string
  modelVersion?: string
}

export interface BehaviorSummary {
  petId: number
  summaryDate: string
  eatingCount: number
  drinkingCount: number
  exercisingSeconds: number
  sleepingSeconds: number
  defecatingCount: number
  updatedAt?: string
}

export interface BehaviorEvent {
  id: number
  petId: number
  deviceId?: number
  behaviorType: string
  confidence: number
  found: boolean
  startedAt: string
  endedAt?: string
  modelVersion?: string
}

export interface IdentityPhoto {
  id: number
  petId: number
  imageUrl: string
  modelVersion: string
  embeddingDimension: number
  createdAt: string
}

export interface IdentityMatch {
  petId: number
  petName: string
  identityPhotoId: number
  similarity: number
  matched: boolean
}
