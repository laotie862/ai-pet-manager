import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { apiData, http } from '../api/http';
export const behaviorText = {
    eating: '进食',
    drinking: '饮水',
    exercising: '活动',
    sleeping: '睡觉',
    defecating: '排便',
    uncertain: '待确认'
};
export const usePetcareStore = defineStore('petcare', () => {
    const pets = ref([]);
    const devices = ref([]);
    const currentPetId = ref(Number(localStorage.getItem('petcare.currentPetId')) || null);
    const currentDeviceId = ref(Number(localStorage.getItem('petcare.currentDeviceId')) || null);
    const currentBehavior = ref(null);
    const summary = ref(null);
    const timeline = ref([]);
    const deviceStatuses = ref({});
    const currentPet = computed(() => pets.value.find((pet) => pet.id === currentPetId.value) || pets.value[0] || null);
    const currentDevice = computed(() => {
        const selected = devices.value.find((device) => device.id === currentDeviceId.value);
        return selected || devices.value.find((device) => device.petId === currentPet.value?.id) || devices.value[0] || null;
    });
    function setCurrentPet(id) {
        currentPetId.value = id;
        localStorage.setItem('petcare.currentPetId', String(id));
    }
    function setCurrentDevice(id) {
        currentDeviceId.value = id;
        localStorage.setItem('petcare.currentDeviceId', String(id));
    }
    async function loadAll() {
        await Promise.all([loadPets(), loadDevices()]);
        if (currentPet.value) {
            await loadBehavior(currentPet.value.id);
        }
    }
    async function loadPets() {
        pets.value = await apiData(http.get('/pets'));
        if (!currentPetId.value && pets.value[0])
            setCurrentPet(pets.value[0].id);
    }
    async function createPet(payload) {
        const pet = await apiData(http.post('/pets', payload));
        await loadPets();
        setCurrentPet(pet.id);
    }
    async function updatePet(id, payload) {
        const pet = await apiData(http.put(`/pets/${id}`, payload));
        await loadPets();
        setCurrentPet(pet.id);
    }
    async function deletePet(id) {
        await apiData(http.delete(`/pets/${id}`));
        if (currentPetId.value === id) {
            currentPetId.value = null;
            localStorage.removeItem('petcare.currentPetId');
        }
        await Promise.all([loadPets(), loadDevices()]);
    }
    async function loadDevices() {
        devices.value = await apiData(http.get('/devices'));
        if (!currentDeviceId.value && devices.value[0])
            setCurrentDevice(devices.value[0].id);
    }
    async function createDevice(payload) {
        const device = await apiData(http.post('/devices', payload));
        await loadDevices();
        setCurrentDevice(device.id);
    }
    async function deleteDevice(id) {
        await apiData(http.delete(`/devices/${id}`));
        if (currentDeviceId.value === id) {
            currentDeviceId.value = null;
            localStorage.removeItem('petcare.currentDeviceId');
        }
        await loadDevices();
    }
    async function startDevice(deviceId) {
        const status = await apiData(http.post(`/devices/${deviceId}/stream/start`));
        deviceStatuses.value[deviceId] = status;
    }
    async function stopDevice(deviceId) {
        const status = await apiData(http.post(`/devices/${deviceId}/stream/stop`));
        deviceStatuses.value[deviceId] = status;
    }
    async function loadStatus(deviceId) {
        deviceStatuses.value[deviceId] = await apiData(http.get(`/devices/${deviceId}/status`));
    }
    async function loadBehavior(petId) {
        const [current, todaySummary, events] = await Promise.all([
            apiData(http.get(`/behavior/pets/${petId}/current`)),
            apiData(http.get(`/behavior/pets/${petId}/summary`)),
            apiData(http.get(`/behavior/pets/${petId}/timeline`))
        ]);
        currentBehavior.value = current;
        summary.value = todaySummary;
        timeline.value = events;
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
    };
});
