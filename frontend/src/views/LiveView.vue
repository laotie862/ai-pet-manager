<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { MonitorPlay, Play, RefreshCw, Signal, Video } from 'lucide-vue-next'
import { useSessionStore } from '../stores/session'
import { usePetcareStore } from '../stores/petcare'

const session = useSessionStore()
const store = usePetcareStore()
const frameSrc = ref('')
const status = ref('未连接')
const error = ref('')
const lastFrameAt = ref('')
let socket: WebSocket | null = null

const wsUrl = computed(() => {
  const device = store.currentDevice
  if (!device || !session.accessToken) return ''
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/devices/${device.id}/stream?token=${encodeURIComponent(session.accessToken)}`
})

async function prepare() {
  error.value = ''
  try {
    await Promise.all([store.loadPets(), store.loadDevices()])
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : '加载失败'
  }
}

async function startLive() {
  error.value = ''
  if (!store.currentDevice) {
    error.value = '请先创建设备'
    return
  }
  try {
    await store.startDevice(store.currentDevice.id)
  } catch {
    // WebSocket handler will try to start stream too.
  }
  connect()
}

function connect() {
  closeSocket()
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
      frameSrc.value = `data:image/jpeg;base64,${payload.imageBase64}`
    }
  }
  socket.onerror = () => {
    status.value = '连接异常'
  }
  socket.onclose = () => {
    status.value = '已断开'
  }
}

function closeSocket() {
  if (socket) {
    socket.close()
    socket = null
  }
}

onMounted(prepare)
onBeforeUnmount(closeSocket)
</script>

<template>
  <section class="page-stack">
    <header class="page-header">
      <div>
        <span class="eyebrow">Live</span>
        <h1>实时视频</h1>
      </div>
      <div class="toolbar">
        <button class="toolbar-button" @click="prepare"><RefreshCw :size="17" />刷新</button>
        <button class="primary-action compact" @click="startLive"><Play :size="17" />开始预览</button>
      </div>
    </header>

    <p v-if="error" class="error-line">{{ error }}</p>

    <section class="live-layout">
      <div class="video-shell large">
        <div class="video-status">
          <MonitorPlay :size="18" />
          <span>{{ store.currentDevice?.name || '未选择设备' }}</span>
          <strong>{{ status }}</strong>
        </div>
        <img v-if="frameSrc" :src="frameSrc" alt="设备实时画面" />
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
          <p><span>设备</span><strong>{{ store.currentDevice?.name || '-' }}</strong></p>
          <p><span>状态</span><strong>{{ status }}</strong></p>
          <p><span>最近帧</span><strong>{{ lastFrameAt ? new Date(lastFrameAt).toLocaleTimeString() : '-' }}</strong></p>
          <p><span>宠物</span><strong>{{ store.currentPet?.name || '-' }}</strong></p>
        </div>
      </aside>
    </section>
  </section>
</template>
