<template>
  <div class="my-courses-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的课程</span>
          <el-button type="primary" @click="openAddCourseDialog">
            创建新课程
          </el-button>
        </div>
      </template>

      <el-empty v-if="courses.length === 0 && !loading" description="您还没有创建任何课程">
        <el-button type="primary" @click="openAddCourseDialog">创建第一门课程</el-button>
      </el-empty>

      <el-table :data="courses" v-loading="loading" style="width: 100%" v-else>
        <el-table-column prop="id" label="课程ID" width="80" />
        <el-table-column prop="name" label="课程名称" min-width="150" />
        <el-table-column prop="college" label="开课学院" width="120" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />

        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="info" @click="viewResources(scope.row.id)">资源管理</el-button>
            <el-button size="small" type="warning" @click="viewQuestions(scope.row.id)">答疑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑课程对话框 -->
    <el-dialog
        v-model="dialogVisible"
        :title="isEditMode ? '编辑课程信息' : '创建新课程'"
        width="500px"
    >
      <el-form
          :model="courseForm"
          :rules="rules"
          ref="formRef"
          label-width="100px"
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
              :rows="4"
              placeholder="请输入课程描述"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">
          {{ isEditMode ? '保存修改' : '创建课程' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import courseService from '@/api/courseService'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

const courses = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEditMode = ref(false)
const submitting = ref(false)
const currentCourseId = ref(null)

const courseForm = reactive({
  name: '',
  college: '',
  description: ''
})

const formRef = ref(null)
const rules = reactive({
  name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  college: [{ required: true, message: '请输入开课学院', trigger: 'blur' }],
  description: [{ required: true, message: '请输入课程描述', trigger: 'blur' }]
})

// 获取我的课程列表
const fetchMyCourses = async () => {
  loading.value = true
  try {
    const response = await courseService.getCourses()
    // 过滤出当前教师创建的课程
    const userId = authStore.user?.id
    courses.value = response.filter(course => course.teacherId === userId)
  } catch (error) {
    ElMessage.error('加载课程列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 打开创建课程对话框
const openAddCourseDialog = () => {
  isEditMode.value = false
  formRef.value?.resetFields()
  Object.assign(courseForm, { name: '', college: '', description: '' })
  dialogVisible.value = true
}

// 编辑课程
const handleEdit = (course) => {
  isEditMode.value = true
  currentCourseId.value = course.id
  Object.assign(courseForm, {
    name: course.name,
    college: course.college,
    description: course.description
  })
  dialogVisible.value = true
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      if (isEditMode.value) {
        await courseService.updateCourse(currentCourseId.value, courseForm)
        ElMessage.success('课程信息更新成功')
      } else {
        await courseService.createCourse(courseForm)
        ElMessage.success('课程创建成功')
      }

      dialogVisible.value = false
      await fetchMyCourses()
    } catch (error) {
      console.error(error)
    } finally {
      submitting.value = false
    }
  })
}

// 查看课程资源
const viewResources = (courseId) => {
  ElMessage.info('跳转到资源管理页面（待实现）')
  // 这里可以后续扩展到课程级别的资源管理页面
}

// 查看课程问答：跳转到教师答疑中心，并带上课程 ID 作为查询参数
const viewQuestions = (courseId) => {
  router.push({ name: 'TeacherQADuty', query: { courseId } })
}

onMounted(() => {
  fetchMyCourses()
})
</script>

<style scoped>
.my-courses-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>