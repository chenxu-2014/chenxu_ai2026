<template>
  <div>
    <h1 class="page-title">💳 交易链路 Demo</h1>

    <div class="card">
      <h3>执行交易（下单 → 库存 → 支付）</h3>
      <div class="inline-form">
        <label>用户ID: <input v-model="userId" placeholder="userId" /></label>
        <label>商品ID: <input v-model="productId" placeholder="productId" /></label>
        <label>金额: <input v-model.number="amount" type="number" placeholder="amount" /></label>
        <button @click="doExecute" :disabled="loading" class="btn btn-primary">执行</button>
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
import { executeOrder } from '../api/transaction'

const loading = ref(false)
const result = ref<any>(null)
const userId = ref('user1')
const productId = ref('prod_1001')
const amount = ref(99.99)

async function doExecute() {
  loading.value = true
  try {
    result.value = await executeOrder(userId.value, productId.value, amount.value)
  } catch (e: any) {
    result.value = e.message || '请求失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page-title { margin: 0 0 24px; color: #333; }
.card {
  background: #fff; padding: 20px; border-radius: 10px;
  margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.card h3 { margin: 0 0 12px; font-size: 15px; color: #555; }
.inline-form { display: flex; gap: 16px; align-items: center; flex-wrap: wrap; }
.inline-form label { font-size: 14px; color: #555; }
.inline-form input {
  padding: 8px 12px; border: 1px solid #ddd; border-radius: 6px;
  font-size: 14px; width: 120px; margin-left: 4px;
}
pre {
  background: #1a1a2e; color: #4fc3f7; padding: 16px; border-radius: 8px;
  font-size: 13px; overflow-x: auto; max-height: 400px; white-space: pre-wrap; word-break: break-all;
}
.btn { padding: 10px 20px; border: none; border-radius: 8px; font-size: 14px; cursor: pointer; color: #fff; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-primary { background: #2ecc71; }
</style>
