<template>
  <div class="sidebar">
    <div class="sidebar-header">
      <h2>Chenxu Demo</h2>
      <span class="user-info" v-if="auth.user">{{ auth.user.nickname }}</span>
    </div>
    <nav class="sidebar-nav">
      <router-link v-for="item in menuItems" :key="item.path" :to="item.path"
                   class="nav-item" active-class="nav-item--active">
        <span class="nav-icon">{{ item.icon }}</span>
        <span>{{ item.label }}</span>
      </router-link>
    </nav>
    <div class="sidebar-footer">
      <button @click="handleLogout" class="logout-btn">退出登录</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

const menuItems = [
  { path: '/',          label: '仪表盘',     icon: '📊' },
  { path: '/meter',     label: '电表数据',   icon: '⚡' },
  { path: '/compare',   label: '数据比对',   icon: '🔍' },
  { path: '/kafka',     label: 'Kafka 消息', icon: '📨' },
  { path: '/redis',     label: 'Redis 类型', icon: '🗄️' },
  { path: '/sentinel',  label: '熔断限流',   icon: '🛡️' },
  { path: '/transaction', label: '交易链路', icon: '💳' }
]

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.sidebar {
  width: 220px;
  height: 100vh;
  background: #1a1a2e;
  color: #eee;
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
}
.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #333;
}
.sidebar-header h2 {
  margin: 0;
  font-size: 18px;
  color: #4fc3f7;
}
.user-info {
  font-size: 12px;
  color: #888;
  margin-top: 4px;
  display: block;
}
.sidebar-nav {
  flex: 1;
  padding: 10px 0;
  overflow-y: auto;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  color: #bbb;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s;
}
.nav-item:hover {
  background: #16213e;
  color: #fff;
}
.nav-item--active {
  background: #0f3460;
  color: #4fc3f7;
  border-right: 3px solid #4fc3f7;
}
.nav-icon {
  font-size: 18px;
  width: 24px;
  text-align: center;
}
.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid #333;
}
.logout-btn {
  width: 100%;
  padding: 8px;
  background: #e74c3c;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}
.logout-btn:hover {
  background: #c0392b;
}
</style>
