<template>
  <div>
    <h1 class="page-title">🔍 数据比对</h1>

    <div class="actions">
      <button @click="doCompare" :disabled="!!loading" class="btn btn-primary">
        {{ loading === 'compare' ? '比对中...' : '全量比对' }}
      </button>
      <button @click="doCompareBuckets" :disabled="!!loading" class="btn btn-info">
        {{ loading === 'buckets' ? '比对中...' : 'Hash 分桶比对' }}
      </button>
    </div>

    <div class="params">
      <label>PageSize: <input v-model.number="pageSize" type="number" /></label>
      <label v-if="bucketMode">BucketCount: <input v-model.number="bucketCount" type="number" /></label>
      <label class="toggle">
        <input type="checkbox" v-model="bucketMode" /> Hash 分桶模式
      </label>
    </div>

    <div class="card">
      <h3>查询比对结果</h3>
      <div class="inline-form">
        <input v-model="batchId" placeholder="输入 batchId" />
        <button @click="doGetResult" :disabled="!!loading" class="btn btn-info">查询</button>
      </div>
    </div>

    <div v-if="result !== null" class="card">
      <h3>返回结果</h3>
      <pre>{{ result }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { runCompare, runCompareWithBuckets, getCompareResult } from '../api/compare'

type LoadingType = string | false
const loading = ref<LoadingType>(false)
const result = ref<any>(null)
const pageSize = ref(5000)
const bucketCount = ref(100)
const bucketMode = ref(false)
const batchId = ref('')

async function doAction<T>(fn: () => Promise<T>, label: string) {
  loading.value = label
  try { result.value = await fn() }
  catch (e: any) { result.value = e.message || '请求失败' }
  finally { loading.value = false }
}

const doCompare = () => doAction(() => runCompare(pageSize.value), 'compare')
const doCompareBuckets = () => doAction(
  () => runCompareWithBuckets(pageSize.value, bucketCount.value), 'buckets'
)
const doGetResult = () => doAction(() => getCompareResult(batchId.value), 'result')
</script>

<style scoped>
.page-title { margin: 0 0 24px; color: #333; }
.actions { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.params { display: flex; gap: 16px; align-items: center; margin-bottom: 16px; font-size: 14px; }
.params input[type=number] { width: 80px; padding: 6px; border: 1px solid #ddd; border-radius: 4px; }
.toggle { cursor: pointer; }
.card {
  background: #fff; padding: 20px; border-radius: 10px;
  margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.card h3 { margin: 0 0 12px; font-size: 15px; color: #555; }
.inline-form { display: flex; gap: 10px; align-items: center; }
.inline-form input {
  padding: 8px 12px; border: 1px solid #ddd; border-radius: 6px;
  font-size: 14px; width: 200px;
}
pre {
  background: #1a1a2e; color: #4fc3f7; padding: 16px; border-radius: 8px;
  font-size: 13px; overflow-x: auto; max-height: 400px; white-space: pre-wrap; word-break: break-all;
}
.btn { padding: 10px 20px; border: none; border-radius: 8px; font-size: 14px; cursor: pointer; color: #fff; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-primary { background: #3498db; }
.btn-info { background: #2ecc71; }
</style>
