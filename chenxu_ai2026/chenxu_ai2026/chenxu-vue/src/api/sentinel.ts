import http from './index'

/** 流控测试 */
export function testFlow() {
  return http.get('/sentinel/test/flow').then(r => r.data)
}

/** 熔断降级测试 */
export function testDegrade(error = false) {
  return http.get('/sentinel/test/degrade', { params: { error } }).then(r => r.data)
}

/** 慢调用熔断测试 */
export function testSlow() {
  return http.get('/sentinel/test/slow').then(r => r.data)
}
