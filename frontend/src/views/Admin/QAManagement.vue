<template>
  <div class="qa-management-container">
    <el-card shadow="hover">
      <div class="header">
        <div>
          <h2>问答内容管理</h2>
          <p class="sub-title">查看、审核并管理平台上的所有问答内容，可删除违规内容或修改问题/回答。</p>
        </div>
      </div>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 问题管理标签页 -->
        <el-tab-pane label="问题管理" name="questions">
          <div class="filters">
            <el-input
                v-model="questionFilters.keyword"
                placeholder="输入问题标题或内容关键字"
                style="width: 300px"
                clearable
                @keyup.enter="handleQuestionSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>

            <el-select
                v-model="questionFilters.status"
                placeholder="按状态筛选"
                clearable
                style="width: 150px"
            >
              <el-option label="全部状态" value="" />
              <el-option label="未回答" value="UNANSWERED" />
              <el-option label="已回答" value="ANSWERED" />
            </el-select>

            <el-button type="primary" @click="handleQuestionSearch">搜索</el-button>
            <el-button @click="handleQuestionReset">重置</el-button>
          </div>

          <el-table
              :data="questions"
              v-loading="questionLoading"
              border
              stripe
              empty-text="暂无问题数据"
              style="margin-top: 20px;"
          >
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="问题标题" min-width="200" />
            <el-table-column prop="content" label="问题内容" min-width="250" show-overflow-tooltip />
            <el-table-column label="所属课程" width="180">
              <template #default="scope">
                {{ scope.row.course?.name || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="提问者" width="140">
              <template #default="scope">
                {{ scope.row.asker?.username || '-' }}
              </template>
            </el-table-column>
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
                <el-button size="small" @click="openQuestionEditDialog(scope.row)">修改</el-button>
                <el-button
                    size="small"
                    type="danger"
                    @click="handleQuestionDelete(scope.row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
                background
                layout="total, sizes, prev, pager, next"
                :current-page="questionPagination.page"
                :page-size="questionPagination.size"
                :page-sizes="[10, 20, 50, 100]"
                :total="questionPagination.total"
                @size-change="handleQuestionSizeChange"
                @current-change="handleQuestionPageChange"
            />
          </div>
        </el-tab-pane>

        <!-- 回答管理标签页 -->
        <el-tab-pane label="回答管理" name="answers">
          <div class="filters">
            <el-input
                v-model="answerFilters.keyword"
                placeholder="输入回答内容关键字"
                style="width: 300px"
                clearable
                @keyup.enter="handleAnswerSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>

            <el-button type="primary" @click="handleAnswerSearch">搜索</el-button>
            <el-button @click="handleAnswerReset">重置</el-button>
          </div>

          <el-table
              :data="answers"
              v-loading="answerLoading"
              border
              stripe
              empty-text="暂无回答数据"
              style="margin-top: 20px;"
          >
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="content" label="回答内容" min-width="300" show-overflow-tooltip />
            <el-table-column label="所属问题" width="200">
              <template #default="scope">
                {{ scope.row.question?.title || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="回答者" width="140">
              <template #default="scope">
                {{ scope.row.replier?.username || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="answerTime" label="回答时间" width="180" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="openAnswerEditDialog(scope.row)">修改</el-button>
                <el-button
                    size="small"
                    type="danger"
                    @click="handleAnswerDelete(scope.row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
                background
                layout="total, sizes, prev, pager, next"
                :current-page="answerPagination.page"
                :page-size="answerPagination.size"
                :page-sizes="[10, 20, 50, 100]"
                :total="answerPagination.total"
                @size-change="handleAnswerSizeChange"
                @current-change="handleAnswerPageChange"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 修改问题对话框 -->
    <el-dialog
        v-model="questionDialogVisible"
        title="修改问题"
        width="600px"
    >
      <el-form
          ref="questionFormRef"
          :model="questionForm"
          :rules="questionFormRules"
          label-width="100px"
      >
        <el-form-item label="问题标题" prop="title">
          <el-input
              v-model="questionForm.title"
              placeholder="请输入问题标题"
              maxlength="255"
              show-word-limit
          />
        </el-form-item>
        <el-form-item label="问题内容" prop="content">
          <el-input
              v-model="questionForm.content"
              type="textarea"
              :rows="6"
              placeholder="请输入问题内容"
              maxlength="2000"
              show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="questionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="questionDialogSubmitting" @click="handleQuestionSubmit">
          保存修改
        </el-button>
      </template>
    </el-dialog>

    <!-- 修改回答对话框 -->
    <el-dialog
        v-model="answerDialogVisible"
        title="修改回答"
        width="600px"
    >
      <el-form
          ref="answerFormRef"
          :model="answerForm"
          :rules="answerFormRules"
          label-width="100px"
      >
        <el-form-item label="回答内容" prop="content">
          <el-input
              v-model="answerForm.content"
              type="textarea"
              :rows="8"
              placeholder="请输入回答内容"
              maxlength="2000"
              show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="answerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="answerDialogSubmitting" @click="handleAnswerSubmit">
          保存修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import qaService from '@/api/qaService'

const activeTab = ref('questions')

// 问题相关状态
const questions = ref([])
const questionLoading = ref(false)
const questionFilters = reactive({
  keyword: '',
  status: ''
})
const questionPagination = reactive({
  page: 1,
  size: 20,
  total: 0
})
const questionDialogVisible = ref(false)
const questionDialogSubmitting = ref(false)
const questionFormRef = ref(null)
const currentQuestionId = ref(null)
const questionForm = reactive({
  title: '',
  content: ''
})
const questionFormRules = {
  title: [
    { required: true, message: '请输入问题标题', trigger: 'blur' },
    { max: 255, message: '标题长度不能超过255个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入问题内容', trigger: 'blur' },
    { max: 2000, message: '内容长度不能超过2000个字符', trigger: 'blur' }
  ]
}

// 回答相关状态
const answers = ref([])
const answerLoading = ref(false)
const answerFilters = reactive({
  keyword: ''
})
const answerPagination = reactive({
  page: 1,
  size: 20,
  total: 0
})
const answerDialogVisible = ref(false)
const answerDialogSubmitting = ref(false)
const answerFormRef = ref(null)
const currentAnswerId = ref(null)
const answerForm = reactive({
  content: ''
})
const answerFormRules = {
  content: [
    { required: true, message: '请输入回答内容', trigger: 'blur' },
    { max: 2000, message: '内容长度不能超过2000个字符', trigger: 'blur' }
  ]
}

// 问题相关方法
const fetchQuestions = async () => {
  questionLoading.value = true
  try {
    const response = await qaService.adminGetAllQuestions({
      page: questionPagination.page - 1,
      size: questionPagination.size,
      keyword: questionFilters.keyword || undefined,
      status: questionFilters.status || undefined
    })
    questions.value = response.content || []
    questionPagination.total = response.totalElements || 0
  } catch (error) {
    console.error('获取问题列表失败：', error)
    ElMessage.error('获取问题列表失败')
  } finally {
    questionLoading.value = false
  }
}

const handleQuestionSearch = () => {
  questionPagination.page = 1
  fetchQuestions()
}

const handleQuestionReset = () => {
  questionFilters.keyword = ''
  questionFilters.status = ''
  questionPagination.page = 1
  fetchQuestions()
}

const handleQuestionSizeChange = (size) => {
  questionPagination.size = size
  questionPagination.page = 1
  fetchQuestions()
}

const handleQuestionPageChange = (page) => {
  questionPagination.page = page
  fetchQuestions()
}

const openQuestionEditDialog = (question) => {
  currentQuestionId.value = question.id
  questionForm.title = question.title || ''
  questionForm.content = question.content || ''
  questionDialogVisible.value = true
}

const handleQuestionSubmit = () => {
  questionFormRef.value?.validate(async (valid) => {
    if (!valid) return

    questionDialogSubmitting.value = true
    try {
      await qaService.adminUpdateQuestion(currentQuestionId.value, {
        title: questionForm.title.trim(),
        content: questionForm.content.trim()
      })
      ElMessage.success('问题已更新')
      questionDialogVisible.value = false
      await fetchQuestions()
    } catch (error) {
      console.error('更新失败：', error)
      ElMessage.error('更新失败')
    } finally {
      questionDialogSubmitting.value = false
    }
  })
}

const handleQuestionDelete = (question) => {
  ElMessageBox.confirm(
      `确定删除问题「${question.title}」吗？删除后无法恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
  )
      .then(async () => {
        try {
          await qaService.adminDeleteQuestion(question.id)
          ElMessage.success('问题删除成功')
          if (questions.value.length === 1 && questionPagination.page > 1) {
            questionPagination.page -= 1
          }
          await fetchQuestions()
        } catch (error) {
          console.error('删除失败：', error)
          ElMessage.error('删除失败')
        }
      })
      .catch(() => {})
}

// 回答相关方法
const fetchAnswers = async () => {
  answerLoading.value = true
  try {
    const response = await qaService.adminGetAllAnswers({
      page: answerPagination.page - 1,
      size: answerPagination.size,
      keyword: answerFilters.keyword || undefined
    })
    answers.value = response.content || []
    answerPagination.total = response.totalElements || 0
  } catch (error) {
    console.error('获取回答列表失败：', error)
    ElMessage.error('获取回答列表失败')
  } finally {
    answerLoading.value = false
  }
}

const handleAnswerSearch = () => {
  answerPagination.page = 1
  fetchAnswers()
}

const handleAnswerReset = () => {
  answerFilters.keyword = ''
  answerPagination.page = 1
  fetchAnswers()
}

const handleAnswerSizeChange = (size) => {
  answerPagination.size = size
  answerPagination.page = 1
  fetchAnswers()
}

const handleAnswerPageChange = (page) => {
  answerPagination.page = page
  fetchAnswers()
}

const openAnswerEditDialog = (answer) => {
  currentAnswerId.value = answer.id
  answerForm.content = answer.content || ''
  answerDialogVisible.value = true
}

const handleAnswerSubmit = () => {
  answerFormRef.value?.validate(async (valid) => {
    if (!valid) return

    answerDialogSubmitting.value = true
    try {
      await qaService.adminUpdateAnswer(currentAnswerId.value, {
        content: answerForm.content.trim()
      })
      ElMessage.success('回答已更新')
      answerDialogVisible.value = false
      await fetchAnswers()
    } catch (error) {
      console.error('更新失败：', error)
      ElMessage.error('更新失败')
    } finally {
      answerDialogSubmitting.value = false
    }
  })
}

const handleAnswerDelete = (answer) => {
  ElMessageBox.confirm(
      `确定删除该回答吗？删除后无法恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
  )
      .then(async () => {
        try {
          await qaService.adminDeleteAnswer(answer.id)
          ElMessage.success('回答删除成功')
          if (answers.value.length === 1 && answerPagination.page > 1) {
            answerPagination.page -= 1
          }
          await fetchAnswers()
        } catch (error) {
          console.error('删除失败：', error)
          ElMessage.error('删除失败')
        }
      })
      .catch(() => {})
}

const handleTabChange = (tabName) => {
  if (tabName === 'questions') {
    fetchQuestions()
  } else if (tabName === 'answers') {
    fetchAnswers()
  }
}

onMounted(() => {
  fetchQuestions()
})
</script>

<style scoped>
.qa-management-container {
  padding: 20px;
}

.header {
  margin-bottom: 20px;
}

.sub-title {
  color: #666;
  font-size: 14px;
  margin-top: 8px;
}

.filters {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>

