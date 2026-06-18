import http from './index'
import type { ApiResponse, AuthParams, LoginData } from '../types/api'

/** 登录 */
export function login(params: AuthParams): Promise<ApiResponse<LoginData>> {
  const data = new URLSearchParams()
  data.append('username', params.username)
  data.append('password', params.password)
  return http.post('/auth/login', data).then(r => r.data)
}

/** 注册 */
export function register(params: AuthParams): Promise<ApiResponse<{ userId: number }>> {
  const data = new URLSearchParams()
  data.append('username', params.username)
  data.append('password', params.password)
  if (params.nickname) data.append('nickname', params.nickname)
  return http.post('/auth/register', data).then(r => r.data)
}
