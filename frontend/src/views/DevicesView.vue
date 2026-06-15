<script setup lang="ts">
import { Check, Play, Plus, RefreshCw, Save, Square, Trash2, Video } from 'lucide-vue-next'
import { computed, onMounted, reactive, ref } from 'vue'

import { errorMessage } from '../api'
import { useWorkspaceStore } from '../stores/workspace'

const workspace = useWorkspaceStore()
const loading = ref(false)
const savingBindings = ref<number | null>(null)
const error = ref('')
const bindingDrafts = reactive<Record<number, number[]>>({})
const form = reactive({
  name: '循环视频设备',
  rtspUrl: 'video://loop?path=/data/raw-videos/测试视频.mp4',
  username: '',
  password: ''
})

const currentPetId = computed(() => workspace.currentPet?.id)

function statusText(status: string) {
  return { ONLINE: '在线', OFFLINE: '离线', ANALYZING: '分析中', ERROR: '异常' }[status] || status
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    await Promise.all([workspace.loadPets(), workspace.loadDevices()])
    syncBindingDrafts()
  } catch (err) {
    error.value = errorMessage(err, '加载失败')
  } finally {
    loading.value = false
  }
}

async function createDevice() {
  error.value = ''
  if (!currentPetId.value) {
    error.value = '请先创建并选择宠物'
    return
  }
  try {
    await workspace.createDevice({ ...form, petId: currentPetId.value })
    syncBindingDrafts()
  } catch (err) {
    error.value = errorMessage(err, '创建设备失败')
  }
}

function syncBindingDrafts() {
  for (const device of workspace.devices) {
    const boundPets = workspace.devicePetBindings[device.id] || []
    bindingDrafts[device.id] = boundPets.length ? boundPets.map((pet) => pet.id) : device.petId ? [device.petId] : []
  }
}

function bindingNames(deviceId: number) {
  const pets = workspace.devicePetBindings[deviceId] || []
  return pets.length ? pets.map((pet) => pet.name).join('、') : '未绑定'
}

async function saveBinding(deviceId: number) {
  const petIds = bindingDrafts[deviceId] || []
  if (!petIds.length) {
    error.value = '设备至少需要绑定一只宠物'
    return
  }
  savingBindings.value = deviceId
  error.value = ''
  try {
    await workspace.replaceDevicePets(deviceId, petIds)
    syncBindingDrafts()
  } catch (err) {
    error.value = errorMessage(err, '保存绑定失败')
  } finally {
    savingBindings.value = null
  }
}

async function startDevice(id: number) {
  error.value = ''
  try {
    workspace.setCurrentDevice(id)
    await workspace.startDevice(id)
    await workspace.loadDevices()
  } catch (err) {
    error.value = errorMessage(err, '启动失败')
  }
}

async function stopDevice(id: number) {
  error.value = ''
  try {
    await workspace.stopDevice(id)
    await workspace.loadDevices()
  } catch (err) {
    error.value = errorMessage(err, '停止失败')
  }
}

async function deleteDevice(id: number, name: string) {
  if (!window.confirm(`确定删除设备「${name}」吗？`)) return
  error.value = ''
  try {
    await workspace.deleteDevice(id)
    syncBindingDrafts()
  } catch (err) {
    error.value = errorMessage(err, '删除失败')
  }
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
        <strong>{{ workspace.currentPet?.name || '未选择' }}</strong>
      </article>
      <article class="selection-card primary">
        <span>当前设备</span>
        <strong>{{ workspace.currentDevice?.name || '未选择' }}</strong>
        <small>{{ workspace.currentDevice ? statusText(workspace.currentDevice.status) : '请在下方切换设备' }}</small>
      </article>
    </section>

    <section class="split-layout">
      <form class="panel form-panel" @submit.prevent="createDevice">
        <div class="panel-title">
          <h2>新增设备</h2>
          <Video :size="20" />
        </div>
        <label>设备名<input v-model="form.name" required /></label>
        <label>视频源<input v-model="form.rtspUrl" required /></label>
        <label>用户名<input v-model="form.username" placeholder="可选" /></label>
        <label>密码<input v-model="form.password" type="password" placeholder="可选" /></label>
        <button class="primary-action" type="submit"><Plus :size="18" />创建设备</button>
      </form>

      <article class="panel">
        <div class="panel-title">
          <h2>点击切换当前设备</h2>
          <span class="count-pill">{{ workspace.devices.length }}</span>
        </div>
        <div v-if="workspace.devices.length" class="device-card-list">
          <article v-for="device in workspace.devices" :key="device.id" class="device-card" :class="{ selected: workspace.currentDeviceId === device.id }">
            <button class="device-main" @click="workspace.setCurrentDevice(device.id)">
              <span class="status-dot" :class="device.status.toLowerCase()"></span>
              <span>
                <strong>{{ device.name }}</strong>
                <small>{{ statusText(device.status) }} · {{ device.rtspUrl }}</small>
                <small>绑定宠物：{{ bindingNames(device.id) }}</small>
              </span>
              <Check v-if="workspace.currentDeviceId === device.id" :size="18" />
            </button>
            <div class="binding-editor">
              <span>绑定宠物</span>
              <label v-for="pet in workspace.pets" :key="pet.id" class="checkbox-line">
                <input v-model="bindingDrafts[device.id]" type="checkbox" :value="pet.id" />
                {{ pet.name }}
              </label>
              <button class="toolbar-button compact" :disabled="savingBindings === device.id" @click="saveBinding(device.id)">
                <Save :size="15" />
                {{ savingBindings === device.id ? '保存中' : '保存绑定' }}
              </button>
            </div>
            <div class="row-actions">
              <button class="toolbar-button" @click="startDevice(device.id)"><Play :size="16" />切换并启动</button>
              <button class="toolbar-button" @click="stopDevice(device.id)"><Square :size="16" />停止</button>
              <button class="danger-action compact" @click="deleteDevice(device.id, device.name)"><Trash2 :size="16" />删除</button>
            </div>
          </article>
        </div>
        <p v-else class="empty-state">{{ loading ? '加载中...' : '暂无设备' }}</p>
      </article>
    </section>
  </section>
</template>
