import { createRouter, createWebHistory } from 'vue-router'
import { useSessionStore } from './stores/session'
import DashboardView from './views/DashboardView.vue'
import LoginView from './views/LoginView.vue'
import PetsView from './views/PetsView.vue'
import DevicesView from './views/DevicesView.vue'
import LiveView from './views/LiveView.vue'
import BehaviorView from './views/BehaviorView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { public: true } },
    { path: '/', component: DashboardView },
    { path: '/pets', component: PetsView },
    { path: '/devices', component: DevicesView },
    { path: '/live', component: LiveView },
    { path: '/behavior', component: BehaviorView }
  ]
})

router.beforeEach((to) => {
  const session = useSessionStore()
  if (!to.meta.public && !session.isAuthed) {
    return '/login'
  }
  if (to.path === '/login' && session.isAuthed) {
    return '/'
  }
  return true
})

export default router
