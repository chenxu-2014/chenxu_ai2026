<template>
  <div class="login-page">
    <div class="login-card">
      <h1>创建账号</h1>
      <p class="subtitle">注册新用户</p>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label>用户名</label>
          <input v-model="username" type="text" placeholder="请输入用户名" required />
        </div>
        <div class="form-group">
          <label>昵称（选填）</label>
          <input v-model="nickname" type="text" placeholder="请输入昵称" />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="password" type="password" placeholder="请输入密码" required />
        </div>
        <p v-if="error" class="error-msg">{{ error }}</p>
        <p v-if="success" class="success-msg">{{ success }}</p>
        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>
      <p class="switch-link">
        已有账号？<router-link to="/login">返回登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const username = ref('')
const nickname = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')
const success = ref('')

async function handleRegister() {
  error.value = ''
  success.value = ''
  loading.value = true
  try {
    const res = await authStore.register({
      username: username.value,
      password: password.value,
      nickname: nickname.value
    })
    if (res.code === 200) {
      success.value = '注册成功！请返回登录'
      username.value = ''
      password.value = ''
      nickname.value = ''
    } else {
      error.value = res.msg || '注册失败'
    }
  } catch {
    error.value = '网络错误，请检查后端是否启动'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}
.login-card {
  background: #fff;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
  width: 380px;
  max-width: 90vw;
}
h1 {
  text-align: center;
  margin: 0;
  color: #333;
}
.subtitle {
  text-align: center;
  color: #888;
  margin: 8px 0 24px;
}
.form-group {
  margin-bottom: 16px;
}
.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: #555;
}
.form-group input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
}
.form-group input:focus {
  outline: none;
  border-color: #11998e;
}
.btn-primary {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  cursor: pointer;
  margin-top: 8px;
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.error-msg {
  color: #e74c3c;
  font-size: 13px;
  margin: 4px 0;
}
.success-msg {
  color: #27ae60;
  font-size: 13px;
  margin: 4px 0;
}
.switch-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #888;
}
.switch-link a {
  color: #11998e;
}
</style>
