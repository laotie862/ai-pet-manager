<script setup lang="ts">
import { Bell, Camera, FileText, RefreshCw } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'

import { errorMessage } from '../api'
import { useWorkspaceStore } from '../stores/workspace'

const workspace = useWorkspaceStore()
const loading = ref(false)
const error = ref('')

const messages = computed(() => {
  const items = []
  const petName = workspace.currentPet?.name || '当前宠物'
  const behavior = workspace.currentBehavior?.behaviorType || 'uncertain'
  if (workspace.currentBehavior && behavior !== 'uncertain') {
    items.push({
      icon: Bell,
      title: `${petName} 正在${workspace.behaviorText[behavior] || behavior}`,
      desc: workspace.currentBehavior.startedAt ? `开始于 ${new Date(workspace.currentBehavior.startedAt).toLocaleString()}` : '已识别到当前行为',
      tag: '行为'
    })
  }
  if (workspace.summary) {
    items.push({
      icon: FileText,
      title: `${petName} 今日摘要已更新`,
      desc: `进食 ${workspace.summary.eatingCount} 次，饮水 ${workspace.summary.drinkingCount} 次，活动 ${minutes(workspace.summary.exercisingSeconds)} 分钟`,
      tag: '日报'
    })
  }
  for (const device of workspace.devices) {
    if (device.status === 'ERROR' || device.lastError) {
      items.push({
        icon: Camera,
        title: `${device.name} 需要检查`,
        desc: device.lastError || `设备状态：${device.status}`,
        tag: '设备'
      })
    }
  }
  return items
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    await workspace.loadAll()
  } catch (err) {
    error.value = errorMessage(err, '加载消息失败')
  } finally {
    loading.value = false
  }
}

function minutes(seconds?: number) {
  return Math.round((seconds || 0) / 60)
}

onMounted(load)
</script>

<template>
  <section class="page-stack">
    <header class="page-header">
      <div>
        <span class="eyebrow">Messages</span>
        <h1>消息中心</h1>
      </div>
      <button class="toolbar-button" :disabled="loading" @click="load">
        <RefreshCw :size="17" />
        {{ loading ? '刷新中' : '刷新' }}
      </button>
    </header>

    <p v-if="error" class="error-line">{{ error }}</p>

    <article class="panel">
      <div class="panel-title">
        <h2>消息列表</h2>
        <span class="count-pill">{{ messages.length }}</span>
      </div>
      <div v-if="messages.length" class="message-list">
        <div v-for="(message, index) in messages" :key="index" class="message-row">
          <span class="message-icon"><component :is="message.icon" :size="20" /></span>
          <span>
            <strong>{{ message.title }}</strong>
            <small>{{ message.desc }}</small>
          </span>
          <em>{{ message.tag }}</em>
        </div>
      </div>
      <p v-else class="empty-state">{{ loading ? '加载中...' : '暂无消息' }}</p>
    </article>
  </section>
</template>
