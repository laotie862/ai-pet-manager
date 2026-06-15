<script setup lang="ts">
import { Activity, ChartColumn, Clock3, RefreshCw } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'

import { errorMessage } from '../api'
import { useWorkspaceStore } from '../stores/workspace'

const workspace = useWorkspaceStore()
const loading = ref(false)
const error = ref('')

const behaviorType = computed(() => workspace.currentBehavior?.behaviorType || 'uncertain')
const behaviorText = computed(() => workspace.behaviorText[behaviorType.value] || '待确认')
const behaviorState = computed(() => {
  if (!workspace.currentBehavior || workspace.currentBehavior.behaviorType === 'uncertain') return '待确认'
  return workspace.currentBehavior.found ? '已识别' : '观察中'
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    await Promise.all([workspace.loadPets(), workspace.loadDevices()])
    if (workspace.currentPet) await workspace.loadBehavior(workspace.currentPet.id)
  } catch (err) {
    error.value = errorMessage(err, '加载失败')
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
        <span class="eyebrow">Behavior</span>
        <h1>行为识别</h1>
      </div>
      <button class="toolbar-button" :disabled="loading" @click="load">
        <RefreshCw :size="17" />
        {{ loading ? '刷新中' : '刷新' }}
      </button>
    </header>

    <p v-if="error" class="error-line">{{ error }}</p>

    <section class="behavior-layout">
      <article class="panel behavior-current polished">
        <span class="behavior-icon"><Activity :size="28" /></span>
        <span>当前行为</span>
        <strong>{{ behaviorText }}</strong>
        <small>{{ behaviorState }}</small>
        <small>{{ workspace.currentPet?.name || '未选择宠物' }}</small>
      </article>

      <article class="panel">
        <div class="panel-title">
          <h2>今日摘要</h2>
          <ChartColumn :size="20" />
        </div>
        <div v-if="workspace.summary" class="summary-grid">
          <div><span>进食</span><strong>{{ workspace.summary.eatingCount }}</strong><small>次</small></div>
          <div><span>饮水</span><strong>{{ workspace.summary.drinkingCount }}</strong><small>次</small></div>
          <div><span>活动</span><strong>{{ minutes(workspace.summary.exercisingSeconds) }}</strong><small>分钟</small></div>
          <div><span>睡眠</span><strong>{{ minutes(workspace.summary.sleepingSeconds) }}</strong><small>分钟</small></div>
        </div>
        <p v-else class="empty-state">暂无摘要。</p>
      </article>
    </section>

    <article class="panel">
      <div class="panel-title">
        <h2>行为时间线</h2>
        <Clock3 :size="20" />
      </div>
      <div v-if="workspace.timeline.length" class="timeline rich">
        <p v-for="event in workspace.timeline" :key="event.id">
          <span class="timeline-dot"></span>
          <strong>{{ workspace.behaviorText[event.behaviorType] || event.behaviorType }}</strong>
          <span>{{ new Date(event.startedAt).toLocaleString() }}</span>
          <small>{{ event.endedAt ? '已结束' : '进行中' }}</small>
        </p>
      </div>
      <p v-else class="empty-state">{{ loading ? '加载中...' : '暂无行为事件' }}</p>
    </article>
  </section>
</template>
