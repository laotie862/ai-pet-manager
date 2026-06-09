<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Check, Play, Plus, Radio, RefreshCw, Square, Trash2 } from 'lucide-vue-next'
import { usePetcareStore } from '../stores/petcare'

const store = usePetcareStore()
const loading = ref(false)
const error = ref('')
const selectedPetId = computed(() => store.currentPet?.id)
const form = reactive({
  name: '循环视频设备',
  rtspUrl: 'video://loop?path=/data/videos/1a285088c966fc1b90f0df53b16a8675.mp4',
  username: '',
  password: ''
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    await Promise.all([store.loadPets(), store.loadDevices()])
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function createDevice() {
  error.value = ''
  if (!selectedPetId.value) {
    error.value = '请先创建并选择宠物'
    return
  }
  try {
    await store.createDevice({ ...form, petId: selectedPetId.value })
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : '创建设备失败'
  }
}

async function start(id: number) {
  error.value = ''
  try {
    store.setCurrentDevice(id)
    await store.startDevice(id)
    await store.loadDevices()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : '启动失败'
  }
}

async function stop(id: number) {
  error.value = ''
  try {
    await store.stopDevice(id)
    await store.loadDevices()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : '停止失败'
  }
}

async function removeDevice(id: number, name: string) {
  const ok = window.confirm(`确定删除设备「${name}」吗？`)
  if (!ok) return
  error.value = ''
  try {
    await store.deleteDevice(id)
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : '删除失败'
  }
}

function statusText(status: string) {
  const map: Record<string, string> = {
    ONLINE: '在线',
    OFFLINE: '离线',
    ANALYZING: '分析中',
    ERROR: '异常'
  }
  return map[status] || status
}

onMounted(load)
</script>

<template>
  <section class="page-stack">
    <header class="page-header">
      <div>
        <span class="eyebrow">Devices</span>
        <h1>设备切换</h1>
      </div>
      <button class="toolbar-button" :disabled="loading" @click="load">
        <RefreshCw :size="17" />
        {{ loading ? '刷新中' : '刷新' }}
      </button>
    </header>

    <p v-if="error" class="error-line">{{ error }}</p>

    <section class="current-selection">
      <article class="selection-card">
        <span>当前宠物</span>
        <strong>{{ store.currentPet?.name || '未选择' }}</strong>
      </article>
      <article class="selection-card primary">
        <span>当前设备</span>
        <strong>{{ store.currentDevice?.name || '未选择' }}</strong>
        <small>{{ store.currentDevice ? statusText(store.currentDevice.status) : '请在下方切换设备' }}</small>
      </article>
    </section>

    <section class="split-layout">
      <form class="panel form-panel" @submit.prevent="createDevice">
        <div class="panel-title">
          <h2>新增设备</h2>
          <Radio :size="20" />
        </div>
        <label>设备名<input v-model="form.name" required /></label>
        <label>视频源<input v-model="form.rtspUrl" required /></label>
        <label>用户名<input v-model="form.username" placeholder="可选" /></label>
        <label>密码<input v-model="form.password" type="password" placeholder="可选" /></label>
        <button class="primary-action" type="submit"><Plus :size="18" />创建设备</button>
      </form>

      <div class="panel">
        <div class="panel-title">
          <h2>点击切换当前设备</h2>
          <span class="count-pill">{{ store.devices.length }}</span>
        </div>
        <div v-if="store.devices.length" class="device-card-list">
          <article
            v-for="device in store.devices"
            :key="device.id"
            class="device-card"
            :class="{ selected: store.currentDeviceId === device.id }"
          >
            <button class="device-main" @click="store.setCurrentDevice(device.id)">
              <span class="status-dot" :class="device.status.toLowerCase()"></span>
              <span>
                <strong>{{ device.name }}</strong>
                <small>{{ statusText(device.status) }} · {{ device.rtspUrl }}</small>
              </span>
              <Check v-if="store.currentDeviceId === device.id" :size="18" />
            </button>
            <div class="row-actions">
              <button class="toolbar-button" @click="start(device.id)"><Play :size="16" />切换并启动</button>
              <button class="toolbar-button" @click="stop(device.id)"><Square :size="16" />停止</button>
              <button class="danger-action compact" @click="removeDevice(device.id, device.name)">
                <Trash2 :size="16" />删除
              </button>
            </div>
          </article>
        </div>
        <p v-else class="empty-state">{{ loading ? '加载中...' : '暂无设备' }}</p>
      </div>
    </section>
  </section>
</template>
