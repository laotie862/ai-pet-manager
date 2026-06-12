<script setup lang="ts">
import { Check, Plus, RefreshCw, Save, Trash2 } from 'lucide-vue-next'
import { computed, onMounted, reactive, ref, watch } from 'vue'

import { errorMessage } from '../api'
import { useWorkspaceStore } from '../stores/workspace'
import type { Pet } from '../types'

const workspace = useWorkspaceStore()
const loading = ref(false)
const saving = ref(false)
const error = ref('')

const createForm = reactive({ name: '小猫', species: 'cat', breed: '', gender: '', birthday: '', weightKg: 4.2 })
const editForm = reactive({ name: '', species: '', breed: '', gender: '', birthday: '', weightKg: undefined as number | undefined })
const selectedPet = computed(() => workspace.currentPet)

watch(selectedPet, fillEditForm, { immediate: true })

function fillEditForm(pet: Pet | null) {
  editForm.name = pet?.name || ''
  editForm.species = pet?.species || ''
  editForm.breed = pet?.breed || ''
  editForm.gender = pet?.gender || ''
  editForm.birthday = pet?.birthday || ''
  editForm.weightKg = pet?.weightKg
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    await workspace.loadPets()
  } catch (err) {
    error.value = errorMessage(err, '加载失败')
  } finally {
    loading.value = false
  }
}

async function createPet() {
  saving.value = true
  error.value = ''
  try {
    await workspace.createPet({ ...createForm, birthday: createForm.birthday || undefined })
    createForm.name = ''
    createForm.breed = ''
    createForm.gender = ''
    createForm.birthday = ''
  } catch (err) {
    error.value = errorMessage(err, '创建失败')
  } finally {
    saving.value = false
  }
}

async function updatePet() {
  if (!selectedPet.value) return
  saving.value = true
  error.value = ''
  try {
    await workspace.updatePet(selectedPet.value.id, { ...editForm, birthday: editForm.birthday || undefined })
  } catch (err) {
    error.value = errorMessage(err, '保存失败')
  } finally {
    saving.value = false
  }
}

async function deletePet() {
  if (!selectedPet.value) return
  if (!window.confirm(`确定删除宠物「${selectedPet.value.name}」吗？`)) return
  saving.value = true
  error.value = ''
  try {
    await workspace.deletePet(selectedPet.value.id)
  } catch (err) {
    error.value = errorMessage(err, '删除失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="page-stack">
    <header class="page-header">
      <div>
        <span class="eyebrow">Pets</span>
        <h1>宠物信息</h1>
      </div>
      <button class="toolbar-button" :disabled="loading" @click="load">
        <RefreshCw :size="17" />
        {{ loading ? '刷新中' : '刷新' }}
      </button>
    </header>

    <p v-if="error" class="error-line">{{ error }}</p>

    <section class="split-layout pet-editor-layout">
      <article class="panel">
        <div class="panel-title">
          <h2>选择宠物</h2>
          <span class="count-pill">{{ workspace.pets.length }}</span>
        </div>
        <div v-if="workspace.pets.length" class="pet-card-grid">
          <button
            v-for="pet in workspace.pets"
            :key="pet.id"
            class="pet-card"
            :class="{ selected: workspace.currentPetId === pet.id }"
            @click="workspace.setCurrentPet(pet.id)"
          >
            <span class="pet-avatar">{{ pet.name.slice(0, 1) }}</span>
            <span>
              <strong>{{ pet.name }}</strong>
              <small>{{ pet.species }} · {{ pet.breed || '未填写品种' }}</small>
              <small>{{ pet.weightKg || '-' }} kg · {{ pet.gender || '未填写性别' }}</small>
            </span>
            <Check v-if="workspace.currentPetId === pet.id" :size="18" />
          </button>
        </div>
        <p v-else class="empty-state">{{ loading ? '加载中...' : '暂无宠物' }}</p>
      </article>

      <div class="editor-column">
        <form class="panel form-panel" @submit.prevent="updatePet">
          <div class="panel-title">
            <h2>修改宠物信息</h2>
            <Save :size="20" />
          </div>
          <template v-if="selectedPet">
            <label>名字<input v-model="editForm.name" required /></label>
            <label>种类<input v-model="editForm.species" required placeholder="cat / dog" /></label>
            <label>品种<input v-model="editForm.breed" placeholder="例如 英短、金毛" /></label>
            <label>性别<input v-model="editForm.gender" placeholder="male / female" /></label>
            <label>生日<input v-model="editForm.birthday" type="date" /></label>
            <label>体重 kg<input v-model.number="editForm.weightKg" type="number" min="0.1" step="0.1" /></label>
            <button class="primary-action" type="submit" :disabled="saving"><Save :size="18" />保存修改</button>
            <button class="danger-action" type="button" :disabled="saving" @click="deletePet"><Trash2 :size="18" />删除宠物</button>
          </template>
          <p v-else class="empty-state">请先在左侧选择一个宠物。</p>
        </form>

        <form class="panel form-panel" @submit.prevent="createPet">
          <div class="panel-title">
            <h2>新增宠物</h2>
            <Plus :size="20" />
          </div>
          <label>名字<input v-model="createForm.name" required /></label>
          <label>种类<input v-model="createForm.species" required placeholder="cat / dog" /></label>
          <label>品种<input v-model="createForm.breed" placeholder="可选" /></label>
          <label>性别<input v-model="createForm.gender" placeholder="可选" /></label>
          <label>生日<input v-model="createForm.birthday" type="date" /></label>
          <label>体重 kg<input v-model.number="createForm.weightKg" type="number" min="0.1" step="0.1" /></label>
          <button class="toolbar-button" type="submit" :disabled="saving"><Plus :size="18" />创建宠物</button>
        </form>
      </div>
    </section>
  </section>
</template>
