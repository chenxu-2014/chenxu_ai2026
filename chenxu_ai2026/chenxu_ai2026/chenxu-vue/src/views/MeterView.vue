<template>
  <div>
    <h1 class="page-title">⚡ 电表数据管理</h1>

    <!-- 操作按钮 -->
    <div class="actions">
      <button @click="doGenerate" :disabled="!!loading" class="btn btn-warn">
        {{ loading === 'generate' ? '生成中...' : '生成 1000w 模拟数据' }}
      </button>
      <button @click="doLoad" :disabled="!!loading" class="btn btn-primary">
        {{ loading === 'load' ? '加载中(3-6分钟)...' : '加载数据到 Redis' }}
      </button>
      <button @click="doCount" :disabled="!!loading" class="btn btn-info">
        {{ loading === 'count' ? '查询中...' : '查询 Redis 数量' }}
      </button>
    </div>

    <!-- 单key查询 -->
    <div class="card">
      <h3>查询指定电表</h3>
      <div class="inline-form">
        <input v-model="meterKey" placeholder="输入电表ID" />
        <button @click="doGetKey" :disabled="!!loading" class="btn btn-info">查询</button>
      </div>
    </div>

    <!-- 结果展示 -->
    <div v-if="result !== null" class="card">
      <h3>返回结果</h3>
      <pre>{{ result }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { generateMeter, loadMeter, countMeter, getMeterKey } from '../api/meter'

type LoadingType = string | false
const loading = ref<LoadingType>(false)
const result = ref<any>(null)
const meterKey = ref('')

async function doAction<T>(fn: () => Promise<T>, label: string) {
  loading.value = label
  try {
    result.value = await fn()
  } catch (e: any) {
    result.value = e.message || '请求失败'
  } finally {
    loading.value = false
  }
}

const doGenerate = () => doAction(generateMeter, 'generate')
const doLoad = () => doAction(loadMeter, 'load')
const doCount = () => doAction(countMeter, 'count')
const doGetKey = () => doAction(() => getMeterKey(meterKey.value), 'count')
</script>

<style scoped>
.page-title { margin: 0 0 24px; color: #333; }
.actions { display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.card {
  background: #fff;
  padding: 20px;
  border-radius: 10px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.card h3 { margin: 0 0 12px; font-size: 15px; color: #555; }
.inline-form { display: flex; gap: 10px; align-items: center; }
.inline-form input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  width: 200px;
}
pre {
  background: #1a1a2e;
  color: #4fc3f7;
  padding: 16px;
  border-radius: 8px;
  font-size: 13px;
  overflow-x: auto;
  max-height: 400px;
  white-space: pre-wrap;
  word-break: break-all;
}
.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  color: #fff;
}
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-primary { background: #3498db; }
.btn-info { background: #2ecc71; }
.btn-warn { background: #e67e22; }
</style>
