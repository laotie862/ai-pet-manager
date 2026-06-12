import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { api, unwrap } from '../api'
import type {
  BehaviorCurrent,
  BehaviorEvent,
  BehaviorSummary,
  Device,
  DeviceRequest,
  DeviceStatus,
  IdentityMatch,
  IdentityPhoto,
  Pet,
  PetRequest
} from '../types'

const CURRENT_PET_KEY = 'petcare.currentPetId'
const CURRENT_DEVICE_KEY = 'petcare.currentDeviceId'

const behaviorText: Record<string, string> = {
  eating: '进食',
  drinking: '饮水',
  exercising: '活动',
  sleeping: '睡觉',
  defecating: '排便',
  uncertain: '待确认'
}

export const useWorkspaceStore = defineStore('workspace', () => {
  const pets = ref<Pet[]>([])
  const devices = ref<Device[]>([])
  const currentPetId = ref<number | null>(Number(localStorage.getItem(CURRENT_PET_KEY)) || null)
  const currentDeviceId = ref<number | null>(Number(localStorage.getItem(CURRENT_DEVICE_KEY)) || null)
  const currentBehavior = ref<BehaviorCurrent | null>(null)
  const summary = ref<BehaviorSummary | null>(null)
  const timeline = ref<BehaviorEvent[]>([])
  const identityPhotos = ref<IdentityPhoto[]>([])
  const identityMatch = ref<IdentityMatch | null>(null)
  const deviceStatuses = ref<Record<number, DeviceStatus>>({})
  const devicePetBindings = ref<Record<number, Pet[]>>({})

  const currentPet = computed(() => pets.value.find((pet) => pet.id === currentPetId.value) || pets.value[0] || null)
  const currentDevice = computed(
    () =>
      devices.value.find((device) => device.id === currentDeviceId.value) ||
      devices.value.find((device) => device.petId === currentPet.value?.id) ||
      devices.value[0] ||
      null
  )

  function setCurrentPet(id: number) {
    currentPetId.value = id
    localStorage.setItem(CURRENT_PET_KEY, String(id))
  }

  function setCurrentDevice(id: number) {
    currentDeviceId.value = id
    localStorage.setItem(CURRENT_DEVICE_KEY, String(id))
  }

  async function loadAll() {
    await Promise.all([loadPets(), loadDevices()])
    if (currentPet.value) {
      await loadBehavior(currentPet.value.id)
      await loadIdentityPhotos(currentPet.value.id)
    }
  }

  async function loadPets() {
    pets.value = await unwrap<Pet[]>(api.get('/pets'))
    if (!currentPetId.value && pets.value[0]) setCurrentPet(pets.value[0].id)
  }

  async function createPet(payload: PetRequest) {
    const pet = await unwrap<Pet>(api.post('/pets', payload))
    await loadPets()
    setCurrentPet(pet.id)
  }

  async function updatePet(id: number, payload: PetRequest) {
    const pet = await unwrap<Pet>(api.put(`/pets/${id}`, payload))
    await loadPets()
    setCurrentPet(pet.id)
  }

  async function deletePet(id: number) {
    await unwrap<void>(api.delete(`/pets/${id}`))
    if (currentPetId.value === id) {
      currentPetId.value = null
      localStorage.removeItem(CURRENT_PET_KEY)
    }
    await Promise.all([loadPets(), loadDevices()])
  }

  async function loadDevices() {
    devices.value = await unwrap<Device[]>(api.get('/devices'))
    await Promise.all(devices.value.map((device) => loadDevicePets(device.id)))
    if (!currentDeviceId.value && devices.value[0]) setCurrentDevice(devices.value[0].id)
  }

  async function createDevice(payload: DeviceRequest) {
    const device = await unwrap<Device>(api.post('/devices', payload))
    await loadDevices()
    setCurrentDevice(device.id)
  }

  async function deleteDevice(id: number) {
    await unwrap<void>(api.delete(`/devices/${id}`))
    delete devicePetBindings.value[id]
    if (currentDeviceId.value === id) {
      currentDeviceId.value = null
      localStorage.removeItem(CURRENT_DEVICE_KEY)
    }
    await loadDevices()
  }

  async function startDevice(id: number) {
    deviceStatuses.value[id] = await unwrap<DeviceStatus>(api.post(`/devices/${id}/stream/start`))
  }

  async function stopDevice(id: number) {
    deviceStatuses.value[id] = await unwrap<DeviceStatus>(api.post(`/devices/${id}/stream/stop`))
  }

  async function loadStatus(id: number) {
    deviceStatuses.value[id] = await unwrap<DeviceStatus>(api.get(`/devices/${id}/status`))
  }

  async function loadDevicePets(deviceId: number) {
    const pets = await unwrap<Pet[]>(api.get(`/devices/${deviceId}/pets`))
    devicePetBindings.value = {
      ...devicePetBindings.value,
      [deviceId]: pets
    }
  }

  async function replaceDevicePets(deviceId: number, petIds: number[]) {
    const pets = await unwrap<Pet[]>(api.put(`/devices/${deviceId}/pets`, { petIds }))
    devicePetBindings.value = {
      ...devicePetBindings.value,
      [deviceId]: pets
    }
    await loadDevices()
  }

  async function loadBehavior(petId: number) {
    const [current, daySummary, events] = await Promise.all([
      unwrap<BehaviorCurrent>(api.get(`/behavior/pets/${petId}/current`)),
      unwrap<BehaviorSummary>(api.get(`/behavior/pets/${petId}/summary`)),
      unwrap<BehaviorEvent[]>(api.get(`/behavior/pets/${petId}/timeline`))
    ])
    currentBehavior.value = current
    summary.value = daySummary
    timeline.value = events
  }

  async function loadIdentityPhotos(petId: number) {
    identityPhotos.value = await unwrap<IdentityPhoto[]>(api.get(`/pets/${petId}/identity-photos`))
  }

  async function uploadIdentityPhoto(petId: number, file: File) {
    const form = new FormData()
    form.append('file', file)
    await unwrap<IdentityPhoto>(api.post(`/pets/${petId}/identity-photos`, form))
    await loadIdentityPhotos(petId)
  }

  async function deleteIdentityPhoto(petId: number, photoId: number) {
    await unwrap<void>(api.delete(`/pets/${petId}/identity-photos/${photoId}`))
    await loadIdentityPhotos(petId)
  }

  async function matchIdentity(file: File) {
    const form = new FormData()
    form.append('file', file)
    identityMatch.value = await unwrap<IdentityMatch>(api.post('/pets/identity/match', form))
  }

  return {
    behaviorText,
    pets,
    devices,
    currentPetId,
    currentDeviceId,
    currentPet,
    currentDevice,
    currentBehavior,
    summary,
    timeline,
    identityPhotos,
    identityMatch,
    deviceStatuses,
    devicePetBindings,
    setCurrentPet,
    setCurrentDevice,
    loadAll,
    loadPets,
    createPet,
    updatePet,
    deletePet,
    loadDevices,
    createDevice,
    deleteDevice,
    startDevice,
    stopDevice,
    loadStatus,
    loadDevicePets,
    replaceDevicePets,
    loadBehavior,
    loadIdentityPhotos,
    uploadIdentityPhoto,
    deleteIdentityPhoto,
    matchIdentity
  }
})
