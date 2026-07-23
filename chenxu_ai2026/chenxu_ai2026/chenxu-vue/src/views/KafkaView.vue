<template>
  <div>
    <h1 class="page-title">📨 Kafka 消息测试</h1>

    <div class="card">
      <h3>发送测试消息</h3>
      <div class="inline-form">
        <input v-model="kafkaKey" placeholder="Key (可选)" />
        <input v-model="kafkaValue" placeholder="Value (可选)" />
        <button @click="doSend" :disabled="!!loading" class="btn btn-primary">发送</button>
      </div>
    </div>

    <div class="actions">
      <button @click="doSendOrdered" :disabled="!!loading" class="btn btn-info">
        {{ loading === 'ordered' ? '发送中...' : '发送 10 条有序消息' }}
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
import { sendKafka, sendOrdered } from '../api/kafka'

type LoadingType = string | false
const loading = ref<LoadingType>(false)
const result = ref<any>(null)
const kafkaKey = ref('')
const kafkaValue = ref('')

async function doAction<T>(fn: () => Promise<T>, label: string) {
  loading.value = label
  try { result.value = await fn() }
  catch (e: any) { result.value = e.message || '请求失败' }
  finally { loading.value = false }
}

const doSend = () => doAction(() => sendKafka(kafkaKey.value, kafkaValue.value), 'send')
const doSendOrdered = () => doAction(sendOrdered, 'ordered')
</script>

<style scoped>
.page-title { margin: 0 0 24px; color: #333; }
.actions { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.card {
  background: #fff; padding: 20px; border-radius: 10px;
  margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.card h3 { margin: 0 0 12px; font-size: 15px; color: #555; }
.inline-form { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.inline-form input {
  padding: 8px 12px; border: 1px solid #ddd; border-radius: 6px;
  font-size: 14px; width: 160px;
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
