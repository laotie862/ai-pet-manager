<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Activity, BarChart3, RefreshCw, Timer } from 'lucide-vue-next'
import { behaviorText, usePetcareStore } from '../stores/petcare'

const store = usePetcareStore()
const loading = ref(false)
const error = ref('')

const behaviorKey = computed(() => store.currentBehavior?.behaviorType || 'uncertain')
const behaviorLabel = computed(() => behaviorText[behaviorKey.value] || '待确认')
const recognitionStatus = computed(() => {
  if (!store.currentBehavior || store.currentBehavior.behaviorType === 'uncertain') return '待确认'
  return store.currentBehavior.found ? '已识别' : '观察中'
})

async function refresh() {
  error.value = ''
  loading.value = true
  try {
    await Promise.all([store.loadPets(), store.loadDevices()])
    if (store.currentPet) {
      await store.loadBehavior(store.currentPet.id)
    }
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function minutes(seconds?: number) {
  return Math.round((seconds || 0) / 60)
}

onMounted(refresh)
</script>

<template>
  <section class="page-stack">
    <header class="page-header">
      <div>
        <span class="eyebrow">Behavior</span>
        <h1>行为识别</h1>
      </div>
      <button class="toolbar-button" :disabled="loading" @click="refresh">
        <RefreshCw :size="17" />
        {{ loading ? '刷新中' : '刷新' }}
      </button>
    </header>

    <p v-if="error" class="error-line">{{ error }}</p>

    <section class="behavior-layout">
      <article class="panel behavior-current polished">
        <div class="behavior-icon"><Activity :size="28" /></div>
        <span>当前行为</span>
        <strong>{{ behaviorLabel }}</strong>
        <small>{{ recognitionStatus }}</small>
        <small>{{ store.currentPet?.name || '未选择宠物' }}</small>
      </article>

      <article class="panel">
        <div class="panel-title">
          <h2>今日摘要</h2>
          <BarChart3 :size="20" />
        </div>
        <div v-if="store.summary" class="summary-grid">
          <div><span>进食</span><strong>{{ store.summary.eatingCount }}</strong><small>次</small></div>
          <div><span>饮水</span><strong>{{ store.summary.drinkingCount }}</strong><small>次</small></div>
          <div><span>活动</span><strong>{{ minutes(store.summary.exercisingSeconds) }}</strong><small>分钟</small></div>
          <div><span>睡眠</span><strong>{{ minutes(store.summary.sleepingSeconds) }}</strong><small>分钟</small></div>
        </div>
        <p v-else class="empty-state">暂无摘要。</p>
      </article>
    </section>

    <article class="panel">
      <div class="panel-title">
        <h2>行为时间线</h2>
        <Timer :size="20" />
      </div>
      <div v-if="store.timeline.length" class="timeline rich">
        <p v-for="event in store.timeline" :key="event.id">
          <span class="timeline-dot"></span>
          <strong>{{ behaviorText[event.behaviorType] || event.behaviorType }}</strong>
          <span>{{ new Date(event.startedAt).toLocaleString() }}</span>
          <small>{{ event.endedAt ? '已结束' : '进行中' }}</small>
        </p>
      </div>
      <p v-else class="empty-state">{{ loading ? '加载中...' : '暂无行为事件' }}</p>
    </article>
  </section>
</template>
