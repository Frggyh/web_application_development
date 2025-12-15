<template>
  <div class="admin-dashboard-container">
    <el-card>
      <h2>🎉 欢迎，{{ user?.username }}</h2>
      <el-divider />
      <p>这是管理员控制台的首页，您可以在这里查看系统概览数据和待处理任务。</p>

    </el-card>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/authStore';
import { computed } from 'vue';

const authStore = useAuthStore();
const user = computed(() => authStore.user);

// test user 应该是 { username: 'admin01', role: 'ADMIN' }
</script>

<style scoped>
.admin-dashboard-container {
  padding: 20px;
}
</style>