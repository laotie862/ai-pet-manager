<script setup lang="ts">
import { CalendarDays, CloudSun, FileText, RefreshCw } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'

import { errorMessage } from '../api'
import { useWorkspaceStore } from '../stores/workspace'

const workspace = useWorkspaceStore()
const loading = ref(false)
const error = ref('')
const selectedDate = ref(new Date().toISOString().slice(0, 10))

const totalEvents = computed(() => workspace.timeline.length)
const report = computed(() => workspace.dailyReport)

async function load() {
  loading.value = true
  error.value = ''
  try {
    await Promise.all([workspace.loadPets(), workspace.loadDevices()])
    if (workspace.currentPet) {
      await Promise.all([
        workspace.loadBehavior(workspace.currentPet.id, selectedDate.value),
        workspace.loadDailyReport(workspace.currentPet.id, selectedDate.value)
      ])
    }
  } catch (err) {
    error.value = errorMessage(err, '加载日报失败')
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
        <span class="eyebrow">Daily Report</span>
        <h1>每日报告</h1>
      </div>
      <div class="toolbar">
        <label class="date-picker">
          <CalendarDays :size="17" />
          <input v-model="selectedDate" type="date" @change="load" />
        </label>
        <button class="toolbar-button" :disabled="loading" @click="load">
          <RefreshCw :size="17" />
          {{ loading ? '刷新中' : '刷新' }}
        </button>
      </div>
    </header>

    <p v-if="error" class="error-line">{{ error }}</p>

    <section class="report-layout">
      <article class="panel report-copy">
        <div class="panel-title">
          <h2>日报文案</h2>
          <FileText :size="20" />
        </div>
        <p v-if="report">{{ report.content }}</p>
        <p v-else class="empty-state">{{ loading ? '正在生成日报...' : '暂无日报' }}</p>
        <small v-if="report" class="muted-line">
          {{ report.templateFallback ? '模板兜底生成' : report.modelVersion }}
        </small>
      </article>

      <article class="panel">
        <div class="panel-title">
          <h2>天气建议</h2>
          <CloudSun :size="20" />
        </div>
        <div v-if="report?.weather" class="weather-card">
          <strong>{{ report.weather.city }} / {{ report.weather.condition }}</strong>
          <span>{{ report.weather.temperatureCelsius ?? '未知' }}{{ report.weather.temperatureCelsius == null ? '' : '℃' }}</span>
          <small>{{ report.weather.advice }}</small>
        </div>
        <p v-else class="empty-state">暂无天气建议。</p>
      </article>
    </section>

    <article class="panel">
      <div class="panel-title">
        <h2>关键指标</h2>
        <span class="count-pill">{{ totalEvents }}</span>
      </div>
      <div v-if="workspace.summary" class="summary-grid">
        <div><span>进食</span><strong>{{ workspace.summary.eatingCount }}</strong><small>次</small></div>
        <div><span>饮水</span><strong>{{ workspace.summary.drinkingCount }}</strong><small>次</small></div>
        <div><span>活动</span><strong>{{ minutes(workspace.summary.exercisingSeconds) }}</strong><small>分钟</small></div>
        <div><span>睡眠</span><strong>{{ minutes(workspace.summary.sleepingSeconds) }}</strong><small>分钟</small></div>
      </div>
      <p v-else class="empty-state">{{ loading ? '加载中...' : '暂无摘要' }}</p>
    </article>

    <article class="panel">
      <div class="panel-title">
        <h2>当日事件</h2>
        <span class="count-pill">{{ workspace.timeline.length }}</span>
      </div>
      <div v-if="workspace.timeline.length" class="timeline rich">
        <p v-for="event in workspace.timeline" :key="event.id">
          <span class="timeline-dot"></span>
          <strong>{{ workspace.behaviorText[event.behaviorType] || event.behaviorType }}</strong>
          <span>{{ new Date(event.startedAt).toLocaleTimeString() }}</span>
          <small>{{ event.endedAt ? '已结束' : '进行中' }}</small>
        </p>
      </div>
      <p v-else class="empty-state">{{ loading ? '加载中...' : '暂无行为事件' }}</p>
    </article>
  </section>
</template>
