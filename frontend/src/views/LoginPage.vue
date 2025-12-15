<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <span>在线学习平台 - 登录</span>
        </div>
      </template>

      <el-form
          :model="loginForm"
          :rules="rules"
          ref="loginFormRef"
          label-width="80px"
          @keyup.enter="submitForm(loginFormRef)"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>

        <el-form-item>
          <el-button
              type="primary"
              @click="submitForm(loginFormRef)"
              :loading="loading"
              style="width: 100%;"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="footer-links">
        <el-link type="info" :underline="false" @click="goToRegister">
          没有账号？注册学生账号
        </el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import { ElMessage } from 'element-plus'; // 导入 Element Plus 消息组件

const router = useRouter();
const authStore = useAuthStore();

// 表单数据和引用
const loginFormRef = ref(null);
const loading = ref(false);

const loginForm = reactive({
  username: '',
  password: '',
});

// 表单验证规则
const rules = reactive({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
});

// 提交表单逻辑
const submitForm = async (formEl) => {
  if (!formEl) return;

  // 验证表单
  await formEl.validate(async (valid, fields) => {
    if (valid) {
      loading.value = true;
      try {
        const result = await authStore.loginAction(loginForm);

        // ★★★ 修正点 3: 根据存储在 Pinia 中的角色进行跳转 ★★★
        if (authStore.user.role === 'ADMIN') {
          router.push('/admin/dashboard'); // 假设管理员主页是这个路径
        } else if (authStore.user.role === 'TEACHER') {
          router.push('/teacher/dashboard'); // 教师跳转路径
        } else {
          router.push('/'); // 默认跳转
        }

        ElMessage.success('登录成功！');

      } catch (error) {
        // 错误已在 store 中处理并显示 ElMessage
      } finally {
        loading.value = false;
      }
    } else {
      ElMessage.warning('表单验证失败，请检查输入项');
    }
  });
};

const goToRegister = () => {
  router.push('/register');
};
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa;
}

.login-card {
  width: 400px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  text-align: center;
  font-size: 18px;
  font-weight: bold;
}

.footer-links {
  margin-top: 15px;
  text-align: center;
}
</style>