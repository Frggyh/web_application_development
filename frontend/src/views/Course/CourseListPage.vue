<template>
  <div class="course-list-page">
    <el-card class="box-card" header="课程列表">
      <div style="margin-bottom: 20px;">
        <el-button
            v-if="authStore.userRole === 'ADMIN'"
            type="primary"
            @click="openAddCourseDialog"
        >
          新增课程
        </el-button>
      </div>

      <el-table :data="courses" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="课程名称" />
        <el-table-column prop="college" label="开课学院" width="150" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="授课教师ID" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.teacherId" type="info">{{ scope.row.teacherId }}</el-tag>
            <el-tag v-else type="danger">未分配</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" v-if="authStore.userRole === 'ADMIN'">
          <template #default="scope">
            <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑课程对话框 -->
    <el-dialog
        v-model="dialogVisible"
        :title="isEditMode ? '编辑课程' : '新增课程'"
        width="500px"
    >
      <el-form
          :model="courseForm"
          :rules="rules"
          ref="formRef"
          label-width="100px"
          v-loading="dialogLoading"
      >
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="courseForm.name" placeholder="请输入课程名称" />
        </el-form-item>

        <el-form-item label="开课学院" prop="college">
          <el-input v-model="courseForm.college" placeholder="请输入开课学院" />
        </el-form-item>

        <el-form-item label="课程描述" prop="description">
          <el-input
              v-model="courseForm.description"
              type="textarea"
              :rows="3"
              placeholder="请输入课程描述"
          />
        </el-form-item>

        <el-form-item label="授课教师" prop="teacherId">
          <el-select
              v-model="courseForm.teacherId"
              placeholder="请选择授课教师"
              filterable
              clearable
              style="width: 100%;"
          >
            <el-option
                v-for="teacher in teachersList"
                :key="teacher.id"
                :label="`${teacher.username} (${teacher.title || '教师'})`"
                :value="teacher.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitCourseForm(formRef)" :loading="dialogLoading">
            {{ isEditMode ? '保存修改' : '立即创建' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/authStore'
import courseService from '@/api/courseService'
import { ElMessage, ElMessageBox } from 'element-plus'

const authStore = useAuthStore()
const courses = ref([])
const loading = ref(false)

// 对话框及表单状态
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const isEditMode = ref(false)
const currentCourseId = ref(null)

// 教师列表：在尽量不改后端的前提下，这里使用“前端静态配置/手工维护”的方式
// 如需真实数据，请根据实际教师账号，补充下面的数组
const teachersList = ref([
  // 示例：
  // { id: 2, username: 'teacher01', title: '讲师' },
  // { id: 3, username: 'teacher02', title: '教授' },
])

// 表单数据，与后端 CourseCreationRequest DTO 匹配
const courseForm = reactive({
  name: '',
  description: '',
  college: '',
  teacherId: null, // 存储教师 Long ID
})

const formRef = ref(null)
const rules = reactive({
  name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  college: [{ required: true, message: '请输入开课学院', trigger: 'blur' }],
  description: [{ required: true, message: '请输入课程描述', trigger: 'blur' }],
  teacherId: [{ required: true, message: '请选择授课教师', trigger: 'change', type: 'number' }],
})

// --- API 调用逻辑 ---

// 1. 获取课程列表
const fetchCourses = async () => {
  loading.value = true
  try {
    const response = await courseService.getCourses()
    // 后端 /api/courses 接口返回课程数组
    courses.value = response
  } catch (error) {
    ElMessage.error('加载课程列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 3. 打开新增课程对话框
const openAddCourseDialog = () => {
  isEditMode.value = false
  // 重置表单，并清空数据
  formRef.value?.resetFields()
  Object.assign(courseForm, { name: '', description: '', college: '', teacherId: null })

  dialogVisible.value = true
}

// 4. 处理编辑按钮点击
const handleEdit = (course) => {
  isEditMode.value = true
  currentCourseId.value = course.id
  // 填充表单数据
  Object.assign(courseForm, {
    name: course.name,
    description: course.description,
    college: course.college,
    teacherId: course.teacherId,
  })
  dialogVisible.value = true
}

// 5. 提交表单 (新增或编辑)
const submitCourseForm = async (formEl) => {
  if (!formEl) return
  await formEl.validate(async (valid) => {
    if (valid) {
      dialogLoading.value = true
      try {
        if (isEditMode.value) {
          // 编辑模式：PUT /api/courses/{id}
          await courseService.updateCourse(currentCourseId.value, courseForm)
          ElMessage.success('课程信息更新成功！')
        } else {
          // 新增模式：POST /api/courses
          await courseService.createCourse(courseForm)
          ElMessage.success('新课程创建成功！')
        }

        dialogVisible.value = false
        await fetchCourses() // 刷新列表
      } catch (error) {
        // 错误已在 request.js 拦截器中处理
        console.error(error)
      } finally {
        dialogLoading.value = false
      }
    }
  })
}

// 6. 处理删除操作
const handleDelete = async (courseId) => {
  try {
    await ElMessageBox.confirm('确定要删除该课程吗？删除后数据不可恢复！', '警告', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    // 调用删除接口
    await courseService.deleteCourse(courseId)
    ElMessage.success('课程删除成功')
    fetchCourses() // 重新加载列表

  } catch (error) {
    if (error === 'cancel') return // 用户取消操作
    console.error(error)
  }
}

onMounted(() => {
  fetchCourses()
})
</script>

<style scoped>
.course-list-page {
  padding: 20px;
}

.box-card {
  border-radius: 8px;
}
</style>