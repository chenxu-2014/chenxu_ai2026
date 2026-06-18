import http from './index'

/** 发送测试消息 */
export function sendKafka(key?: string, value?: string) {
  return http.get('/kafka/send', { params: { key, value } }).then(r => r.data)
}

/** 发送 10 条有序消息 */
export function sendOrdered() {
  return http.post('/sendOrdered').then(r => r.data)
}
