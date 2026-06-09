import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { apiData, http } from '../api/http'
import type { BehaviorCurrent, BehaviorEvent, BehaviorSummary, Device, DeviceStatus, Pet } from '../api/types'

export const behaviorText: Record<string, string> = {
  eating: '进食',
  drinking: '饮水',
  exercising: '活动',
  sleeping: '睡觉',
  defecating: '排便',
  uncertain: '待确认'
}

export const usePetcareStore = defineStore('petcare', () => {
  const pets = ref<Pet[]>([])
  const devices = ref<Device[]>([])
  const currentPetId = ref<number | null>(Number(localStorage.getItem('petcare.currentPetId')) || null)
  const currentDeviceId = ref<number | null>(Number(localStorage.getItem('petcare.currentDeviceId')) || null)
  const currentBehavior = ref<BehaviorCurrent | null>(null)
  const summary = ref<BehaviorSummary | null>(null)
  const timeline = ref<BehaviorEvent[]>([])
  const deviceStatuses = ref<Record<number, DeviceStatus>>({})

  const currentPet = computed(() => pets.value.find((pet) => pet.id === currentPetId.value) || pets.value[0] || null)
  const currentDevice = computed(() => {
    const selected = devices.value.find((device) => device.id === currentDeviceId.value)
    return selected || devices.value.find((device) => device.petId === currentPet.value?.id) || devices.value[0] || null
  })

  function setCurrentPet(id: number) {
    currentPetId.value = id
    localStorage.setItem('petcare.currentPetId', String(id))
  }

  function setCurrentDevice(id: number) {
    currentDeviceId.value = id
    localStorage.setItem('petcare.currentDeviceId', String(id))
  }

  async function loadAll() {
    await Promise.all([loadPets(), loadDevices()])
    if (currentPet.value) {
      await loadBehavior(currentPet.value.id)
    }
  }

  async function loadPets() {
    pets.value = await apiData<Pet[]>(http.get('/pets'))
    if (!currentPetId.value && pets.value[0]) setCurrentPet(pets.value[0].id)
  }

  async function createPet(payload: Partial<Pet>) {
    const pet = await apiData<Pet>(http.post('/pets', payload))
    await loadPets()
    setCurrentPet(pet.id)
  }

  async function updatePet(id: number, payload: Partial<Pet>) {
    const pet = await apiData<Pet>(http.put(`/pets/${id}`, payload))
    await loadPets()
    setCurrentPet(pet.id)
  }

  async function deletePet(id: number) {
    await apiData<void>(http.delete(`/pets/${id}`))
    if (currentPetId.value === id) {
      currentPetId.value = null
      localStorage.removeItem('petcare.currentPetId')
    }
    await Promise.all([loadPets(), loadDevices()])
  }

  async function loadDevices() {
    devices.value = await apiData<Device[]>(http.get('/devices'))
    if (!currentDeviceId.value && devices.value[0]) setCurrentDevice(devices.value[0].id)
  }

  async function createDevice(payload: { name: string; petId?: number; rtspUrl: string; username?: string; password?: string }) {
    const device = await apiData<Device>(http.post('/devices', payload))
    await loadDevices()
    setCurrentDevice(device.id)
  }

  async function deleteDevice(id: number) {
    await apiData<void>(http.delete(`/devices/${id}`))
    if (currentDeviceId.value === id) {
      currentDeviceId.value = null
      localStorage.removeItem('petcare.currentDeviceId')
    }
    await loadDevices()
  }

  async function startDevice(deviceId: number) {
    const status = await apiData<DeviceStatus>(http.post(`/devices/${deviceId}/stream/start`))
    deviceStatuses.value[deviceId] = status
  }

  async function stopDevice(deviceId: number) {
    const status = await apiData<DeviceStatus>(http.post(`/devices/${deviceId}/stream/stop`))
    deviceStatuses.value[deviceId] = status
  }

  async function loadStatus(deviceId: number) {
    deviceStatuses.value[deviceId] = await apiData<DeviceStatus>(http.get(`/devices/${deviceId}/status`))
  }

  async function loadBehavior(petId: number) {
    const [current, todaySummary, events] = await Promise.all([
      apiData<BehaviorCurrent>(http.get(`/behavior/pets/${petId}/current`)),
      apiData<BehaviorSummary>(http.get(`/behavior/pets/${petId}/summary`)),
      apiData<BehaviorEvent[]>(http.get(`/behavior/pets/${petId}/timeline`))
    ])
    currentBehavior.value = current
    summary.value = todaySummary
    timeline.value = events
  }

  return {
    pets,
    devices,
    currentPetId,
    currentDeviceId,
    currentPet,
    currentDevice,
    currentBehavior,
    summary,
    timeline,
    deviceStatuses,
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
    loadBehavior
  }
})
