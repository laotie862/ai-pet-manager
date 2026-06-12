import { createRouter, createWebHistory } from 'vue-router'

import BehaviorView from './views/BehaviorView.vue'
import DashboardView from './views/DashboardView.vue'
import DevicesView from './views/DevicesView.vue'
import IdentityView from './views/IdentityView.vue'
import LiveView from './views/LiveView.vue'
import LoginView from './views/LoginView.vue'
import PetsView from './views/PetsView.vue'
import { useAuthStore } from './stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { public: true } },
    { path: '/', component: DashboardView },
    { path: '/pets', component: PetsView },
    { path: '/devices', component: DevicesView },
    { path: '/live', component: LiveView },
    { path: '/behavior', component: BehaviorView },
    { path: '/identity', component: IdentityView }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isAuthed) return '/login'
  if (to.path === '/login' && auth.isAuthed) return '/'
  return true
})

export default router
