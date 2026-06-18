import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi } from '../api/auth'
import type { AuthParams, UserInfo } from '../types/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref<UserInfo | null>(JSON.parse(localStorage.getItem('user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)

  async function login(params: AuthParams) {
    const res = await loginApi(params)
    if (res.code === 200 && res.data) {
      token.value = res.data.token
      user.value = {
        userId: res.data.userId,
        username: res.data.username,
        nickname: res.data.nickname
      }
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify(user.value))
    }
    return res
  }

  async function register(params: AuthParams) {
    return await registerApi(params)
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, isLoggedIn, login, register, logout }
})
