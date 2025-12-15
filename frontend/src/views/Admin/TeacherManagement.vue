<template>
  <div class="teacher-management-container">
    <el-card shadow="hover">
      <div class="header">
        <div>
          <h2>教师账号管理</h2>
          <p class="sub-title">在此对教师账号进行新增、编辑、删除等操作。</p>
        </div>
        <el-button type="primary" @click="openCreateDialog">新增教师</el-button>
      </div>

      <el-table
          :data="teachers"
          v-loading="loading"
          border
          stripe
          class="mt-20"
          empty-text="暂无教师数据"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="title" label="职称" min-width="140">
          <template #default="scope">
            <el-tag v-if="scope.row.title" type="success">{{ scope.row.title }}</el-tag>
            <el-tag v-else type="info">未填写</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="introduction" label="简介" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button size="small" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button
                size="small"
                type="danger"
                @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
            layout="total, prev, pager, next, sizes"
            :page-size="pagination.size"
            :current-page="pagination.page"
            :page-sizes="[5, 10, 20, 50]"
            :total="pagination.total"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
            background
        />
      </div>
    </el-card>

    <el-dialog
        v-model="dialogVisible"
        :title="isEditMode ? '编辑教师' : '新增教师'"
        width="480px"
    >
      <el-form
          ref="formRef"
          :model="teacherForm"
          :rules="formRules"
          label-width="90px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="teacherForm.username" placeholder="请输入教师用户名" :disabled="isEditMode" />
        </el-form-item>

        <el-form-item label="密码" prop="password" v-if="!isEditMode">
          <el-input v-model="teacherForm.password" placeholder="请输入登录密码" show-password />
        </el-form-item>

        <el-form-item label="职称" prop="title">
          <el-input v-model="teacherForm.title" placeholder="例如：讲师、副教授" />
        </el-form-item>

        <el-form-item label="简介" prop="introduction">
          <el-input
              v-model="teacherForm.introduction"
              type="textarea"
              :rows="3"
              placeholder="可填写擅长领域、教学经验等"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="dialogSubmitting">
            {{ isEditMode ? '保存修改' : '立即创建' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import teacherService from '@/api/teacherService'

const teachers = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogSubmitting = ref(false)
const isEditMode = ref(false)
const currentTeacherId = ref(null)

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

const formRef = ref(null)
const defaultForm = {
  username: '',
  password: '',
  title: '',
  introduction: '',
}
const teacherForm = reactive({ ...defaultForm })

const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, message: '用户名至少 4 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 个字符', trigger: 'blur' },
  ],
}

const fetchTeachers = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1,
      size: pagination.size,
    }
    const response = await teacherService.getTeachers(params)
    if (response?.content) {
      teachers.value = response.content
      pagination.total = response.totalElements || response.content.length
    } else if (Array.isArray(response)) {
      teachers.value = response
      pagination.total = response.length
    } else {
      teachers.value = []
      pagination.total = 0
    }
  } catch (error) {
    console.error('加载教师列表失败：', error)
    ElMessage.error('加载教师列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  isEditMode.value = false
  currentTeacherId.value = null
  Object.assign(teacherForm, defaultForm)
  dialogVisible.value = true
}

const openEditDialog = (teacher) => {
  isEditMode.value = true
  currentTeacherId.value = teacher.id
  Object.assign(teacherForm, {
    username: teacher.username,
    password: '',
    title: teacher.title || '',
    introduction: teacher.introduction || '',
  })
  dialogVisible.value = true
}

const handleSubmit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    dialogSubmitting.value = true
    try {
      if (isEditMode.value) {
        await teacherService.updateTeacher(currentTeacherId.value, {
          title: teacherForm.title,
          introduction: teacherForm.introduction,
        })
        ElMessage.success('教师信息已更新')
      } else {
        await teacherService.createTeacher({
          username: teacherForm.username,
          password: teacherForm.password,
          title: teacherForm.title,
          introduction: teacherForm.introduction,
        })
        ElMessage.success('教师创建成功')
      }
      dialogVisible.value = false
      await fetchTeachers()
    } catch (error) {
      console.error('提交教师信息失败：', error)
    } finally {
      dialogSubmitting.value = false
    }
  })
}

const handleDelete = (teacher) => {
  ElMessageBox.confirm(
      `确定删除教师「${teacher.username}」吗？该操作不可撤销。`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
  )
    .then(async () => {
      try {
        await teacherService.deleteTeacher(teacher.id)
        ElMessage.success('教师删除成功')
        // 如果当前页只有一个数据被删除，且不是第一页，则自动回退一页
        if (teachers.value.length === 1 && pagination.page > 1) {
          pagination.page -= 1
        }
        await fetchTeachers()
      } catch (error) {
        console.error('删除教师失败：', error)
      }
    })
    .catch(() => {})
}

const handleSizeChange = (size) => {
  pagination.size = size
  pagination.page = 1
  fetchTeachers()
}

const handlePageChange = (page) => {
  pagination.page = page
  fetchTeachers()
}

onMounted(() => {
  fetchTeachers()
})
</script>

<style scoped>
.teacher-management-container {
  padding: 20px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.sub-title {
  margin: 4px 0 0;
  color: #909399;
  font-size: 13px;
}

.mt-20 {
  margin-top: 20px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>