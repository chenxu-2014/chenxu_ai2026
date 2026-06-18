<template>
  <div>
    <h1 class="page-title">🗄️ Redis 数据类型</h1>

    <div class="actions">
      <button v-for="t in types" :key="t.key" @click="doDemo(t.key, t.fn)"
              :disabled="loading" class="btn btn-info">
        {{ t.label }}
      </button>
      <button @click="doClean" :disabled="loading" class="btn btn-warn">清理 demo 数据</button>
    </div>

    <div v-if="result !== null" class="card">
      <h3>返回结果</h3>
      <pre>{{ result }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import {
  demoString, demoList, demoSet, demoZSet, demoHash,
  demoBitmap, demoHyperLogLog, demoGeo, demoStream, cleanDemos
} from '../api/redis'

type LoadingType = string | null
const loading = ref<LoadingType>(null)
const result = ref<any>(null)

const types = [
  { key: 'string', label: 'String', fn: demoString },
  { key: 'list', label: 'List', fn: demoList },
  { key: 'set', label: 'Set', fn: demoSet },
  { key: 'zset', label: 'ZSet', fn: demoZSet },
  { key: 'hash', label: 'Hash', fn: demoHash },
  { key: 'bitmap', label: 'Bitmap', fn: demoBitmap },
  { key: 'hll', label: 'HyperLogLog', fn: demoHyperLogLog },
  { key: 'geo', label: 'Geo', fn: demoGeo },
  { key: 'stream', label: 'Stream', fn: demoStream }
]

async function doDemo(key: string, fn: () => Promise<any>) {
  loading.value = key
  try { result.value = await fn() }
  catch (e: any) { result.value = e.message || '请求失败' }
  finally { loading.value = null }
}

async function doClean() {
  loading.value = 'clean'
  try { result.value = await cleanDemos() }
  catch (e: any) { result.value = e.message || '请求失败' }
  finally { loading.value = null }
}
</script>

<style scoped>
.page-title { margin: 0 0 24px; color: #333; }
.actions { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.card {
  background: #fff; padding: 20px; border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.card h3 { margin: 0 0 12px; font-size: 15px; color: #555; }
pre {
  background: #1a1a2e; color: #4fc3f7; padding: 16px; border-radius: 8px;
  font-size: 13px; overflow-x: auto; max-height: 400px; white-space: pre-wrap; word-break: break-all;
}
.btn { padding: 8px 16px; border: none; border-radius: 8px; font-size: 13px; cursor: pointer; color: #fff; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-info { background: #2ecc71; }
.btn-warn { background: #e67e22; }
</style>
