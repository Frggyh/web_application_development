<template>
  <div class="user-management-container">
    <el-card shadow="hover">
      <div class="header">
        <div>
          <h2>系统用户管理</h2>
          <p class="sub-title">查看、筛选并维护全体用户账号（学生 / 教师 / 管理员）。</p>
        </div>
      </div>

      <div class="filters">
        <el-select
            v-model="filters.role"
            placeholder="按角色筛选"
            clearable
            style="width: 180px"
        >
          <el-option label="全部角色" value="" />
          <el-option label="管理员" value="ADMIN" />
          <el-option label="教师" value="TEACHER" />
          <el-option label="学生" value="STUDENT" />
        </el-select>

        <el-input
            v-model="filters.keyword"
            placeholder="输入用户名关键字"
            style="width: 220px"
            clearable
            @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table
          :data="users"
          v-loading="loading"
          stripe
          border
          empty-text="暂无用户数据"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="scope">
            <el-tag :type="roleTagType(scope.row.role)">
              {{ translateRole(scope.row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="职称/职位" min-width="160">
          <template #default="scope">
            <span>{{ scope.row.title || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="introduction" label="简介" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button size="small" @click="openEditDialog(scope.row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
            background
            layout="total, prev, pager, next, sizes"
            :current-page="pagination.page"
            :page-size="pagination.size"
            :page-sizes="[5, 10, 20, 50]"
            :total="pagination.total"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog
        v-model="dialogVisible"
        title="编辑用户"
        width="500px"
    >
      <el-form
          ref="formRef"
          :model="userForm"
          :rules="formRules"
          label-width="100px"
      >
        <el-form-item label="用户名">
          <el-input :value="userForm.username" disabled />
        </el-form-item>

        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="请选择角色">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
        </el-form-item>

        <el-form-item label="新密码" prop="password">
          <el-input
              v-model="userForm.password"
              placeholder="留空表示不修改密码"
              show-password
          />
        </el-form-item>

        <el-form-item label="职称/职位">
          <el-input v-model="userForm.title" placeholder="教师可填写职称，其他角色可选填" />
        </el-form-item>

        <el-form-item label="简介">
          <el-input
              v-model="userForm.introduction"
              type="textarea"
              :rows="3"
              placeholder="可记录教师简介或其他说明"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="dialogSubmitting" @click="handleSubmit">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import userService from '@/api/userService'

const users = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogSubmitting = ref(false)
const currentUserId = ref(null)

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

const filters = reactive({
  role: '',
  keyword: '',
})

const formRef = ref(null)
const defaultForm = {
  username: '',
  role: '',
  password: '',
  title: '',
  introduction: '',
}
const userForm = reactive({ ...defaultForm })

const formRules = {
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: [{ min: 6, message: '密码至少 6 个字符', trigger: 'blur' }],
}

const roleTagType = (role) => {
  switch ((role || '').toUpperCase()) {
    case 'ADMIN':
      return 'danger'
    case 'TEACHER':
      return 'success'
    case 'STUDENT':
      return 'info'
    default:
      return 'default'
  }
}

const translateRole = (role) => {
  switch ((role || '').toUpperCase()) {
    case 'ADMIN':
      return '管理员'
    case 'TEACHER':
      return '教师'
    case 'STUDENT':
      return '学生'
    default:
      return '未知'
  }
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1,
      size: pagination.size,
    }
    if (filters.role) params.role = filters.role
    if (filters.keyword) params.keyword = filters.keyword

    const response = await userService.getUsers(params)

    if (response?.content) {
      users.value = response.content
      pagination.total = response.totalElements ?? response.content.length
    } else if (Array.isArray(response)) {
      users.value = response
      pagination.total = response.length
    } else {
      users.value = []
      pagination.total = 0
    }
  } catch (error) {
    console.error('加载用户列表失败：', error)
    ElMessage.error('加载用户列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchUsers()
}

const handleReset = () => {
  filters.role = ''
  filters.keyword = ''
  pagination.page = 1
  fetchUsers()
}

const handleSizeChange = (size) => {
  pagination.size = size
  pagination.page = 1
  fetchUsers()
}

const handlePageChange = (page) => {
  pagination.page = page
  fetchUsers()
}

const openEditDialog = (user) => {
  currentUserId.value = user.id
  Object.assign(userForm, {
    username: user.username,
    role: user.role,
    password: '',
    title: user.title || '',
    introduction: user.introduction || '',
  })
  dialogVisible.value = true
}

const handleSubmit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    dialogSubmitting.value = true
    try {
      const payload = {
        role: userForm.role,
        title: userForm.title,
        introduction: userForm.introduction,
      }
      if (userForm.password) {
        payload.password = userForm.password
      }

      await userService.updateUser(currentUserId.value, payload)
      ElMessage.success('用户信息已更新')
      dialogVisible.value = false
      await fetchUsers()
    } catch (error) {
      console.error('更新用户失败：', error)
    } finally {
      dialogSubmitting.value = false
    }
  })
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.user-management-container {
  padding: 20px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sub-title {
  margin: 4px 0 0;
  color: #909399;
  font-size: 13px;
}

.filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin: 20px 0;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>