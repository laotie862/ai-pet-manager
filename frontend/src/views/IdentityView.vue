<script setup lang="ts">
import { Fingerprint, ImagePlus, RefreshCw, ScanSearch, Trash2 } from 'lucide-vue-next'
import { onMounted, ref } from 'vue'

import { errorMessage } from '../api'
import { useWorkspaceStore } from '../stores/workspace'

const workspace = useWorkspaceStore()
const loading = ref(false)
const uploading = ref(false)
const matching = ref(false)
const error = ref('')
const uploadInput = ref<HTMLInputElement | null>(null)
const matchInput = ref<HTMLInputElement | null>(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    await workspace.loadPets()
    if (workspace.currentPet) {
      await workspace.loadIdentityPhotos(workspace.currentPet.id)
    }
  } catch (err) {
    error.value = errorMessage(err, '加载失败')
  } finally {
    loading.value = false
  }
}

async function uploadPhotos() {
  const petId = workspace.currentPet?.id
  const files = Array.from(uploadInput.value?.files || [])
  if (!petId) {
    error.value = '请先创建并选择宠物'
    return
  }
  if (!files.length) return
  uploading.value = true
  error.value = ''
  try {
    for (const file of files) {
      await workspace.uploadIdentityPhoto(petId, file)
    }
    if (uploadInput.value) uploadInput.value.value = ''
  } catch (err) {
    error.value = errorMessage(err, '上传失败')
  } finally {
    uploading.value = false
  }
}

async function deletePhoto(photoId: number) {
  const petId = workspace.currentPet?.id
  if (!petId) return
  error.value = ''
  try {
    await workspace.deleteIdentityPhoto(petId, photoId)
  } catch (err) {
    error.value = errorMessage(err, '删除失败')
  }
}

async function matchPhoto() {
  const file = matchInput.value?.files?.[0]
  if (!file) return
  matching.value = true
  error.value = ''
  try {
    await workspace.matchIdentity(file)
  } catch (err) {
    error.value = errorMessage(err, '匹配失败')
  } finally {
    matching.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="page-stack">
    <header class="page-header">
      <div>
        <span class="eyebrow">Identity</span>
        <h1>宠物身份识别</h1>
      </div>
      <button class="toolbar-button" :disabled="loading" @click="load">
        <RefreshCw :size="17" />
        {{ loading ? '刷新中' : '刷新' }}
      </button>
    </header>

    <p v-if="error" class="error-line">{{ error }}</p>

    <section class="current-selection">
      <article class="selection-card primary">
        <span>当前宠物</span>
        <strong>{{ workspace.currentPet?.name || '未选择' }}</strong>
        <small>建议上传正脸、侧脸、全身 3 张身份照片</small>
      </article>
      <article class="selection-card">
        <span>身份照片</span>
        <strong>{{ workspace.identityPhotos.length }}</strong>
        <small>当前模型 local-identity-v1</small>
      </article>
    </section>

    <section class="split-layout">
      <article class="panel form-panel">
        <div class="panel-title">
          <h2>上传身份照片</h2>
          <ImagePlus :size="20" />
        </div>
        <label>
          选择照片
          <input ref="uploadInput" type="file" multiple accept="image/*" />
        </label>
        <button class="primary-action" :disabled="uploading" @click="uploadPhotos">
          <ImagePlus :size="18" />
          {{ uploading ? '上传中' : '上传照片' }}
        </button>
      </article>

      <article class="panel form-panel">
        <div class="panel-title">
          <h2>测试身份匹配</h2>
          <ScanSearch :size="20" />
        </div>
        <label>
          测试图片
          <input ref="matchInput" type="file" accept="image/*" />
        </label>
        <button class="toolbar-button" :disabled="matching" @click="matchPhoto">
          <Fingerprint :size="18" />
          {{ matching ? '匹配中' : '开始匹配' }}
        </button>
        <div v-if="workspace.identityMatch" class="match-result">
          <strong>{{ workspace.identityMatch.petName }}</strong>
          <span>{{ workspace.identityMatch.matched ? '已命中' : '低于阈值' }}</span>
          <small>相似度 {{ Math.round(workspace.identityMatch.similarity * 100) }}%</small>
        </div>
      </article>
    </section>

    <article class="panel">
      <div class="panel-title">
        <h2>已上传身份照片</h2>
        <span class="count-pill">{{ workspace.identityPhotos.length }}</span>
      </div>
      <div v-if="workspace.identityPhotos.length" class="identity-grid">
        <figure v-for="photo in workspace.identityPhotos" :key="photo.id" class="identity-card">
          <img :src="photo.imageUrl" alt="宠物身份照片" />
          <figcaption>
            <strong>#{{ photo.id }}</strong>
            <span>{{ photo.modelVersion }} · {{ photo.embeddingDimension }}维</span>
            <button class="danger-action compact" @click="deletePhoto(photo.id)">
              <Trash2 :size="15" />
              删除
            </button>
          </figcaption>
        </figure>
      </div>
      <p v-else class="empty-state">{{ loading ? '加载中...' : '暂无身份照片' }}</p>
    </article>
  </section>
</template>
