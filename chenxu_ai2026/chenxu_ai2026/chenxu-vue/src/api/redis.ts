import http from './index'

export function getRedisTypes()      { return http.get('/redis/types').then(r => r.data) }
export function demoString()          { return http.get('/redis/types/string').then(r => r.data) }
export function demoList()            { return http.get('/redis/types/list').then(r => r.data) }
export function demoSet()             { return http.get('/redis/types/set').then(r => r.data) }
export function demoZSet()            { return http.get('/redis/types/zset').then(r => r.data) }
export function demoHash()            { return http.get('/redis/types/hash').then(r => r.data) }
export function demoBitmap()          { return http.get('/redis/types/bitmap').then(r => r.data) }
export function demoHyperLogLog()     { return http.get('/redis/types/hll').then(r => r.data) }
export function demoGeo()             { return http.get('/redis/types/geo').then(r => r.data) }
export function demoStream()          { return http.get('/redis/types/stream').then(r => r.data) }
export function cleanDemos()          { return http.get('/redis/types/clean').then(r => r.data) }
