<template>
  <div class="register-container">
    <el-card class="register-card">
      <template #header>
        <div class="card-header">
          <span>学生账号注册</span>
        </div>
      </template>

      <el-form
          :model="registerForm"
          :rules="rules"
          ref="registerFormRef"
          label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="设置密码" show-password />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
        </el-form-item>

        <el-form-item>
          <el-button
              type="primary"
              @click="submitForm(registerFormRef)"
              :loading="loading"
              style="width: 100%;"
          >
            立即注册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="footer-links">
        <el-link type="info" :underline="false" @click="goToLogin">
          已有账号？去登录
        </el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import registerService from '@/api/registerService';
import { ElMessage } from 'element-plus';

const router = useRouter();
const registerFormRef = ref(null);
const loading = ref(false);

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
});

// 自定义确认密码验证规则
const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'));
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致!'));
  } else {
    callback();
  }
};

// 表单验证规则
const rules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 15, message: '用户名长度需在 3 到 15 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, validator: validatePass2, trigger: 'blur' }
  ]
});

// 提交表单逻辑
const submitForm = async (formEl) => {
  if (!formEl) return;

  await formEl.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        // 只需要发送 username 和 password 字段给后端
        await registerService.registerStudent({
          username: registerForm.username,
          password: registerForm.password,
        });

        ElMessage.success('注册成功！请使用您的账号登录。');
        router.push('/login'); // 注册成功后跳转到登录页

      } catch (error) {
        // 假设后端返回 409 Conflict 或 400 Bad Request
        const message = error.response?.data?.message || '注册失败，请检查用户名是否已存在。';
        ElMessage.error(message);
        console.error("注册失败:", error);
      } finally {
        loading.value = false;
      }
    } else {
      ElMessage.warning('表单验证失败，请检查输入项');
    }
  });
};

const goToLogin = () => {
  router.push('/login');
};
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa;
}

.register-card {
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