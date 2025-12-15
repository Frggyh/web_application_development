<template>
  <div class="not-found-container">
    <el-result
      icon="error"
      title="404 页面未找到"
      sub-title="抱歉，您访问的页面不存在，或者您没有访问该页面的权限。"
    >
      <template #icon>
        <el-icon color="#F56C6C" size="80"><CloseBold /></el-icon>
      </template>

      <template #extra>
        <el-button type="primary" size="large" @click="goToDashboard">
          返回工作台/首页
        </el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import { CloseBold } from '@element-plus/icons-vue'; // 导入 Element Plus 核心图标

const router = useRouter();
const authStore = useAuthStore();

// 根据用户角色智能跳转到对应的首页
const goToDashboard = () => {
    if (authStore.isLoggedIn) {
        const role = authStore.userRole;
        switch (role) {
            case 'ADMIN':
                router.push('/admin/dashboard');
                break;
            case 'TEACHER':
                router.push('/teacher/dashboard');
                break;
            case 'STUDENT':
                router.push('/student/resources');
                break;
            default:
                router.push('/');
        }
    } else {
        // 如果未登录，直接跳转到登录页
        router.push('/login');
    }
};
</script>

<style scoped>
.not-found-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100%;
  background-color: #ffffff;
}
</style>