/** 后端统一响应格式 */
export interface ApiResponse<T = any> {
  code: number
  msg: string
  data?: T
}

/** 登录/注册参数 */
export interface AuthParams {
  username: string
  password: string
  nickname?: string
}

/** 登录返回值 */
export interface LoginData {
  token: string
  userId: number
  username: string
  nickname: string
}

/** 用户信息 */
export interface UserInfo {
  userId: number
  username: string
  nickname: string
}
