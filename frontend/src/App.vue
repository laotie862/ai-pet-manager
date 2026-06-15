<script setup lang="ts">
import { Activity, Bell, Camera, FileText, Fingerprint, Home, LogOut, PawPrint, Radio, Settings } from 'lucide-vue-next'
import { useRouter } from 'vue-router'

import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
const router = useRouter()

const navItems = [
  { to: '/', label: '主页', icon: Home },
  { to: '/pets', label: '宠物信息', icon: PawPrint },
  { to: '/devices', label: '设备切换', icon: Camera },
  { to: '/live', label: '实时视频', icon: Radio },
  { to: '/behavior', label: '行为记录', icon: Activity },
  { to: '/identity', label: '身份识别', icon: Fingerprint },
  { to: '/report', label: '日报', icon: FileText },
  { to: '/messages', label: '消息', icon: Bell },
  { to: '/settings/push', label: '推送', icon: Settings }
]

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <section class="app-shell" :class="{ 'top-shell': auth.isAuthed }">
    <header v-if="auth.isAuthed" class="topbar">
      <RouterLink class="brand top-brand" to="/">
        <span class="brand-mark"><PawPrint :size="24" /></span>
        <span>
          <strong>AI 宠物管家</strong>
          <small>行为识别工作台</small>
        </span>
      </RouterLink>

      <nav class="top-nav" aria-label="主导航">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to">
          <component :is="item.icon" :size="18" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="top-user">
        <PawPrint :size="18" />
        <span>{{ auth.user?.nickname || auth.user?.email }}</span>
        <button class="icon-button" title="退出登录" @click="logout">
          <LogOut :size="17" />
        </button>
      </div>
    </header>

    <main class="main-panel">
      <RouterView />
    </main>
  </section>
</template>
