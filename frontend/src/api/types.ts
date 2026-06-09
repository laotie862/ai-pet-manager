export interface User {
  id: number
  email: string
  phone?: string
  nickname: string
  role: string
  status: string
  vip: boolean
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
}

export interface Device {
  id: number
  petId?: number
  name: string
  rtspUrl: string
  username?: string
  status: string
  streamPath: string
  lastError?: string
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

export interface BehaviorEvent {
  id: number
  petId: number
  deviceId: number
  behaviorType: string
  confidence: number
  found: boolean
  startedAt: string
  endedAt?: string
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
