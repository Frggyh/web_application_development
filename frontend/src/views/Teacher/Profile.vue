<template>
  <div class="profile-container">
    <el-row :gutter="20">
      <!-- 个人信息卡片 -->
      <el-col :span="12">
        <el-card header="个人信息">
          <el-form
              :model="profileForm"
              :rules="profileRules"
              ref="profileFormRef"
              label-width="100px"
          >
            <el-form-item label="用户名">
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>

            <el-form-item label="角色">
              <el-tag type="success">{{ profileForm.role }}</el-tag>
            </el-form-item>

            <el-form-item label="职称" prop="title">
              <el-input
                  v-model="profileForm.title"
                  placeholder="例如：副教授、讲师"
              />
            </el-form-item>

            <el-form-item label="个人简介" prop="introduction">
              <el-input
                  v-model="profileForm.introduction"
                  type="textarea"
                  :rows="5"
                  placeholder="请输入个人简介"
                  maxlength="500"
                  show-word-limit
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="updateProfile" :loading="updating">
                保存修改
              </el-button>
              <el-button @click="resetProfileForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 修改密码卡片 -->
      <el-col :span="12">
        <el-card header="修改密码">
          <el-form
              :model="passwordForm"
              :rules="passwordRules"
              ref="passwordFormRef"
              label-width="100px"
          >
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input
                  v-model="passwordForm.oldPassword"
                  type="password"
                  placeholder="请输入旧密码"
                  show-password
              />
            </el-form-item>

            <el-form-item label="新密码" prop="newPassword">
              <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  placeholder="请输入新密码（至少6位）"
                  show-password
              />
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入新密码"
                  show-password
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="updatePassword" :loading="changingPassword">
                修改密码
              </el-button>
              <el-button @click="resetPasswordForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/authStore'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()

const updating = ref(false)
const changingPassword = ref(false)

// 个人信息表单
const profileForm = reactive({
  username: '',
  role: '',
  title: '',
  introduction: ''
})

const profileFormRef = ref(null)
const profileRules = reactive({
  title: [{ max: 100, message: '职称长度不能超过100个字符', trigger: 'blur' }],
  introduction: [{ max: 500, message: '简介长度不能超过500个字符', trigger: 'blur' }]
})

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordFormRef = ref(null)

// 自定义确认密码验证
const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = reactive({
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }]
})

// 获取个人信息
const fetchProfile = async () => {
  try {
    const response = await request.get('/profile')
    Object.assign(profileForm, {
      username: response.username || authStore.user?.username,
      role: response.role || authStore.user?.role,
      title: response.title || '',
      introduction: response.introduction || ''
    })
  } catch (error) {
    ElMessage.error('加载个人信息失败')
    console.error(error)
  }
}

// 更新个人信息
const updateProfile = async () => {
  if (!profileFormRef.value) return

  await profileFormRef.value.validate(async (valid) => {
    if (!valid) return

    updating.value = true
    try {
      await request.put('/profile', {
        title: profileForm.title,
        introduction: profileForm.introduction
      })

      ElMessage.success('个人信息更新成功')
      await fetchProfile()
    } catch (error) {
      console.error(error)
    } finally {
      updating.value = false
    }
  })
}

// 修改密码
const updatePassword = async () => {
  if (!passwordFormRef.value) return

  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return

    changingPassword.value = true
    try {
      await request.put('/profile/password', {
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })

      ElMessage.success('密码修改成功，请重新登录')

      // 清空密码表单
      resetPasswordForm()

      // 延迟登出
      setTimeout(() => {
        authStore.logout()
        window.location.href = '/login'
      }, 2000)
    } catch (error) {
      console.error(error)
    } finally {
      changingPassword.value = false
    }
  })
}

// 重置个人信息表单
const resetProfileForm = () => {
  fetchProfile()
}

// 重置密码表单
const resetPasswordForm = () => {
  passwordFormRef.value?.resetFields()
  Object.assign(passwordForm, {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  })
}

onMounted(() => {
  fetchProfile()
})
</script>

<style scoped>
.profile-container {
  padding: 20px;
}
</style>