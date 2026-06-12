<script setup lang="ts">
import { Radio, RefreshCw, Signal, Video } from 'lucide-vue-next'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

import { errorMessage } from '../api'
import { useAuthStore } from '../stores/auth'
import { useWorkspaceStore } from '../stores/workspace'

const auth = useAuthStore()
const workspace = useWorkspaceStore()
const imageSrc = ref('')
const status = ref('未连接')
const error = ref('')
const lastFrameAt = ref('')
let socket: WebSocket | null = null

const wsUrl = computed(() => {
  const device = workspace.currentDevice
  if (!device || !auth.accessToken) return ''
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/devices/${device.id}/stream?token=${encodeURIComponent(auth.accessToken)}`
})

async function load() {
  error.value = ''
  try {
    await Promise.all([workspace.loadPets(), workspace.loadDevices()])
  } catch (err) {
    error.value = errorMessage(err, '加载失败')
  }
}

async function reconnectPreview() {
  error.value = ''
  if (!workspace.currentDevice) {
    error.value = '请先创建设备'
    return
  }
  connect()
}

function connect() {
  disconnect()
  if (!wsUrl.value) return
  status.value = '连接中'
  socket = new WebSocket(wsUrl.value)
  socket.onopen = () => {
    status.value = '已连接'
  }
  socket.onmessage = (event) => {
    const payload = JSON.parse(event.data)
    status.value = `${payload.status || 'UNKNOWN'}${payload.frameReady ? ' · 有画面' : ' · 等待画面'}`
    lastFrameAt.value = payload.lastFrameAt || payload.serverTime || ''
    if (payload.imageBase64) {
      imageSrc.value = `data:image/jpeg;base64,${payload.imageBase64}`
    }
  }
  socket.onerror = () => {
    status.value = '连接异常'
  }
  socket.onclose = () => {
    status.value = '已断开'
  }
}

function disconnect() {
  if (socket) {
    socket.close()
    socket = null
  }
}

watch(
  () => workspace.currentDevice?.id,
  () => {
    if (workspace.currentDevice && auth.accessToken) {
      connect()
    }
  }
)

onMounted(async () => {
  await load()
  connect()
})
onBeforeUnmount(disconnect)
</script>

<template>
  <section class="page-stack">
    <header class="page-header">
      <div>
        <span class="eyebrow">Live</span>
        <h1>实时视频</h1>
      </div>
      <div class="toolbar">
        <button class="toolbar-button" @click="load"><RefreshCw :size="17" />刷新</button>
        <button class="primary-action compact" @click="reconnectPreview"><Radio :size="17" />重新连接</button>
      </div>
    </header>

    <p v-if="error" class="error-line">{{ error }}</p>

    <section class="live-layout">
      <div class="video-shell large">
        <div class="video-status">
          <Video :size="18" />
          <span>{{ workspace.currentDevice?.name || '未选择设备' }}</span>
          <strong>{{ status }}</strong>
        </div>
        <img v-if="imageSrc" :src="imageSrc" alt="设备实时画面" />
        <div v-else class="video-empty">
          <Video :size="42" />
          <span>等待视频帧</span>
        </div>
      </div>

      <aside class="panel live-side">
        <div class="panel-title">
          <h2>连接状态</h2>
          <Signal :size="20" />
        </div>
        <div class="info-list compact-list">
          <p><span>设备</span><strong>{{ workspace.currentDevice?.name || '-' }}</strong></p>
          <p><span>状态</span><strong>{{ status }}</strong></p>
          <p><span>最近帧</span><strong>{{ lastFrameAt ? new Date(lastFrameAt).toLocaleTimeString() : '-' }}</strong></p>
          <p><span>宠物</span><strong>{{ workspace.currentPet?.name || '-' }}</strong></p>
        </div>
      </aside>
    </section>
  </section>
</template>
