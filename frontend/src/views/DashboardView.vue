<script setup lang="ts">
import { Activity, Camera, ChartColumn, Fingerprint, PawPrint, Radio, RefreshCw } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'

import { errorMessage } from '../api'
import { useWorkspaceStore } from '../stores/workspace'

const workspace = useWorkspaceStore()
const loading = ref(false)
const error = ref('')

const currentBehaviorText = computed(() => {
  const behavior = workspace.currentBehavior?.behaviorType || 'uncertain'
  return workspace.behaviorText[behavior] || '待确认'
})
const onlineDevices = computed(() => workspace.devices.filter((device) => ['ONLINE', 'ANALYZING'].includes(device.status)).length)

const shortcuts = [
  { to: '/pets', title: '宠物信息', desc: '查看、修改宠物档案', icon: PawPrint },
  { to: '/devices', title: '设备切换', desc: '选择当前摄像头或视频源', icon: Camera },
  { to: '/live', title: '实时视频', desc: '打开视频流预览', icon: Radio },
  { to: '/behavior', title: '行为记录', desc: '查看当前行为和时间线', icon: Activity },
  { to: '/identity', title: '身份识别', desc: '上传身份照片并测试匹配', icon: Fingerprint }
]

async function load() {
  loading.value = true
  error.value = ''
  try {
    await workspace.loadAll()
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
    <article class="dashboard-hero">
      <div class="hero-copy">
        <span class="eyebrow">AI Pet Care</span>
        <h1>看护闭环工作台</h1>
        <p>用视频循环先跑通识别、事件、摘要和样本沉淀，再逐步接入真实摄像头。</p>
      </div>
      <button class="toolbar-button light" :disabled="loading" @click="load">
        <RefreshCw :size="17" />
        {{ loading ? '刷新中' : '刷新' }}
      </button>
    </article>

    <p v-if="error" class="error-line">{{ error }}</p>

    <section class="shortcut-grid">
      <RouterLink v-for="item in shortcuts" :key="item.to" class="shortcut-card" :to="item.to">
        <component :is="item.icon" :size="24" />
        <strong>{{ item.title }}</strong>
        <span>{{ item.desc }}</span>
      </RouterLink>
    </section>

    <section class="metric-grid">
      <article class="metric-card accent-green">
        <span>当前宠物</span>
        <strong>{{ workspace.currentPet?.name || '未创建' }}</strong>
      </article>
      <article class="metric-card accent-blue">
        <span>在线设备</span>
        <strong>{{ onlineDevices }} / {{ workspace.devices.length }}</strong>
      </article>
      <article class="metric-card accent-rose">
        <span>当前行为</span>
        <strong>{{ currentBehaviorText }}</strong>
      </article>
    </section>

    <section class="dashboard-grid">
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
        <p v-else class="empty-state">暂无摘要，启动设备后会逐步生成。</p>
      </article>

      <article class="panel">
        <div class="panel-title">
          <h2>最近行为</h2>
          <Activity :size="20" />
        </div>
        <div v-if="workspace.timeline.length" class="timeline clean">
          <p v-for="event in workspace.timeline.slice(0, 4)" :key="event.id">
            <strong>{{ workspace.behaviorText[event.behaviorType] || event.behaviorType }}</strong>
            <span>{{ new Date(event.startedAt).toLocaleString() }}</span>
            <small>{{ event.endedAt ? '已结束' : '进行中' }}</small>
          </p>
        </div>
        <p v-else class="empty-state">暂无行为事件。</p>
      </article>
    </section>
  </section>
</template>
