<template>
  <div class="ask-question-container">
    <el-card header="我要提问">
      <el-alert
          title="提示"
          type="info"
          description="请详细描述您的问题，以便教师能够更好地帮助您解答。"
          :closable="false"
          style="margin-bottom: 20px;"
      />

      <el-form
          :model="questionForm"
          :rules="rules"
          ref="formRef"
          label-width="100px"
      >
        <el-form-item label="选择课程" prop="courseId">
          <el-select
              v-model="questionForm.courseId"
              placeholder="请选择要提问的课程"
              style="width: 100%;"
              filterable
          >
            <el-option
                v-for="course in courses"
                :key="course.id"
                :label="course.name"
                :value="course.id"
            >
              <span>{{ course.name }}</span>
              <span style="float: right; color: #8492a6; font-size: 13px;">
                {{ course.college }}
              </span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="问题标题" prop="title">
          <el-input
              v-model="questionForm.title"
              placeholder="请输入问题标题（简明扼要）"
              maxlength="255"
              show-word-limit
          />
        </el-form-item>

        <el-form-item label="问题内容" prop="content">
          <el-input
              v-model="questionForm.content"
              type="textarea"
              :rows="8"
              placeholder="请详细描述您的问题..."
              maxlength="5000"
              show-word-limit
          />
        </el-form-item>

        <el-form-item label="附件">
          <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :limit="1"
              :on-change="handleFileChange"
              :on-exceed="handleExceed"
              accept="image/*,.pdf,.doc,.docx"
          >
            <el-button type="primary" plain>选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持图片、PDF、Word文档，单个文件不超过10MB
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitQuestion" :loading="submitting">
            提交问题
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 我的提问列表 -->
    <el-card header="我的提问" style="margin-top: 20px;">
      <el-table :data="myQuestions" v-loading="loading" style="width: 100%">
        <el-table-column prop="title" label="问题标题" min-width="200" />
        <el-table-column prop="courseName" label="所属课程" width="150" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'ANSWERED' ? 'success' : 'warning'">
              {{ scope.row.status === 'ANSWERED' ? '已回答' : '待回答' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="askTime" label="提问时间" width="180" />
        <el-table-column label="操作" width="220">
          <template #default="scope">
            <el-button size="small" @click="openViewDialog(scope.row)">查看</el-button>
            <el-button
                size="small"
                type="primary"
                @click="openEditDialog(scope.row)"
                v-if="scope.row.status === 'UNANSWERED'"
            >
              编辑
            </el-button>
            <el-button
                size="small"
                type="danger"
                @click="deleteQuestion(scope.row)"
                v-if="scope.row.status === 'UNANSWERED'"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
          v-if="total > 10"
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchMyQuestions"
          style="margin-top: 20px; justify-content: center;"
      />
    </el-card>

    <!-- 查看问题详情 -->
    <el-dialog
        v-model="viewDialogVisible"
        title="问题详情"
        width="600px"
    >
      <el-descriptions :column="1" border v-if="selectedQuestion">
        <el-descriptions-item label="问题标题">
          {{ selectedQuestion.title }}
        </el-descriptions-item>
        <el-descriptions-item label="所属课程">
          {{ selectedQuestion.courseName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="selectedQuestion.status === 'ANSWERED' ? 'success' : 'warning'">
            {{ selectedQuestion.status === 'ANSWERED' ? '已回答' : '待回答' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="提问时间">
          {{ selectedQuestion.askTime }}
        </el-descriptions-item>
        <el-descriptions-item label="问题内容">
          <div style="white-space: pre-wrap;">{{ selectedQuestion.content }}</div>
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
        <el-button
            type="primary"
            v-if="selectedQuestion?.status === 'UNANSWERED'"
            @click="() => openEditDialog(selectedQuestion)"
        >
          编辑
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑问题 -->
    <el-dialog
        v-model="editDialogVisible"
        title="修改问题"
        width="640px"
    >
      <el-form
          ref="editFormRef"
          :model="editForm"
          :rules="editRules"
          label-width="100px"
      >
        <el-form-item label="所属课程" prop="courseId">
          <el-select v-model="editForm.courseId" placeholder="请选择课程">
            <el-option
                v-for="course in courses"
                :key="course.id"
                :label="course.name"
                :value="course.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="问题标题" prop="title">
          <el-input
              v-model="editForm.title"
              maxlength="255"
              show-word-limit
          />
        </el-form-item>

        <el-form-item label="问题内容" prop="content">
          <el-input
              v-model="editForm.content"
              type="textarea"
              :rows="6"
              maxlength="5000"
              show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="submitEdit">
          保存修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import courseService from '@/api/courseService'
import { ElMessage, ElMessageBox } from 'element-plus'
import qaService from '@/api/qaService'

const courses = ref([])
const myQuestions = ref([])
const loading = ref(false)
const submitting = ref(false)
const editSubmitting = ref(false)

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const questionForm = reactive({
  courseId: null,
  title: '',
  content: '',
  attachmentFileName: ''
})

const uploadRef = ref(null)
const formRef = ref(null)
const selectedFile = ref(null)
const viewDialogVisible = ref(false)
const editDialogVisible = ref(false)
const selectedQuestion = ref(null)
const editFormRef = ref(null)
const editForm = reactive({
  id: null,
  courseId: null,
  title: '',
  content: ''
})

const rules = reactive({
  courseId: [{ required: true, message: '请选择课程', trigger: 'change', type: 'number' }],
  title: [
    { required: true, message: '请输入问题标题', trigger: 'blur' },
    { min: 5, message: '标题至少5个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入问题内容', trigger: 'blur' },
    { min: 10, message: '内容至少10个字符', trigger: 'blur' }
  ]
})

const editRules = reactive({
  courseId: [{ required: true, message: '请选择课程', trigger: 'change', type: 'number' }],
  title: [
    { required: true, message: '请输入问题标题', trigger: 'blur' },
    { min: 5, message: '标题至少5个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入问题内容', trigger: 'blur' },
    { min: 10, message: '内容至少10个字符', trigger: 'blur' }
  ]
})

// 获取课程列表
const fetchCourses = async () => {
  try {
    const response = await courseService.getCourses()
    courses.value = response
  } catch (error) {
    ElMessage.error('加载课程列表失败')
    console.error(error)
  }
}

// 获取我的提问
const fetchMyQuestions = async () => {
  loading.value = true
  try {
    const response = await request.get('/qa/my-questions', {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value
      }
    })

    myQuestions.value = (response.content || []).map(q => ({
      ...q,
      courseName: q.course?.name || '',
      courseId: q.course?.id || q.courseId,
      askTime: q.askTime,
    }))
    total.value = response.totalElements || 0
  } catch (error) {
    ElMessage.error('加载提问列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 文件选择
const handleFileChange = (file) => {
  selectedFile.value = file.raw
  questionForm.attachmentFileName = file.name
}

// 文件超出限制
const handleExceed = () => {
  ElMessage.warning('只能上传一个附件')
}

// 提交问题
const submitQuestion = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      // 构建 FormData
      const formData = new FormData()
      formData.append('data', new Blob([JSON.stringify({
        title: questionForm.title,
        content: questionForm.content,
        courseId: questionForm.courseId
      })], { type: 'application/json' }))

      if (selectedFile.value) {
        formData.append('attachment', selectedFile.value)
      }

      // 提交问题
      await request.post('/qa/questions', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })

      ElMessage.success('问题提交成功，请等待教师回复')
      resetForm()
      await fetchMyQuestions()
    } catch (error) {
      console.error(error)
    } finally {
      submitting.value = false
    }
  })
}

// 重置表单
const resetForm = () => {
  formRef.value?.resetFields()
  uploadRef.value?.clearFiles()
  selectedFile.value = null
  Object.assign(questionForm, {
    courseId: null,
    title: '',
    content: '',
    attachmentFileName: ''
  })
}

// 查看问题详情
const openViewDialog = (question) => {
  selectedQuestion.value = { ...question }
  viewDialogVisible.value = true
}

// 打开编辑对话框
const openEditDialog = (question) => {
  selectedQuestion.value = { ...question }
  editForm.id = question.id
  editForm.courseId = question.courseId
  editForm.title = question.title
  editForm.content = question.content
  viewDialogVisible.value = false
  editDialogVisible.value = true
}

// 提交编辑
const submitEdit = () => {
  editFormRef.value?.validate(async (valid) => {
    if (!valid) return

    editSubmitting.value = true
    try {
      await qaService.studentUpdateQuestion(editForm.id, {
        title: editForm.title.trim(),
        content: editForm.content.trim(),
        courseId: editForm.courseId
      })
      ElMessage.success('问题已更新')
      editDialogVisible.value = false
      await fetchMyQuestions()
    } catch (error) {
      console.error('更新问题失败：', error)
      ElMessage.error('更新问题失败')
    } finally {
      editSubmitting.value = false
    }
  })
}

// 删除问题
const deleteQuestion = async (question) => {
  try {
    await ElMessageBox.confirm('确定要删除这个问题吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await qaService.studentDeleteQuestion(question.id)
    ElMessage.success('问题已删除')
    await fetchMyQuestions()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

onMounted(() => {
  fetchCourses()
  fetchMyQuestions()
})
</script>

<style scoped>
.ask-question-container {
  padding: 20px;
}
</style>