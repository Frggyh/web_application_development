<template>
  <el-container class="admin-layout">
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <el-text tag="b" style="color: white;">管理员控制台</el-text>
      </div>

      <el-menu
          :default-active="$route.path"
          class="el-menu-vertical-demo"
          background-color="#545c64"
          text-color="#fff"
          active-text-color="#ffd04b"
          router
      >
        <el-menu-item index="/admin/dashboard">
          <span>首页</span>
        </el-menu-item>

        <el-menu-item index="/admin/courses">
          <span>课程管理</span>
        </el-menu-item>

        <el-menu-item index="/admin/teachers">
          <span>教师账号管理</span>
        </el-menu-item>

        <el-menu-item index="/admin/users">
          <span>用户管理</span>
        </el-menu-item>

        <el-menu-item index="/admin/resources">
          <span>学习资源管理</span>
        </el-menu-item>

        <el-menu-item index="/admin/qa">
          <span>问答内容管理</span>
        </el-menu-item>

      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>管理首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title || '首页' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="user-info">
          <el-dropdown trigger="click">
            <span class="el-dropdown-link">
              欢迎您，{{ authStore.user?.username }} ({{ authStore.userRole }})
              <el-icon class="el-icon--right"><i class="el-icon-arrow-down"></i></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import { ElMessage } from 'element-plus';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

// 处理登出
const handleLogout = () => {
  authStore.logout(); // 清除 Pinia 状态和 Token
  ElMessage.success('您已安全退出。');
  router.push('/login'); // 跳转回登录页
};
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}

.sidebar {
  background-color: #545c64;
  color: white;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  border-bottom: 1px solid #4a5057;
}

.el-menu-vertical-demo:not(.el-menu--collapse) {
  width: 200px;
  min-height: 400px;
  border-right: none;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid #eee;
  background-color: #fff;
}

.user-info {
  cursor: pointer;
  color: #333;
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
  /* 确保内容可以滚动 */
  overflow: auto;
}
</style>