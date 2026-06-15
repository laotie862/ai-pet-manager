<script setup lang="ts">
import { BellRing, Save, Send } from 'lucide-vue-next'
import { onMounted, reactive, ref } from 'vue'

const STORAGE_KEY = 'petcare.pushSettings'
const saved = ref(false)
const form = reactive({
  wecomUserId: '',
  dailyReport: true,
  alerts: true,
  greetings: true,
  quietStart: '22:30',
  quietEnd: '08:00'
})

function load() {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return
  try {
    Object.assign(form, JSON.parse(raw))
  } catch {
    localStorage.removeItem(STORAGE_KEY)
  }
}

function save() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(form))
  saved.value = true
  window.setTimeout(() => {
    saved.value = false
  }, 1800)
}

onMounted(load)
</script>

<template>
  <section class="page-stack">
    <header class="page-header">
      <div>
        <span class="eyebrow">Push</span>
        <h1>推送设置</h1>
      </div>
      <button class="primary-action" @click="save">
        <Save :size="17" />
        保存
      </button>
    </header>

    <p v-if="saved" class="success-line">推送偏好已保存。</p>

    <section class="settings-layout">
      <article class="panel">
        <div class="panel-title">
          <h2>企业微信接收</h2>
          <Send :size="20" />
        </div>
        <label class="field">
          <span>企业微信 UserID / External UserID</span>
          <input v-model.trim="form.wecomUserId" placeholder="后续由绑定接口回填" />
        </label>
        <div class="form-grid">
          <label class="field">
            <span>免打扰开始</span>
            <input v-model="form.quietStart" type="time" />
          </label>
          <label class="field">
            <span>免打扰结束</span>
            <input v-model="form.quietEnd" type="time" />
          </label>
        </div>
      </article>

      <article class="panel">
        <div class="panel-title">
          <h2>接收类型</h2>
          <BellRing :size="20" />
        </div>
        <div class="toggle-list">
          <label><input v-model="form.dailyReport" type="checkbox" /> 每日报告</label>
          <label><input v-model="form.alerts" type="checkbox" /> 行为告警</label>
          <label><input v-model="form.greetings" type="checkbox" /> 节日祝福</label>
        </div>
      </article>
    </section>
  </section>
</template>
