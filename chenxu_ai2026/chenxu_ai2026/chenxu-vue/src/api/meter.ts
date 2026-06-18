import http from './index'

/** 加载电表数据到 Redis */
export function loadMeter() {
  return http.post('/meter/load').then(r => r.data)
}

/** 生成 1000w 模拟数据 */
export function generateMeter() {
  return http.post('/meter/generate').then(r => r.data)
}

/** 查询 Redis 中电表数量 */
export function countMeter() {
  return http.get('/meter/count').then(r => r.data)
}

/** 查询指定电表 */
export function getMeterKey(key: string) {
  return http.get(`/meter/getkey/${key}`).then(r => r.data)
}
