import http from './index'

/** 执行全量数据比对 */
export function runCompare(pageSize = 5000) {
  return http.post('/compare/run', null, { params: { pageSize } }).then(r => r.data)
}

/** Hash 分桶比对 */
export function runCompareWithBuckets(pageSize = 5000, bucketCount = 100) {
  return http.post('/compare/runWithBuckets', null, { params: { pageSize, bucketCount } }).then(r => r.data)
}

/** 查询比对结果 */
export function getCompareResult(batchId: string) {
  return http.get(`/compare/result/${batchId}`).then(r => r.data)
}
