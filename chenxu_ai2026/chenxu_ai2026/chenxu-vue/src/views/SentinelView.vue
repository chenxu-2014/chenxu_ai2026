<template>
  <div>
    <h1 class="page-title">🛡️ Sentinel 熔断限流</h1>

    <div class="actions">
      <button @click="doFlow" :disabled="loading" class="btn btn-primary">
        {{ loading === 'flow' ? '测试中...' : '流控测试 (QPS=2)' }}
      </button>
      <button @click="doDegrade(false)" :disabled="loading" class="btn btn-info">
        {{ loading === 'degrade-ok' ? '测试中...' : '熔断测试 (正常)' }}
      </button>
      <button @click="doDegrade(true)" :disabled="loading" class="btn btn-warn">
        {{ loading === 'degrade-err' ? '测试中...' : '熔断测试 (触发错误)' }}
      </button>
      <button @click="doSlow" :disabled="loading" class="btn btn-danger">
        {{ loading === 'slow' ? '测试中...' : '慢调用测试 (500ms)' }}
      </button>
    </div>

    <div v-if="result !== null" class="card">
      <h3>返回结果</h3>
      <pre>{{ result }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { testFlow, testDegrade, testSlow } from '../api/sentinel'

type LoadingType = string | null
const loading = ref<LoadingType>(null)
const result = ref<any>(null)

async function doAction<T>(fn: () => Promise<T>, label: string) {
  loading.value = label
  try { result.value = await fn() }
  catch (e: any) { result.value = e.message || '请求失败' }
  finally { loading.value = null }
}

const doFlow = () => doAction(testFlow, 'flow')
const doDegrade = (error: boolean) => doAction(() => testDegrade(error), error ? 'degrade-err' : 'degrade-ok')
const doSlow = () => doAction(testSlow, 'slow')
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
.btn { padding: 10px 20px; border: none; border-radius: 8px; font-size: 14px; cursor: pointer; color: #fff; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-primary { background: #3498db; }
.btn-info { background: #2ecc71; }
.btn-warn { background: #e67e22; }
.btn-danger { background: #e74c3c; }
</style>
