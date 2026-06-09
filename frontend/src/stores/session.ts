import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { apiData, http } from '../api/http'
import type { TokenResponse, User } from '../api/types'

const ACCESS_KEY = 'petcare.accessToken'
const REFRESH_KEY = 'petcare.refreshToken'
const USER_KEY = 'petcare.user'

export const useSessionStore = defineStore('session', () => {
  const accessToken = ref(localStorage.getItem(ACCESS_KEY) || '')
  const refreshToken = ref(localStorage.getItem(REFRESH_KEY) || '')
  const user = ref<User | null>(readUser())
  const isAuthed = computed(() => Boolean(accessToken.value))

  function applyToken(token: TokenResponse) {
    accessToken.value = token.accessToken
    refreshToken.value = token.refreshToken
    user.value = token.user
    localStorage.setItem(ACCESS_KEY, token.accessToken)
    localStorage.setItem(REFRESH_KEY, token.refreshToken)
    localStorage.setItem(USER_KEY, JSON.stringify(token.user))
  }

  async function login(account: string, password: string) {
    const token = await apiData<TokenResponse>(http.post('/auth/login', { account, password }))
    applyToken(token)
  }

  async function register(email: string, password: string, nickname: string, phone?: string) {
    const token = await apiData<TokenResponse>(http.post('/auth/register', { email, password, nickname, phone }))
    applyToken(token)
  }

  async function refresh() {
    const token = await apiData<TokenResponse>(http.post('/auth/refresh', { refreshToken: refreshToken.value }))
    applyToken(token)
  }

  function logout() {
    accessToken.value = ''
    refreshToken.value = ''
    user.value = null
    localStorage.removeItem(ACCESS_KEY)
    localStorage.removeItem(REFRESH_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { accessToken, refreshToken, user, isAuthed, login, register, refresh, logout }
})

function readUser(): User | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}
