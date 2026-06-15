<script setup lang="ts">
import { LogIn, UserPlus } from 'lucide-vue-next'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { errorMessage } from '../api'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const mode = ref<'login' | 'register'>('login')
const account = ref('admin@example.com')
const email = ref('user@example.com')
const nickname = ref('宠物家长')
const phone = ref('')
const password = ref('Admin@123456')
const loading = ref(false)
const error = ref('')

async function submit() {
  loading.value = true
  error.value = ''
  try {
    if (mode.value === 'login') {
      await auth.login(account.value, password.value)
    } else {
      await auth.register(email.value, password.value, nickname.value, phone.value || undefined)
    }
    router.push('/')
  } catch (err) {
    error.value = errorMessage(err)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-page">
    <div class="auth-panel">
      <div class="auth-copy">
        <span class="eyebrow">AI Pet Care</span>
        <h1>AI 宠物管家</h1>
        <p>从视频流到行为识别，再到看护记录和训练样本，先把完整产品闭环跑起来。</p>
      </div>

      <form class="auth-form" @submit.prevent="submit">
        <div class="segmented">
          <button type="button" :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
          <button type="button" :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
        </div>

        <label v-if="mode === 'login'">
          账号
          <input v-model="account" autocomplete="username" placeholder="邮箱或手机号" />
        </label>

        <template v-else>
          <label>
            邮箱
            <input v-model="email" autocomplete="email" placeholder="user@example.com" />
          </label>
          <label>
            昵称
            <input v-model="nickname" placeholder="宠物家长" />
          </label>
          <label>
            手机号
            <input v-model="phone" placeholder="可选" />
          </label>
        </template>

        <label>
          密码
          <input v-model="password" autocomplete="current-password" type="password" />
        </label>

        <p v-if="error" class="error-line">{{ error }}</p>

        <button class="primary-action" type="submit" :disabled="loading">
          <LogIn v-if="mode === 'login'" :size="18" />
          <UserPlus v-else :size="18" />
          {{ loading ? '处理中...' : mode === 'login' ? '进入系统' : '创建账号' }}
        </button>
      </form>
    </div>
  </section>
</template>
