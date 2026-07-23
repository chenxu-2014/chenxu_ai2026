import axios from 'axios'
import type { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

// 创建 Axios 实例
const http: AxiosInstance = axios.create({
  baseURL: '/',  // Vite proxy 自动转发到 localhost:8081
  timeout: 30000,
  headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
})

// 请求拦截器：自动附加 Token
http.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：401 跳转登录
http.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default http
