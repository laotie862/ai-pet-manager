import axios from 'axios'
import { useSessionStore } from '../stores/session'

export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  traceId?: string
}

export const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const session = useSessionStore()
  if (session.accessToken) {
    config.headers.Authorization = `Bearer ${session.accessToken}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const session = useSessionStore()
    const original = error.config
    if (error.response?.status === 401 && session.refreshToken && !original?._retry) {
      original._retry = true
      try {
        await session.refresh()
        original.headers.Authorization = `Bearer ${session.accessToken}`
        return http(original)
      } catch {
        session.logout()
      }
    }
    return Promise.reject(error)
  }
)

export async function apiData<T>(request: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const response = await request
  if (response.data.code !== 'SUCCESS') {
    throw new Error(response.data.message || '请求失败')
  }
  return response.data.data
}
