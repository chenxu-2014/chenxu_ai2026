<template>
  <div class="main-layout">
    <AppSidebar />
    <div class="main-content">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import AppSidebar from '../components/AppSidebar.vue'
</script>

<style scoped>
.main-layout {
  display: flex;
  min-height: 100vh;
  background: #f0f2f5;
}
.main-content {
  margin-left: 220px;
  flex: 1;
  padding: 24px;
}
</style>
