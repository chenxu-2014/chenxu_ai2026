<template>
  <div>
    <h1 class="page-title">📊 仪表盘</h1>
    <div class="dashboard-grid">
      <div class="card" v-for="card in cards" :key="card.title" @click="$router.push(card.path)">
        <span class="card-icon">{{ card.icon }}</span>
        <h3>{{ card.title }}</h3>
        <p>{{ card.desc }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const cards = [
  { icon: '⚡', title: '电表数据', desc: '1000w 数据 Redis 加载 / 生成', path: '/meter' },
  { icon: '🔍', title: '数据比对', desc: '游标分页 + HashMap 差异比对', path: '/compare' },
  { icon: '📨', title: 'Kafka 消息', desc: '消息发送 / 有序生产', path: '/kafka' },
  { icon: '🗄️', title: 'Redis 类型', desc: 'String/List/Set/ZSet/Hash/Stream...', path: '/redis' },
  { icon: '🛡️', title: '熔断限流', desc: 'Sentinel 流控 / 降级 / 慢调用', path: '/sentinel' },
  { icon: '💳', title: '交易链路', desc: '下单 → 库存 → 支付 埋点', path: '/transaction' }
]
</script>

<style scoped>
.page-title {
  margin: 0 0 24px;
  color: #333;
}
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}
.card {
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
}
.card-icon {
  font-size: 36px;
  display: block;
  margin-bottom: 12px;
}
.card h3 {
  margin: 0 0 8px;
  color: #333;
}
.card p {
  margin: 0;
  font-size: 13px;
  color: #888;
}
</style>
