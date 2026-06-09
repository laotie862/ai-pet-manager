<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { Activity, Home, LogOut, MonitorPlay, PawPrint, Radio, UserRound } from 'lucide-vue-next'
import { useSessionStore } from './stores/session'

const session = useSessionStore()
const router = useRouter()
const isAuthed = computed(() => session.isAuthed)

function logout() {
  session.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app-shell top-shell">
    <header v-if="isAuthed" class="topbar">
      <RouterLink class="brand top-brand" to="/">
        <div class="brand-mark"><PawPrint :size="22" /></div>
        <div>
          <strong>AI 宠物管家</strong>
          <span>用户端 Web</span>
        </div>
      </RouterLink>

      <nav class="top-nav">
        <RouterLink to="/"><Home :size="18" />首页</RouterLink>
        <RouterLink to="/pets"><PawPrint :size="18" />宠物信息</RouterLink>
        <RouterLink to="/devices"><Radio :size="18" />设备切换</RouterLink>
        <RouterLink to="/live"><MonitorPlay :size="18" />实时视频</RouterLink>
        <RouterLink to="/behavior"><Activity :size="18" />行为记录</RouterLink>
      </nav>

      <div class="top-user">
        <UserRound :size="18" />
        <span>{{ session.user?.nickname || session.user?.email }}</span>
        <button class="icon-button" title="退出登录" @click="logout">
          <LogOut :size="17" />
        </button>
      </div>
    </header>

    <main class="main-panel">
      <RouterView />
    </main>
  </div>
</template>
