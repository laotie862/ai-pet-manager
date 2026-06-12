import axios, { AxiosError, type AxiosRequestConfig } from 'axios'

import type { ApiResponse } from './types'
import { useAuthStore } from './stores/auth'

type BackendError = Partial<ApiResponse<unknown>>

export const api = axios.create({
  baseURL: '/api',
  timeout: 15000
})

api.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const auth = useAuthStore()
    const config = error.config as (AxiosRequestConfig & { _retry?: boolean }) | undefined
    if (error.response?.status === 401 && auth.refreshToken && config && !config._retry) {
      config._retry = true
      try {
        await auth.refresh()
        config.headers = {
          ...config.headers,
          Authorization: `Bearer ${auth.accessToken}`
        }
        return api(config)
      } catch {
        auth.logout()
      }
    }
    if (error.response?.status === 401) {
      auth.logout()
    }
    return Promise.reject(error)
  }
)

export async function unwrap<T>(request: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const response = await request
  if (response.data.code !== 'SUCCESS') {
    throw new Error(response.data.message || '请求失败')
  }
  return response.data.data
}

export function errorMessage(error: unknown, fallback = '操作失败') {
  if (axios.isAxiosError<BackendError>(error)) {
    if (error.response?.status === 401) {
      return '登录已过期，请重新登录'
    }
    const message = error.response?.data?.message
    if (message) {
      return message
    }
    if (error.response?.status) {
      return `${fallback}：HTTP ${error.response.status}`
    }
  }
  if (error instanceof Error) {
    return error.message
  }
  return fallback
}
