<template>
  <div class="qa-search-container">
    <el-card header="全站问答搜索">
      <el-alert
          title="提示"
          type="info"
          description="您可以按学科、教师、关键字检索全站问答内容，帮助您快速找到相关问题与答案。"
          :closable="false"
          style="margin-bottom: 20px;"
      />

      <!-- 搜索表单 -->
      <el-form :model="searchForm" :inline="true" class="search-form">
        <el-form-item label="关键字">
          <el-input
              v-model="searchForm.keyword"
              placeholder="搜索问题标题或内容"
              clearable
              style="width: 250px;"
              @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="课程">
          <el-select
              v-model="searchForm.courseId"
              placeholder="选择课程（可选）"
              clearable
              filterable
              style="width: 200px;"
          >
            <el-option
                v-for="course in courses"
                :key="course.id"
                :label="course.name"
                :value="course.id"
            >
              <span>{{ course.name }}</span>
              <span style="float: right; color: #8492a6; font-size: 12px;">
                {{ course.college }}
              </span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="教师">
          <el-select
              v-model="searchForm.teacherId"
              placeholder="选择教师（可选）"
              clearable
              filterable
              style="width: 200px;"
          >
            <el-option
                v-for="teacher in teachers"
                :key="teacher.id"
                :label="teacher.username"
                :value="teacher.id"
            >
              <span>{{ teacher.username }}</span>
              <span v-if="teacher.title" style="float: right; color: #8492a6; font-size: 12px;">
                {{ teacher.title }}
              </span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select
              v-model="searchForm.status"
              placeholder="选择状态（可选）"
              clearable
              style="width: 150px;"
          >
            <el-option label="已回答" value="ANSWERED" />
            <el-option label="待回答" value="UNANSWERED" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="loading">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 搜索结果统计 -->
      <div v-if="searched" class="result-info">
        <el-text type="info">
          共找到 <strong>{{ total }}</strong> 条结果
        </el-text>
      </div>

      <!-- 搜索结果列表 -->
      <el-table
          :data="questions"
          v-loading="loading"
          style="width: 100%; margin-top: 20px;"
          empty-text="暂无数据，请尝试调整搜索条件"
      >
        <el-table-column prop="title" label="问题标题" min-width="250" show-overflow-tooltip>
          <template #default="scope">
            <el-link
                type="primary"
                @click="viewQuestionDetail(scope.row)"
                :underline="false"
            >
              {{ scope.row.title }}
            </el-link>
          </template>
        </el-table-column>

        <el-table-column prop="courseName" label="所属课程" width="150" />
        <el-table-column prop="teacherName" label="授课教师" width="120" />

        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'ANSWERED' ? 'success' : 'warning'">
              {{ scope.row.status === 'ANSWERED' ? '已回答' : '待回答' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="askerName" label="提问者" width="120" />
        <el-table-column prop="askTime" label="提问时间" width="180" />

        <el-table-column label="操作" width="120" fixed="right">
          <template #default="scope">
            <el-button
                size="small"
                type="primary"
                link
                @click="viewQuestionDetail(scope.row)"
            >
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
          v-if="total > 0"
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
          style="margin-top: 20px; justify-content: center;"
      />
    </el-card>

    <!-- 问题详情对话框 -->
    <el-dialog
        v-model="detailDialogVisible"
        :title="selectedQuestion?.title"
        width="70%"
        :close-on-click-modal="false"
    >
      <div v-if="selectedQuestion" class="question-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="所属课程">
            {{ selectedQuestion.courseName }}
          </el-descriptions-item>
          <el-descriptions-item label="授课教师">
            {{ selectedQuestion.teacherName }}
          </el-descriptions-item>
          <el-descriptions-item label="提问者">
            {{ selectedQuestion.askerName }}
          </el-descriptions-item>
          <el-descriptions-item label="提问时间">
            {{ selectedQuestion.askTime }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="selectedQuestion.status === 'ANSWERED' ? 'success' : 'warning'">
              {{ selectedQuestion.status === 'ANSWERED' ? '已回答' : '待回答' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <div class="question-content">
          <h4>问题内容：</h4>
          <div class="content-text">{{ selectedQuestion.content }}</div>
        </div>

        <div v-if="selectedQuestion.attachmentPath" class="attachment-section">
          <h4>附件：</h4>
          <el-link
              type="primary"
              :href="getQuestionAttachmentUrl(selectedQuestion.id)"
              target="_blank"
              @click.prevent="downloadQuestionAttachment(selectedQuestion.id)"
          >
            <el-icon><Document /></el-icon>
            查看附件
          </el-link>
        </div>

        <!-- 回答列表 -->
        <el-divider />
        <div class="answers-section">
          <h4>回答列表：</h4>
          <div v-if="answers.length === 0" class="no-answers">
            <el-empty description="暂无回答" :image-size="100" />
          </div>
          <div v-else>
            <el-card
                v-for="answer in answers"
                :key="answer.id"
                class="answer-card"
                shadow="hover"
            >
              <div class="answer-header">
                <el-text strong>{{ answer.replierName }}</el-text>
                <el-text type="info" size="small">{{ answer.replyTime }}</el-text>
              </div>
              <div class="answer-content">{{ answer.content }}</div>
              <div v-if="answer.attachmentPath" class="answer-attachment">
                <el-link
                    type="primary"
                    :href="getAnswerAttachmentUrl(answer.id)"
                    target="_blank"
                    @click.prevent="downloadAnswerAttachment(answer.id)"
                >
                  <el-icon><Document /></el-icon>
                  查看附件
                </el-link>
              </div>
            </el-card>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Document } from '@element-plus/icons-vue'
import qaService from '@/api/qaService'
import courseService from '@/api/courseService'
import teacherService from '@/api/teacherService'

const searchForm = reactive({
  keyword: '',
  courseId: null,
  teacherId: null,
  status: null
})

const courses = ref([])
const teachers = ref([])
const questions = ref([])
const answers = ref([])
const loading = ref(false)
const searched = ref(false)

const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const detailDialogVisible = ref(false)
const selectedQuestion = ref(null)

// 获取课程列表
const fetchCourses = async () => {
  try {
    const response = await courseService.getCourses()
    courses.value = response
  } catch (error) {
    console.error('加载课程列表失败:', error)
  }
}

// 获取教师列表
const fetchTeachers = async () => {
  try {
    const response = await teacherService.getTeachers({ page: 0, size: 1000 })
    // 处理分页响应
    if (response && response.content) {
      teachers.value = response.content
    } else if (Array.isArray(response)) {
      teachers.value = response
    } else {
      teachers.value = []
    }
  } catch (error) {
    console.error('加载教师列表失败:', error)
    // 静默失败，不影响页面使用（教师筛选为可选功能）
    teachers.value = []
  }
}

// 搜索问题
const handleSearch = async () => {
  loading.value = true
  searched.value = true
  currentPage.value = 1

  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value
    }

    if (searchForm.keyword) {
      params.keyword = searchForm.keyword
    }
    if (searchForm.courseId) {
      params.courseId = searchForm.courseId
    }
    if (searchForm.teacherId) {
      params.teacherId = searchForm.teacherId
    }
    if (searchForm.status) {
      params.status = searchForm.status
    }

    const response = await qaService.searchAllQuestions(params)

    questions.value = (response.content || []).map(q => {
      const course = q.course || {}
      const teacherId = course.teacherId || (course.teacher ? course.teacher.id : null)
      return {
        ...q,
        courseName: course.name || '',
        teacherName: teacherId ? getTeacherName(teacherId) : '',
        askerName: q.asker?.username || '',
        askTime: formatDateTime(q.askTime)
      }
    })

    total.value = response.totalElements || 0

    if (total.value === 0) {
      ElMessage.info('未找到相关问题，请尝试调整搜索条件')
    }
  } catch (error) {
    ElMessage.error('搜索失败，请稍后重试')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 重置搜索条件
const handleReset = () => {
  Object.assign(searchForm, {
    keyword: '',
    courseId: null,
    teacherId: null,
    status: null
  })
  questions.value = []
  total.value = 0
  searched.value = false
  currentPage.value = 1
}

// 分页大小改变
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  if (searched.value) {
    handleSearch()
  }
}

// 页码改变
const handlePageChange = (val) => {
  currentPage.value = val
  if (searched.value) {
    handleSearch()
  }
}

// 查看问题详情
const viewQuestionDetail = async (question) => {
  const course = question.course || {}
  const teacherId = course.teacherId || (course.teacher ? course.teacher.id : null)
  
  selectedQuestion.value = {
    ...question,
    courseName: question.courseName || course.name || '',
    teacherName: question.teacherName || (teacherId ? getTeacherName(teacherId) : ''),
    askerName: question.askerName || question.asker?.username || '',
    askTime: question.askTime || formatDateTime(question.askTime)
  }

  // 加载回答列表
  try {
    const response = await qaService.getAnswersByQuestionId(question.id)
    answers.value = (response || []).map(a => ({
      ...a,
      replierName: a.replier?.username || '',
      replyTime: formatDateTime(a.replyTime)
    }))
  } catch (error) {
    console.error('加载回答列表失败:', error)
    answers.value = []
  }

  detailDialogVisible.value = true
}

// 获取问题附件URL
const getQuestionAttachmentUrl = (questionId) => {
  if (!questionId) return ''
  return `/api/qa/questions/${questionId}/attachment`
}

// 获取回答附件URL
const getAnswerAttachmentUrl = (answerId) => {
  if (!answerId) return ''
  return `/api/qa/answers/${answerId}/attachment`
}

// 下载问题附件（使用 axios 下载，避免页面跳转）
const downloadQuestionAttachment = async (questionId) => {
  if (!questionId) return
  await downloadFileWithAuth(`/qa/questions/${questionId}/attachment`)
}

// 下载回答附件（使用 axios 下载，避免页面跳转）
const downloadAnswerAttachment = async (answerId) => {
  if (!answerId) return
  await downloadFileWithAuth(`/qa/answers/${answerId}/attachment`)
}

// 使用 axios 下载文件（支持 token 认证，避免页面跳转）
const downloadFileWithAuth = async (url) => {
  try {
    const token = localStorage.getItem('token')
    const response = await fetch(`http://localhost:8080/api${url}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    if (!response.ok) {
      ElMessage.error('下载失败')
      return
    }
    
    // 获取文件名（从 Content-Disposition 头中提取）
    const contentDisposition = response.headers.get('Content-Disposition')
    let filename = 'attachment'
    if (contentDisposition) {
      // 优先提取 filename*=UTF-8'' 格式（支持中文文件名）
      const filenameStarMatch = contentDisposition.match(/filename\*=UTF-8''(.+?)(?:;|$)/i)
      if (filenameStarMatch) {
        filename = decodeURIComponent(filenameStarMatch[1])
      } else {
        // 否则提取 filename="..." 格式
        const filenameMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
        if (filenameMatch) {
          filename = filenameMatch[1]
        }
      }
    }
    
    console.log('提取的文件名:', filename)
    
    const blob = await response.blob()
    const blobUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(blobUrl)
  } catch (error) {
    console.error('下载附件失败:', error)
    ElMessage.error('下载附件失败')
  }
}

// 获取教师名称
const getTeacherName = (teacherId) => {
  const teacher = teachers.value.find(t => t.id === teacherId)
  return teacher ? teacher.username : ''
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  fetchCourses()
  fetchTeachers()
})
</script>

<style scoped>
.qa-search-container {
  padding: 20px;
}

.search-form {
  margin-bottom: 20px;
}

.result-info {
  margin-bottom: 10px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.question-detail {
  max-height: 70vh;
  overflow-y: auto;
}

.question-content {
  margin: 20px 0;
}

.content-text {
  margin-top: 10px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-word;
}

.attachment-section {
  margin: 20px 0;
}

.answers-section {
  margin-top: 20px;
}

.answer-card {
  margin-bottom: 15px;
}

.answer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.answer-content {
  margin: 10px 0;
  padding: 10px;
  background-color: #f9f9f9;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-word;
}

.answer-attachment {
  margin-top: 10px;
}

.no-answers {
  text-align: center;
  padding: 40px 0;
}
</style>

