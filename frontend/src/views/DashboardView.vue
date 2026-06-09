<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { Activity, Camera, Clock3, HeartPulse, MonitorPlay, PawPrint, RefreshCw, Settings2 } from 'lucide-vue-next'
import { behaviorText, usePetcareStore } from '../stores/petcare'

const store = usePetcareStore()
const loading = ref(false)
const error = ref('')

const currentBehaviorText = computed(() => {
  const key = store.currentBehavior?.behaviorType || 'uncertain'
  return behaviorText[key] || '待确认'
})
const onlineDevices = computed(() => store.devices.filter((device) => ['ONLINE', 'ANALYZING'].includes(device.status)).length)
const behaviorStatus = computed(() => {
  if (!store.currentBehavior || store.currentBehavior.behaviorType === 'uncertain') return '待确认'
  return store.currentBehavior.found ? '已识别' : '观察中'
})

const shortcuts = [
  { to: '/pets', title: '宠物信息', desc: '查看、修改宠物档案', icon: PawPrint },
  { to: '/devices', title: '设备切换', desc: '选择当前摄像头或视频源', icon: Settings2 },
  { to: '/live', title: '实时视频', desc: '打开视频流预览', icon: MonitorPlay },
  { to: '/behavior', title: '行为记录', desc: '查看当前行为和时间线', icon: Activity }
]

async function refresh() {
  loading.value = true
  error.value = ''
  try {
    await store.loadAll()
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
    <header class="dashboard-hero">
      <div class="hero-copy">
        <span class="eyebrow">Home</span>
        <h1>{{ store.currentPet ? `${store.currentPet.name} 的看护主页` : 'AI 宠物管家' }}</h1>
        <p>主页作为功能入口，下面的卡片可直接跳转到宠物信息、设备切换、视频和行为记录。</p>
      </div>
      <button class="toolbar-button light" :disabled="loading" @click="refresh">
        <RefreshCw :size="17" />
        {{ loading ? '刷新中' : '刷新' }}
      </button>
    </header>

    <p v-if="error" class="error-line">{{ error }}</p>

    <section class="shortcut-grid">
      <RouterLink v-for="item in shortcuts" :key="item.to" class="shortcut-card" :to="item.to">
        <component :is="item.icon" :size="24" />
        <strong>{{ item.title }}</strong>
        <span>{{ item.desc }}</span>
      </RouterLink>
    </section>

    <div class="metric-grid">
      <article class="metric-card accent-green">
        <PawPrint :size="22" />
        <span>当前宠物</span>
        <strong>{{ store.currentPet?.name || '未选择' }}</strong>
      </article>
      <article class="metric-card accent-blue">
        <Camera :size="22" />
        <span>当前设备</span>
        <strong>{{ store.currentDevice?.name || '未选择' }}</strong>
        <small>{{ onlineDevices }}/{{ store.devices.length }} 在线</small>
      </article>
      <article class="metric-card accent-rose">
        <Activity :size="22" />
        <span>当前行为</span>
        <strong>{{ currentBehaviorText }}</strong>
        <small>{{ behaviorStatus }}</small>
      </article>
    </div>

    <section class="dashboard-grid">
      <article class="panel">
        <div class="panel-title">
          <h2>今日摘要</h2>
          <Clock3 :size="20" />
        </div>
        <div v-if="store.summary" class="summary-grid">
          <div><span>进食</span><strong>{{ store.summary.eatingCount }}</strong><small>次</small></div>
          <div><span>饮水</span><strong>{{ store.summary.drinkingCount }}</strong><small>次</small></div>
          <div><span>活动</span><strong>{{ minutes(store.summary.exercisingSeconds) }}</strong><small>分钟</small></div>
          <div><span>睡眠</span><strong>{{ minutes(store.summary.sleepingSeconds) }}</strong><small>分钟</small></div>
        </div>
        <p v-else class="empty-state">暂无摘要，启动设备后会逐步生成。</p>
      </article>

      <article class="panel">
        <div class="panel-title">
          <h2>最近行为</h2>
          <HeartPulse :size="20" />
        </div>
        <div v-if="store.timeline.length" class="timeline clean">
          <p v-for="event in store.timeline.slice(0, 4)" :key="event.id">
            <strong>{{ behaviorText[event.behaviorType] || event.behaviorType }}</strong>
            <span>{{ new Date(event.startedAt).toLocaleString() }}</span>
            <small>{{ event.endedAt ? '已结束' : '进行中' }}</small>
          </p>
        </div>
        <p v-else class="empty-state">暂无行为事件。</p>
      </article>
    </section>
  </section>
</template>
