<template>
  <div class="qa-duty-container">
    <el-card class="summary-card" shadow="hover">
      <div class="summary-header">
        <div>
          <h2>教师答疑中心</h2>
          <p class="sub-title">查看自己课程中的学生提问，并进行回答或修改回答。</p>
        </div>
        <div class="summary-right">
          <el-statistic
              title="待回答问题总数"
              :value="unansweredCount"
          />
        </div>
      </div>
    </el-card>

    <el-card class="main-card" shadow="hover">
      <div class="filters">
        <el-select
            v-model="selectedCourseId"
            placeholder="请选择课程"
            style="width: 260px"
            clearable
            @change="handleCourseChange"
        >
          <el-option
              v-for="course in courses"
              :key="course.id"
              :label="course.name"
              :value="course.id"
          />
        </el-select>

        <el-select
            v-model="questionFilters.status"
            placeholder="按状态筛选"
            clearable
            style="width: 160px"
            :disabled="!selectedCourseId"
        >
          <el-option label="全部状态" value="" />
          <el-option label="未回答" value="UNANSWERED" />
          <el-option label="已回答" value="ANSWERED" />
        </el-select>

        <el-input
            v-model="questionFilters.keyword"
            placeholder="输入问题标题或内容关键字"
            style="width: 280px"
            clearable
            :disabled="!selectedCourseId"
            @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-button type="primary" :disabled="!selectedCourseId" @click="handleSearch">
          搜索
        </el-button>
        <el-button :disabled="!selectedCourseId" @click="handleReset">
          重置
        </el-button>
      </div>

      <el-empty
          v-if="!selectedCourseId && !loading"
          description="请先选择一门课程查看提问"
      />

      <el-table
          v-else
          :data="questions"
          v-loading="loading"
          border
          stripe
          empty-text="当前课程暂无学生提问"
          style="margin-top: 10px;"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="问题标题" min-width="220" />
        <el-table-column prop="content" label="问题内容" min-width="260" show-overflow-tooltip />
        <el-table-column prop="askTime" label="提问时间" width="180" />
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'ANSWERED' ? 'success' : 'warning'">
              {{ scope.row.status === 'ANSWERED' ? '已回答' : '未回答' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="openAnswerDialog(scope.row)">
              查看 / 回答
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
            background
            layout="total, sizes, prev, pager, next"
            :current-page="pagination.page"
            :page-size="pagination.size"
            :page-sizes="[10, 20, 50]"
            :total="pagination.total"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 回答问题对话框 -->
    <el-dialog
        v-model="answerDialogVisible"
        :title="currentQuestion?.title || '回答问题'"
        width="720px"
    >
      <el-descriptions :column="1" border v-if="currentQuestion">
        <el-descriptions-item label="问题内容">
          <div style="white-space: pre-wrap;">{{ currentQuestion.content }}</div>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider>已有回答</el-divider>

      <el-empty
          v-if="answers.length === 0"
          description="暂时没有回答"
      />
      <el-timeline v-else>
        <el-timeline-item
            v-for="item in answers"
            :key="item.id"
            :timestamp="item.answerTime"
        >
          <div class="answer-item">
            <div class="answer-meta">
              <span class="answer-author">
                回答者：{{ item.replier?.username || '未知' }}
              </span>
            </div>
            <div class="answer-content">
              {{ item.content }}
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>

      <el-divider>我的回答</el-divider>

      <el-form
          ref="answerFormRef"
          :model="answerForm"
          :rules="answerFormRules"
          label-width="80px"
      >
        <el-form-item label="回答内容" prop="content">
          <el-input
              v-model="answerForm.content"
              type="textarea"
              :rows="6"
              placeholder="请输入要回复给学生的内容"
              maxlength="5000"
              show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="answerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="answerSubmitting" @click="handleAnswerSubmit">
          {{ hasMyAnswer ? '更新回答' : '提交回答' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/authStore'
import courseService from '@/api/courseService'
import qaService from '@/api/qaService'

const authStore = useAuthStore()

// 课程与统计
const courses = ref([])
const selectedCourseId = ref(null)
const unansweredCount = ref(0)

// 问题列表
const questions = ref([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const questionFilters = reactive({
  keyword: '',
  status: 'UNANSWERED'
})

// 回答对话框
const answerDialogVisible = ref(false)
const answerSubmitting = ref(false)
const currentQuestion = ref(null)
const answers = ref([])
const answerFormRef = ref(null)
const answerForm = reactive({
  content: ''
})

const answerFormRules = {
  content: [
    { required: true, message: '请输入回答内容', trigger: 'blur' },
    { max: 5000, message: '内容长度不能超过5000个字符', trigger: 'blur' }
  ]
}

// 当前教师是否已回答过该问题
const hasMyAnswer = computed(() => {
  const username = authStore.user?.username
  if (!username) return false
  return answers.value.some(a => a.replier?.username === username)
})

const myAnswer = computed(() => {
  const username = authStore.user?.username
  if (!username) return null
  return answers.value.find(a => a.replier?.username === username) || null
})

// 加载教师课程（过滤当前教师）
const fetchCourses = async () => {
  try {
    const response = await courseService.getCourses()
    // 过滤出当前教师创建的课程（与 MyCourses.vue 逻辑一致）
    const userId = authStore.user?.id
    if (!userId) {
      console.warn('用户ID未找到，无法过滤课程')
      courses.value = []
      return
    }
    courses.value = Array.isArray(response) 
      ? response.filter(course => course.teacherId === userId)
      : []
  } catch (error) {
    console.error('加载课程失败：', error)
    ElMessage.error('加载课程失败')
    courses.value = []
  }
}

// 未回答数量
const fetchUnansweredCount = async () => {
  try {
    const count = await qaService.teacherGetUnansweredCount()
    // teacherGetUnansweredCount 返回的就是数字
    unansweredCount.value = typeof count === 'number' ? count : (count ?? 0)
  } catch (error) {
    console.error('获取未回答数量失败：', error)
  }
}

// 加载问题列表
const fetchQuestions = async () => {
  if (!selectedCourseId.value) return
  loading.value = true
  try {
    const response = await qaService.teacherSearchQuestions({
      courseId: selectedCourseId.value,
      keyword: questionFilters.keyword || '',
      status: questionFilters.status || undefined,
      page: pagination.page - 1,
      size: pagination.size
    })

    questions.value = response.content || []
    pagination.total = response.totalElements || 0
  } catch (error) {
    console.error('加载问题列表失败：', error)
    ElMessage.error('加载问题列表失败')
  } finally {
    loading.value = false
  }
}

const handleCourseChange = () => {
  pagination.page = 1
  fetchQuestions()
}

const handleSearch = () => {
  pagination.page = 1
  fetchQuestions()
}

const handleReset = () => {
  questionFilters.keyword = ''
  questionFilters.status = 'UNANSWERED'
  pagination.page = 1
  fetchQuestions()
}

const handleSizeChange = (size) => {
  pagination.size = size
  pagination.page = 1
  fetchQuestions()
}

const handlePageChange = (page) => {
  pagination.page = page
  fetchQuestions()
}

// 打开回答对话框
const openAnswerDialog = async (question) => {
  currentQuestion.value = question
  answerForm.content = ''
  answers.value = []

  try {
    const list = await qaService.getAnswersByQuestionId(question.id)
    answers.value = Array.isArray(list) ? list : []

    if (myAnswer.value) {
      answerForm.content = myAnswer.value.content || ''
    }
  } catch (error) {
    console.error('获取回答列表失败：', error)
    ElMessage.error('获取回答列表失败')
  }

  answerDialogVisible.value = true
}

// 提交回答 / 更新回答
const handleAnswerSubmit = () => {
  if (!currentQuestion.value) return

  answerFormRef.value?.validate(async (valid) => {
    if (!valid) return

    answerSubmitting.value = true
    try {
      const questionId = currentQuestion.value.id
      const existing = myAnswer.value

      if (existing) {
        await qaService.teacherUpdateAnswer(existing.id, {
          questionId,
          content: answerForm.content.trim()
        })
        ElMessage.success('回答已更新')
      } else {
        await qaService.teacherCreateAnswer({
          questionId,
          content: answerForm.content.trim()
        })
        ElMessage.success('回答已提交')
      }

      await fetchQuestions()
      await fetchUnansweredCount()

      // 重新加载回答列表
      const list = await qaService.getAnswersByQuestionId(questionId)
      answers.value = Array.isArray(list) ? list : []

      answerDialogVisible.value = false
    } catch (error) {
      console.error('提交回答失败：', error)
      ElMessage.error('提交回答失败')
    } finally {
      answerSubmitting.value = false
    }
  })
}

onMounted(async () => {
  // 确保用户信息已加载
  if (!authStore.user?.id) {
    console.warn('用户信息未加载，等待一下...')
    // 等待一下让 authStore 初始化
    await new Promise(resolve => setTimeout(resolve, 100))
  }
  
  await fetchCourses()
  if (courses.value.length > 0) {
    selectedCourseId.value = courses.value[0].id
  }
  await fetchUnansweredCount()
  await fetchQuestions()
})
</script>

<style scoped>
.qa-duty-container {
  padding: 20px;
}

.summary-card {
  margin-bottom: 16px;
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sub-title {
  color: #666;
  font-size: 14px;
  margin-top: 4px;
}

.summary-right {
  min-width: 160px;
  text-align: right;
}

.main-card {
  margin-top: 8px;
}

.filters {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.answer-item {
  padding: 4px 0;
}

.answer-meta {
  font-size: 13px;
  color: #999;
  margin-bottom: 4px;
}

.answer-content {
  white-space: pre-wrap;
}
</style>