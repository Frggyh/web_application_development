<template>
    <el-container class="teacher-layout">
        <el-aside width="200px" class="sidebar">
            <div class="logo">
                <el-text tag="b" style="color: white;">教师工作台</el-text>
            </div>

            <el-menu
            :default-active="$route.path"
            class="el-menu-vertical-demo"
            background-color="#545c64"
            text-color="#fff"
            active-text-color="#ffd04b"
            router
            >
            <el-menu-item index="/teacher/dashboard">
                <span>首页</span>
            </el-menu-item>

            <el-menu-item index="/teacher/courses">
                <span>我的课程</span>
            </el-menu-item>

            <el-menu-item index="/teacher/qa-duty">
                <span>答疑区提醒</span>
            </el-menu-item>

            <el-menu-item index="/teacher/resources/upload">
                <span>资源上传与管理</span>
            </el-menu-item>

            <el-menu-item index="/teacher/profile">
                <span>个人信息修改</span>
            </el-menu-item>

        </el-menu>
    </el-aside>

    <el-container>
        <el-header class="header">
            <div class="breadcrumb">
                <el-breadcrumb separator="/">
                    <el-breadcrumb-item>教师工作台</el-breadcrumb-item>
                    <el-breadcrumb-item>{{ $route.meta.title || '首页' }}</el-breadcrumb-item>
                </el-breadcrumb>
            </div>

            <div class="user-info">
                <el-dropdown trigger="click">
            <span class="el-dropdown-link">
              欢迎您，{{ authStore.user?.username }} ({{ authStore.user?.title || '教师' }})
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
    import { useRouter } from 'vue-router';
    import { useAuthStore } from '@/stores/authStore';
    import { ElMessage } from 'element-plus';

    const router = useRouter();
    const authStore = useAuthStore();

    const handleLogout = () => {
    authStore.logout();
    ElMessage.success('您已安全退出。');
    router.push('/login');
};
</script>

<style scoped>
    /* 样式与 AdminLayout.vue 保持一致 */
    .teacher-layout { height: 100vh; }
    .sidebar { background-color: #545c64; color: white; }
    .logo { height: 60px; line-height: 60px; text-align: center; border-bottom: 1px solid #4a5057; }
    .header { display: flex; justify-content: space-between; align-items: center; padding: 0 20px; border-bottom: 1px solid #eee; background-color: #fff; }
    .user-info { cursor: pointer; color: #333; }
    .main-content { background-color: #f0f2f5; padding: 20px; overflow: auto; }
</style>