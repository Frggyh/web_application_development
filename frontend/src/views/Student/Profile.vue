<template>
  <div class="profile-container">
    <el-row :gutter="20">
      <!-- 个人信息卡片 -->
      <el-col :span="12">
        <el-card header="个人信息">
          <div style="text-align: center; margin-bottom: 20px;">
            <el-avatar
                :size="100"
                :src="avatarUrl"
                @error="handleAvatarLoadError"
            >
              <el-icon><User /></el-icon>
            </el-avatar>
            <div style="margin-top: 10px;">
              <el-upload
                  :action="avatarUploadUrl"
                  :headers="uploadHeaders"
                  :on-success="handleAvatarSuccess"
                  :on-error="handleAvatarUploadError"
                  :show-file-list="false"
                  accept="image/*"
                  :before-upload="beforeAvatarUpload"
              >
                <el-button type="primary" size="small">更换头像</el-button>
              </el-upload>
            </div>
          </div>

          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户名">
              {{ profileData.username }}
            </el-descriptions-item>
            <el-descriptions-item label="邮箱">
              <span v-if="!editingEmail">{{ profileData.email || '未设置' }}</span>
              <el-input
                  v-else
                  v-model="profileData.email"
                  size="small"
                  style="width: 200px"
                  placeholder="请输入邮箱"
              />
              <el-button
                  v-if="!editingEmail"
                  type="text"
                  size="small"
                  @click="editingEmail = true"
                  style="margin-left: 10px"
              >
                编辑
              </el-button>
              <el-button
                  v-else
                  type="text"
                  size="small"
                  @click="saveEmail"
                  style="margin-left: 10px"
              >
                保存
              </el-button>
              <el-button
                  v-if="editingEmail"
                  type="text"
                  size="small"
                  @click="cancelEditEmail"
              >
                取消
              </el-button>
            </el-descriptions-item>
            <el-descriptions-item label="角色">
              <el-tag type="primary">学生</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="注册时间">
              {{ profileData.createdAt || '未知' }}
            </el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">学习统计</el-divider>

          <el-row :gutter="20" style="margin-top: 20px;">
            <el-col :span="8">
              <el-statistic title="提问数" :value="stats.questionCount">
                <template #suffix>个</template>
              </el-statistic>
            </el-col>
            <el-col :span="8">
              <el-statistic title="已回答" :value="stats.answeredCount">
                <template #suffix>个</template>
              </el-statistic>
            </el-col>
            <el-col :span="8">
              <el-statistic title="资源下载" :value="stats.downloadCount">
                <template #suffix>次</template>
              </el-statistic>
            </el-col>
          </el-row>
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

    <!-- 快捷入口 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span>我的资源管理</span>
              <el-button type="primary" size="small" @click="goToResourceUpload">
                前往管理
              </el-button>
            </div>
          </template>
          <p style="color: #909399; margin: 0;">
            查看、编辑或删除您上传的学习资源
          </p>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span>我的问题管理</span>
              <el-button type="primary" size="small" @click="goToAskQuestion">
                前往管理
              </el-button>
            </div>
          </template>
          <p style="color: #909399; margin: 0;">
            查看、编辑或删除您提出的问题，查看教师的回答
          </p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import request from '@/utils/request'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { User } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const changingPassword = ref(false)
const editingEmail = ref(false)
const originalEmail = ref('')

// 个人信息数据
const profileData = reactive({
  username: '',
  role: '',
  createdAt: '',
  email: '',
  avatar: ''
})

// 头像URL - 使用blob URL
const avatarUrl = ref('')

// 加载头像
const loadAvatar = async () => {
  if (!profileData.avatar) {
    avatarUrl.value = ''
    return
  }
  
  try {
    const filename = profileData.avatar.split('/').pop().split('?')[0]
    // 使用axios直接调用（可以设置responseType为blob）
    const token = authStore.token
    const response = await axios.get(`http://localhost:8080/api/profile/avatar/${filename}`, {
      responseType: 'blob',
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    
    // 将blob转换为URL
    const blobUrl = URL.createObjectURL(response.data)
    // 释放旧的blob URL（避免内存泄漏）
    if (avatarUrl.value && avatarUrl.value.startsWith('blob:')) {
      URL.revokeObjectURL(avatarUrl.value)
    }
    avatarUrl.value = blobUrl
  } catch (error) {
    console.error('加载头像失败:', error)
    avatarUrl.value = ''
  }
}

// 监听头像变化
watch(() => profileData.avatar, () => {
  loadAvatar()
}, { immediate: true })

// 头像上传配置
const avatarUploadUrl = computed(() => {
  return 'http://localhost:8080/api/profile/avatar'
})

const uploadHeaders = computed(() => {
  const token = authStore.token
  return {
    Authorization: `Bearer ${token}`
  }
})

// 学习统计数据
const stats = reactive({
  questionCount: 0,
  answeredCount: 0,
  downloadCount: 0
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
    Object.assign(profileData, {
      username: response.username || authStore.user?.username,
      role: response.role || authStore.user?.role,
      createdAt: response.createdAt || '未知',
      email: response.email || '',
      avatar: response.avatar || ''
    })
    originalEmail.value = response.email || ''
  } catch (error) {
    ElMessage.error('加载个人信息失败')
    console.error(error)
  }
}

// 保存邮箱
const saveEmail = async () => {
  try {
    await request.put('/profile', {
      email: profileData.email
    })
    ElMessage.success('邮箱更新成功')
    originalEmail.value = profileData.email
    editingEmail.value = false
    // 更新 authStore 中的用户信息
    if (authStore.user) {
      authStore.user.email = profileData.email
    }
  } catch (error) {
    ElMessage.error('邮箱更新失败')
    console.error(error)
  }
}

// 取消编辑邮箱
const cancelEditEmail = () => {
  profileData.email = originalEmail.value
  editingEmail.value = false
}

// 头像上传前验证
const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB!')
    return false
  }
  return true
}

// 头像上传成功
const handleAvatarSuccess = (response, file) => {
  // el-upload 的 response 可能是原始响应对象，需要检查结构
  const data = response.data || response
  if (data && data.avatar) {
    // 移除可能存在的查询参数
    const cleanAvatar = data.avatar.split('?')[0]
    profileData.avatar = cleanAvatar
    ElMessage.success(data.message || '头像上传成功')
    // 更新 authStore 中的用户信息
    if (authStore.user) {
      authStore.user.avatar = cleanAvatar
    }
    // 重新加载头像
    loadAvatar()
  } else {
    ElMessage.error(data?.error || '头像上传失败')
  }
}

// 头像上传失败
const handleAvatarUploadError = (error, file) => {
  console.error('头像上传失败:', error)
  ElMessage.error('头像上传失败，请重试')
}

// 头像加载失败（图片显示失败）
const handleAvatarLoadError = () => {
  // 静默处理，不显示错误消息（因为可能是头像未设置）
  console.log('头像图片加载失败，使用默认图标')
}

// 获取学习统计（调用后端接口）
const fetchStats = async () => {
  try {
    const response = await request.get('/profile/stats')
    Object.assign(stats, {
      questionCount: response.questionCount || 0,
      answeredCount: response.answeredCount || 0,
      downloadCount: response.downloadCount || 0
    })
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
  }
}

// 导航到资源管理页面
const goToResourceUpload = () => {
  router.push('/student/resources/upload')
}

// 导航到问题管理页面
const goToAskQuestion = () => {
  router.push('/student/qa/ask')
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
  fetchProfile().then(() => {
    // 个人信息加载完成后加载头像
    loadAvatar()
  })
  fetchStats()
})
</script>

<style scoped>
.profile-container {
  padding: 20px;
}
</style>