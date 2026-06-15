import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { api, unwrap } from '../api'
import type { TokenResponse, User } from '../types'

const ACCESS_TOKEN_KEY = 'petcare.accessToken'
const REFRESH_TOKEN_KEY = 'petcare.refreshToken'
const USER_KEY = 'petcare.user'

function loadUser(): User | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as User
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem(ACCESS_TOKEN_KEY) || '')
  const refreshToken = ref(localStorage.getItem(REFRESH_TOKEN_KEY) || '')
  const user = ref<User | null>(loadUser())
  const isAuthed = computed(() => Boolean(accessToken.value))

  function applyToken(token: TokenResponse) {
    accessToken.value = token.accessToken
    refreshToken.value = token.refreshToken
    user.value = token.user
    localStorage.setItem(ACCESS_TOKEN_KEY, token.accessToken)
    localStorage.setItem(REFRESH_TOKEN_KEY, token.refreshToken)
    localStorage.setItem(USER_KEY, JSON.stringify(token.user))
  }

  async function login(account: string, password: string) {
    const token = await unwrap<TokenResponse>(api.post('/auth/login', { account, password }))
    applyToken(token)
  }

  async function register(email: string, password: string, nickname: string, phone?: string) {
    const token = await unwrap<TokenResponse>(api.post('/auth/register', { email, password, nickname, phone }))
    applyToken(token)
  }

  async function refresh() {
    const token = await unwrap<TokenResponse>(api.post('/auth/refresh', { refreshToken: refreshToken.value }))
    applyToken(token)
  }

  function logout() {
    accessToken.value = ''
    refreshToken.value = ''
    user.value = null
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { accessToken, refreshToken, user, isAuthed, login, register, refresh, logout }
})
