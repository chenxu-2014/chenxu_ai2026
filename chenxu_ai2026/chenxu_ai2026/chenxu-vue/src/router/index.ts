import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/LoginView.vue'),
      meta: { guest: true }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('../views/RegisterView.vue'),
      meta: { guest: true }
    },
    {
      path: '/',
      component: () => import('../layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', name: 'Dashboard', component: () => import('../views/DashboardView.vue') },
        { path: 'meter', name: 'Meter', component: () => import('../views/MeterView.vue') },
        { path: 'compare', name: 'Compare', component: () => import('../views/CompareView.vue') },
        { path: 'kafka', name: 'Kafka', component: () => import('../views/KafkaView.vue') },
        { path: 'redis', name: 'Redis', component: () => import('../views/RedisView.vue') },
        { path: 'sentinel', name: 'Sentinel', component: () => import('../views/SentinelView.vue') },
        { path: 'transaction', name: 'Transaction', component: () => import('../views/TransactionView.vue') }
      ]
    }
  ]
})

// 导航守卫：未登录跳登录页，已登录不重复进登录页
router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    return { name: 'Login' }
  }
  if (to.meta.guest && token) {
    return { name: 'Dashboard' }
  }
})

export default router
